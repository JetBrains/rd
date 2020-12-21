using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Serialization;
using JetBrains.Util.Internal;

namespace JetBrains.Threading
{
  /// <summary>
  /// Circular auto expandable and shrinkable byte buffer based on linked list of arrays with two roles:
  /// <list type="number">
  /// <item>Producers - fills buffer with <see cref="Put(byte*,int)"/> method</item>
  /// <item>Consumer - process buffer in dedicated thread</item>
  /// </list>
  /// 
  /// </summary>
  public unsafe class ByteBufferAsyncProcessor
  {

    public enum StateKind
    {
      Initialized,
      AsyncProcessing,
      Stopping,
      Terminating,
      Terminated
    }

    public delegate void Processor(byte[] data, int offset, int len, ref long seqN);

    class Chunk
    {
      internal readonly byte[] Data;
      internal int Ptr;
      internal Chunk Next;
      internal long SeqN;

      public Chunk(int chunkSize)
      {
        Data = new byte[chunkSize];
        Reset();
      }

      public bool CheckEmpty(ByteBufferAsyncProcessor buffer)
      {
        if (Ptr == 0)
        {
          Assertion.Assert(SeqN == long.MaxValue, "SeqN == long.MaxValue, but: {0}", SeqN);
          return true;
        }
        
        if (buffer.AcknowledgedSeqN < SeqN)
          return false;
        
        Reset();
        
        return true;
      }

      public bool IsNotProcessed => SeqN == long.MaxValue;

      internal void Reset()
      {
        SeqN = long.MaxValue;
        Ptr = 0;
      }
    }


    private const string LogCategory = "ByteBufferAsyncProcessor";
    [PublicAPI] public string Id { get; }
    private readonly Processor myProcessor;

    //to be done:  more granular locking for better performance
    private readonly object myLock = new object();
    public long AcknowledgedSeqN { get; private set; }

    private const int DefaultChunkSize = 16370; // some reserve for length + seqN
    private readonly ILog myLog;  //use with care because Logger use AsyncProcessor itself 
    [PublicAPI] public readonly int ChunkSize;

    private const int DefaultShrinkIntervalMs = 30000; //30 sec
    private int myLastShrinkOrGrowTimeMs;
    [PublicAPI] public int ShrinkIntervalMs;

    private bool myAllDataProcessed = true;
    public bool AllDataProcessed
    {
      get
      {
        Memory.Barrier();
        return myAllDataProcessed;
      }
    }
    
    private readonly HashSet<string> myPauseReasons = new HashSet<string>();

    public StateKind State { get; private set; }

    private Chunk myChunkToFill;

    private bool myProcessing;
    
    private volatile Chunk myChunkToProcess;

    private Thread myAsyncProcessingThread;


    public ByteBufferAsyncProcessor(string id, Processor processor)
      : this(id, DefaultChunkSize, processor)
    {}

    public ByteBufferAsyncProcessor(string id, int chunkSize, Processor processor)
    {
      Id = id;
      myProcessor = processor;
      ChunkSize = chunkSize;
      ShrinkIntervalMs = DefaultShrinkIntervalMs;
      
      
      myLog = Log.GetLog<ByteBufferAsyncProcessor>().GetSublogger(Id);

//      var otherChunk = new Chunk(chunkSize);
//      myChunkToFill = new Chunk(chunkSize) { Next = otherChunk };
//      otherChunk.Next = myChunkToFill;

      Reset(chunkSize);
      
      State = StateKind.Initialized;
    }

    public int ChunkCount
    {
      get
      {
        lock (myLock)
        {
          var chunk = myChunkToFill.Next;
          int res = 1;
          while (chunk != myChunkToFill)
          {
            res++;
            chunk = chunk.Next;
          }

          return res;
        }
      }
    }


    #region Helpers
    private void CleanupInternal()
    {
      lock (myLock)
      {
        State = StateKind.Terminated;
        myChunkToFill = null;
        myChunkToProcess = null;
        myAllDataProcessed = true;
      }
    }


