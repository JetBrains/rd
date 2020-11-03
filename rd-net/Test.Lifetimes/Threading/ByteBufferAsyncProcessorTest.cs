using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Diagnostics;
using JetBrains.Serialization;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.Lifetimes.Threading
{
  [TestFixture]
  public class ByteBufferAsyncProcessorTest : LifetimesTestBase
  {
    [Test]
    public void TestOneProducer()
    {
      var processed = 0;
      var buffer = new ByteBufferAsyncProcessor("TestAsyncProcessor", 10, delegate(byte[] data, int offset, int len, ref long seqN)
      {
        Assert.Greater(len, 0);
        
        for (int i = 0; i < len - 1; i++)
        {
          Assert.AreEqual(data[offset + i+1], (byte)(data[offset + i]+1));
          
        }

        processed += len;
      });

      byte b = 0;
      int l = 0;
      var toProcess = 0;
      for (int i = 0; i < 300; i++)
      {
        toProcess += l;
        byte[] p = new byte[l++];
        for (int j = 0; j < p.Length; j++)
        {
          p[j] = b++;
        }
        buffer.Put(p);

        if (i == 20)
        {
          buffer.Start(); //testing delayed start
        }
        if (i > 0 && i % 50 == 0)
        {
          SpinWaitEx.SpinUntil(() => buffer.AllDataProcessed); //give it to process
        }
      }

      SpinWaitEx.SpinUntil(() => buffer.AllDataProcessed); //give it to process

      Assert.AreEqual(toProcess, processed);
      Assert.True(buffer.Stop(1000));
    }

    [Test]
    public void TestClean()
    {
      int x = 0;
      var buffer = new ByteBufferAsyncProcessor("TestAsyncProcessor", 1, delegate(byte[] data, int offset, int len, ref long seqN) { x += data[offset]; });
      
      buffer.Put(new byte[]{1});
      buffer.Put(new byte[]{2});
      buffer.Put(new byte[]{3});
      
      Assert.False(buffer.AllDataProcessed); //not started
      
      buffer.Clear();
      Assert.True(buffer.AllDataProcessed);
      
      buffer.Start();
      buffer.Put(new byte[]{1, 2, 3});

      SpinWaitEx.SpinUntil(() => buffer.AllDataProcessed);
      Assert.AreEqual(6, x);
    }
    
    [Test]
    public void TestPause()
    {
      int x = 0;
      var buffer = new ByteBufferAsyncProcessor("TestAsyncProcessor", 1, delegate(byte[] data, int offset, int len, ref long seqN) { x += data[offset]; });
      var reason1 = "reason1";
      var reason2 = "reason2";
      Assert.True(buffer.Pause(reason1));
      Assert.False(buffer.Pause(reason1));
      Assert.True(buffer.Pause(reason2));
      Assert.False(buffer.Pause(reason2));
      
      buffer.Start();
      buffer.Put(new byte[]{1, 2, 3});
      Thread.Sleep(50);      
      Assert.AreEqual(0, x);
      
      Assert.True(buffer.Resume(reason1));
      Assert.False(buffer.Resume(reason1));      
      Thread.Sleep(50);      
      Assert.AreEqual(0, x);
      
      Assert.True(buffer.Resume(reason2));
      Assert.False(buffer.Resume(reason2));
      Assert.False(buffer.Resume(reason2));
      SpinWaitEx.SpinUntil(() => buffer.AllDataProcessed);
      Assert.AreEqual(6, x);
    }

    
#if !NET35
    [Test]
    public unsafe void StressTestWithAck()
    {
//      LogLog.SeverityFilter = LoggingLevel.VERBOSE;
//      LogLog.RecordsChanged += record => { Console.WriteLine(record.Format(true)); };

      long prev = 0;
      ByteBufferAsyncProcessor buffer = null;
      buffer = new ByteBufferAsyncProcessor("TestAsyncProcessor", 8,
        delegate(byte[] data, int offset, int len, ref long seqN)
        {
          long l = 0;
          Log.Root.Catch(() =>
          {
            fixed (byte* b = data)
            {
              l = UnsafeReader.CreateReader(b, 8).ReadLong();
              Assert.True(l > prev);
              prev = l;
              if (l % 1 == 0) 
                Ack(l);
                
            }
          });
          seqN = l;
        });
      buffer.ShrinkIntervalMs = 10;
      buffer.Start();

      void Ack(long seqn)
      {
        buffer?.Acknowledge(seqn);
      }
      
      var start = Environment.TickCount;

      bool Until() => Environment.TickCount - start < 1000;

      long next = 0;
      var tasks = new List<Task>();
      
      for (int i=0; i<4; i++)
        tasks.Add(Task.Run(() =>
        {
          var rnd = new Random();

          while (Until())
          {
            lock (tasks)
            {
              using (var cookie = UnsafeWriter.NewThreadLocalWriter())
              {
                cookie.Writer.Write(++next);
                buffer.Put(cookie);
              }
            }

            if (rnd.Next(1000) < 1) Thread.Sleep(1);
            if (rnd.Next(1000) < 5)
              buffer.Clear();
          }
        }));
      
      Task.WaitAll(tasks.ToArray());
//      Console.WriteLine(next);
//      Console.WriteLine(buffer.ChunkCount);
    }
#endif

    [Test]
    public unsafe void TestReprocess()
    {
      long prev = 0;
      ByteBufferAsyncProcessor buffer = null;
      List<long> log = new List<long>();
      
      buffer = new ByteBufferAsyncProcessor("TestAsyncProcessor", 8,
        delegate(byte[] data, int offset, int len, ref long seqN)
        {
          try
          {
            fixed (byte* b = data)
            {
              long l = UnsafeReader.CreateReader(b, 8).ReadLong();
              if (seqN != 0)
                Assert.AreEqual(l, seqN);
              seqN = l;
              log.Add(l);
              
              Assert.True(l > prev);
              prev = l;
            }
          }
          catch (Exception e)
          {
            Log.Root.Error(e);
          }
        });
      
      buffer.ShrinkIntervalMs = 10;
      buffer.Start();  
      
      PutLong(buffer, 1);
      PutLong(buffer, 2);
      PutLong(buffer, 3);
      PutLong(buffer, 4);

      SpinWaitEx.SpinUntil(() => buffer.AllDataProcessed);
      Assert.AreEqual(new List<int> {1, 2, 3, 4}, log);
      
      buffer.Acknowledge(2);
      prev = 2;
      buffer.ReprocessUnacknowledged();
      
      PutLong(buffer, 5);
      SpinWaitEx.SpinUntil(() => buffer.AllDataProcessed);
      Assert.AreEqual(new List<int> {1, 2, 3, 4, 3, 4, 5}, log);
      
      buffer.Acknowledge(5);
      buffer.ReprocessUnacknowledged();
      SpinWaitEx.SpinUntil(() => buffer.AllDataProcessed);
      Assert.AreEqual(new List<int> {1, 2, 3, 4, 3, 4, 5}, log);
    }

    private void PutLong(ByteBufferAsyncProcessor buffer, long l)
    {
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        cookie.Writer.Write(l);
        buffer.Put(cookie);
      }  
    }
  }
}