    private bool TerminateInternal(int timeoutMs, StateKind state, string action)
    {
      lock (myLock)
      {
        if (State == StateKind.Initialized)
        {
          LogLog.Verbose(LogCategory, "Can't {1} '{0}', because it hasn't been started yet", Id, action);
          CleanupInternal();
          return true;
        }

        if (State >= state)
        {
          LogLog.Verbose(LogCategory, "Trying to {2} async processor '{0}' but it's in state '{1}'", Id, State, action);
          return true;
        }

        State = state;
        Monitor.Pulse(myLock);
      }

      var res =  myAsyncProcessingThread.Join(timeoutMs);

      if (!res)
      {
        LogLog.Warn($"Async processor {Id} hasn't finished in ${timeoutMs} ms. Trying to abort thread.");
        LogLog.Catch(() => myAsyncProcessingThread.Abort());
      }

      CleanupInternal();
      return res;
    }


    //since sometimes we terminate thread via Thread.Abort() it's quite normal to catch this abort
    private void ThreadProcCatchAbort()
    {
      try
      {
        ThreadProc();
      }
      catch (ThreadAbortException e)
      {
        LogLog.Info($"ByteBufferProcessor {Id} was stopped by Thread.Abort rather than by normal Stop(): {0}", e);

        Thread.ResetAbort();
      }
    }
    
    #endregion
    
    #region Processing 

    public void Acknowledge(long seqNumber)
    {
      lock (myLock)
      {
        LogLog.Trace(LogCategory, "New acknowledged seqN: {0}", seqNumber);
        if (seqNumber > AcknowledgedSeqN)
        {
          AcknowledgedSeqN = seqNumber;
        }
        else
        {
          //it's ok ack came 2 times for same package, because if connection lost/resume client resend package with lower number and could receive packages with lower numbers
          //throw new InvalidOperationException($"Acknowledge({seqNumber}) called, while next {nameof(seqNumber)} MUST BE greater than `{AcknowledgedSeqN}`");
        }
      }
    }

    public void ReprocessUnacknowledged()
    {
      Assertion.Require(Thread.CurrentThread != myAsyncProcessingThread, "Thread.CurrentThread != myAsyncProcessingThread");
      lock (myLock)
      {
        while (myProcessing) 
          Monitor.Wait(myLock, 1);

        var chunk = myChunkToFill.Next;
        while (chunk != myChunkToFill)
        {
          if (!chunk.CheckEmpty(this))
          {
            //todo forbid acknowledges that could break processing
            myChunkToProcess = chunk;
            myAllDataProcessed = false;
            Monitor.PulseAll(myLock);
            return;
          }
          else
            chunk = chunk.Next;
        }
      }
    }
    
    
    private void ThreadProc()
    {
      while (true)
      {
        lock (myLock)
        {
          if (State >= StateKind.Terminating) return;

          while (myAllDataProcessed || myPauseReasons.Count > 0)
          {
            if (State >= StateKind.Stopping) return;
            Monitor.Wait(myLock);
            if (State >= StateKind.Terminating) return;
          }

          //In case of only put requests, we could write Assertion.Assert(chunk.Ptr > 0, "chunk.Ptr > 0"); 
          //But in case of clear, we could get "Wait + Put(full)  + Clear + Put" before this line and 'chunkToProcess' will point to empty chunk. 
          //RIDER-15223
          while (myChunkToProcess.CheckEmpty(this)) //should never be endless, because `myAllDataProcessed` is 'false', that means that we MUST have ptr > 0 somewhere
            myChunkToProcess = myChunkToProcess.Next;

          if (myChunkToFill == myChunkToProcess)
          {
            //it's possible that next chuck is occupied by entry with seqN > acknowledgedSeqN
            GrowConditionally();

            myChunkToFill = myChunkToProcess.Next;
          }

          ShrinkConditionally(myChunkToProcess);

          Assertion.Assert(myChunkToProcess.Ptr > 0, "chunkToProcess.Ptr > 0");
          Assertion.Assert(myChunkToFill != myChunkToProcess && myChunkToFill.IsNotProcessed, "myChunkToFill != chunkToProcess && myChunkToFill.IsNotProcessed");

          myProcessing = true;
        }


        long seqN = myChunkToProcess.IsNotProcessed ? 0 : myChunkToProcess.SeqN;
        try
        {
          myProcessor(myChunkToProcess.Data, 0, myChunkToProcess.Ptr, ref seqN);
        }
        catch (Exception e)
        {
          LogLog.Error(e);
        }
        finally
        {
          lock (myLock)
          {
            myProcessing = false;

            if (myChunkToProcess == null)
            {
              LogLog.Error($"{nameof(myChunkToProcess)} is null. State: {State}");
            }
            else
            {
              myChunkToProcess.SeqN = seqN;
              myChunkToProcess = myChunkToProcess.Next;
              //            Assertion.Assert(myChunkToProcess.IsNotProcessed, "chunkToProcess.IsNotProcessed"); not true in case of reprocessing
              if (myChunkToProcess.Ptr == 0)
                myAllDataProcessed = true;
            }
          }
        }
      }
    }

    #endregion



    #region State changing API

    /// <summary>
    /// Starts async processing of queue.
    /// </summary>
    public void Start()
    {
      lock (myLock)
      {
        if (State != StateKind.Initialized)
        {
          LogLog.Verbose(LogCategory, "Trying to START async processor '{0}' but it's in state '{1}'", Id, State);
          return;
        }

        State = StateKind.AsyncProcessing;

        myAsyncProcessingThread = new Thread(ThreadProcCatchAbort) { Name = Id, IsBackground = true};
        myAsyncProcessingThread.Start();
      }
    }

    private void Reset(int chunkSize)
    {
      myChunkToFill = new Chunk(chunkSize);
      myChunkToFill.Next = myChunkToFill;
      myLastShrinkOrGrowTimeMs = Environment.TickCount;
      myChunkToProcess = myChunkToFill; 
    }
    
    public void Clear()
    {
      Assertion.Require(Thread.CurrentThread != myAsyncProcessingThread, "Thread.CurrentThread != myAsyncProcessingThread");

      lock (myLock)
      {
        LogLog.Verbose(LogCategory, "Cleaning '{0}', state={1}", Id, State);
        if (State >= StateKind.Stopping) return;
        
        WaitProcessingFinished();

        Reset(ChunkSize);        
        myAllDataProcessed = true;
      }
    }
    
    public bool Pause([NotNull] string reason)
    {
      if (reason == null) throw new ArgumentNullException(nameof(reason));
      
      lock (myLock)
      {        
        if (State >= StateKind.Stopping) return false;
        
        var newReasonAdded = myPauseReasons.Add(reason);
        myLog.Verbose("PAUSE ('{0}') {1}:: state={2}", reason, newReasonAdded ? "": "<already has this pause reason>", State);
        
        WaitProcessingFinished();
        return newReasonAdded;
      }
    }

    private void WaitProcessingFinished()
    {
      if (Thread.CurrentThread == myAsyncProcessingThread) return; //don't want to deadlock
      
      while (myProcessing) 
        Monitor.Wait(myLock, 1);
    }

    public bool Resume([NotNull] string reason)
    {
      if (reason == null) throw new ArgumentNullException(nameof(reason));
      
      lock (myLock)
      {
        var present = myPauseReasons.Remove(reason);
        var unpaused = myPauseReasons.Count == 0;
        
        myLog.Verbose((unpaused ? "RESUME" : $"Remove pause reason('{reason}')") + $" :: state={State}");
        Monitor.PulseAll(myLock);
        return present;
      }
    }

    /// <summary>
    /// Graceful stop. Process queue, but doesn't accept new data via <see cref="Put(byte[])"/>. Joins processing thread for given timeout. If timeout elapsed, aborts thread.
    /// </summary>
    /// <param name="timeoutMs">Timeout to wait. <see cref="Timeout.Infinite"/> for infinite waiting.</param>
    /// <returns>'true' if Join(timeoutMs) was successful, false otherwise. Also returns 'false' if thread is already stopped or killed."></returns>
    public bool Stop(int timeoutMs = Timeout.Infinite)
    {
      return TerminateInternal(timeoutMs, StateKind.Stopping, "STOP");
    }



    /// <summary>
    /// Force stop. Doesn't process queue, doesn't accept new data via <see cref="Put(byte[])"/>. Joins processing thread for given timeout. If timeout elapsed, aborts thread.
    /// </summary>
    /// <param name="timeoutMs">Timeout to wait. <see cref="Timeout.Infinite"/> for infinite waiting.</param>
    /// <returns>'true' if Join(timeoutMs) was successful, false otherwise. Also returns 'false' if thread is already stopped or killed."></returns>
    public bool Terminate(int timeoutMs = Timeout.Infinite)
    {
      return TerminateInternal(timeoutMs, StateKind.Terminating, "TERMINATE");
    }

    #endregion



    #region Queue filling API

    [PublicAPI] public void Put(byte[] data)
    {
      fixed (byte* ptr = data)
      {
        Put(ptr, data.Length);
      }
    }


    [PublicAPI] public void Put(UnsafeWriter.Cookie data)
    {
      Put(data.Data, data.Count);
    }


#if !NET35
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions] // to force myLock to be unlocked even in case of corrupted state exception
#endif
    [PublicAPI] public void Put(byte* start, int count)
    {
      if (count <= 0) return;
      
      lock (myLock)
      {
        if (State >= StateKind.Stopping) return;        
        
//        //reentrancy guard
//        if (myAsyncProcessingThread == Thread.CurrentThread)
//        {
//          byte[] instantChunk = new byte[count];
//          Marshal.Copy((IntPtr)start, instantChunk, 0, count);
//          myProcessor(instantChunk, 0, count);
//          return;
//        }

        try
        {
          var ptr = 0;
          while (ptr < count)
          {
            Assertion.Assert(myChunkToFill.IsNotProcessed, "myChunkToFill.IsNotProcessed");
            var rest = count - ptr;
            var available = ChunkSize - myChunkToFill.Ptr;
            if (available > 0)
            {
              var copylen = Math.Min(rest, available);
              Marshal.Copy((IntPtr) (start + ptr), myChunkToFill.Data, myChunkToFill.Ptr, copylen);
              myChunkToFill.Ptr += copylen;
              ptr += copylen;
            }
            else
            {
              GrowConditionally();
              myChunkToFill = myChunkToFill.Next;
            }
          }

          if (myAllDataProcessed) //speedup
          {
            myAllDataProcessed = false;
            Monitor.Pulse(myLock);
          }
        }
        catch (Exception ex)
        {
          LogLog.Error(ex);
          throw;
        }
      }
    }

    private void GrowConditionally() //under lock
    {
      if (myChunkToFill.Next.CheckEmpty(this)) return;
      
      LogLog.Trace(LogCategory, "Grow: {0} bytes", ChunkSize);
      myChunkToFill.Next = new Chunk(ChunkSize) { Next = myChunkToFill.Next };
      myLastShrinkOrGrowTimeMs = Environment.TickCount;
    }

    private void ShrinkConditionally(Chunk upTo) //under lock
    {
      Assertion.Assert(myChunkToFill != upTo, "myFreeChunk != upTo");
      
      var now = Environment.TickCount;
      if (now - myLastShrinkOrGrowTimeMs <= ShrinkIntervalMs && /*overflow*/now - myLastShrinkOrGrowTimeMs >= 0 ) return;
      
      myLastShrinkOrGrowTimeMs = now;
      while (true)
      {
        var toRemove = myChunkToFill.Next;
        if (toRemove == upTo || !toRemove.CheckEmpty(this))
          break;
          
        LogLog.Trace(LogCategory, "Shrink: {0} bytes, seqN: {1}", ChunkSize, toRemove);
        myChunkToFill.Next = toRemove.Next;
      }
    }
    #endregion

  }
}