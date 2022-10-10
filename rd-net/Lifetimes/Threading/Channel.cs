using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Util;

#if !NET35
namespace JetBrains.Threading
{
  public interface IReceiveChannel<T>
  {
    T ReceiveBlocking();
    Task<T> ReceiveAsync();
  }

  public interface ISendChannel<T>
  {
    void SendBlocking(T msg);
    Task SendAsync(T msg);
  }

  /// <summary>
  /// The same as blocking queue but for non-blocking asynchronous world.
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public class AsyncChannel<T> : IReceiveChannel<T>, ISendChannel<T>
  {
    private readonly Lifetime myLifetime;

    private readonly object myLock = new object();
        

    private readonly Queue<TaskCompletionSource<Unit>> mySenders = new Queue<TaskCompletionSource<Unit>>();
    private readonly Queue<T> myMessages = new Queue<T>();
    private readonly Queue<TaskCompletionSource<T>> myReceivers = new Queue<TaskCompletionSource<T>>();

    private int myTotalMessagesSent = 0;
    [PublicAPI] public long TotalMessagesSent
    {
      get
      {
        lock (myLock)
          return myTotalMessagesSent;
      }
    }

    [PublicAPI] public readonly int SendBufferSize;
    [PublicAPI] public bool IsEmpty
    {
      get
      {
        lock (myLock)
          return myMessages.Count == 0;
      }
    }

    void SetCanceledIsolated<TResult>(TaskCompletionSource<TResult> tcs) { try { tcs.SetCanceled(); } catch (Exception e) { Log.Root.Error(e);}}
    
    public AsyncChannel(Lifetime lifetime, int sendBufferSize = Int32.MaxValue)
    {
      
      myLifetime = lifetime;
      if (sendBufferSize < 0) throw new ArgumentException(nameof(sendBufferSize) + "must be greater or equal to zero, but was: " + sendBufferSize);
      
      SendBufferSize = sendBufferSize;

      lifetime.TryOnTermination(() =>
      {
        lock (myLock) { } // no one should get beyond this point

        while (mySenders.TryDequeue(out var tcs))
          SetCanceledIsolated(tcs);

        while (myReceivers.TryDequeue(out var tcs))
          SetCanceledIsolated(tcs);

        myTotalMessagesSent -= myMessages.Count;
        myMessages.Clear();
      });
    }

    #region Blocking API

    [PublicAPI] public void SendBlocking(T msg) => SendAsync(msg).Wait();
    [PublicAPI] public T ReceiveBlocking() => ReceiveAsync().Result;

    #endregion


    #region Diagnostics
    private void AssertState()
    {
      if (!Mode.IsAssertion) return;

      lock (myLock)
      {
        Assertion.Assert(mySenders.Count <= myMessages.Count
                         && (myMessages.Count < SendBufferSize || mySenders.Count + SendBufferSize == myMessages.Count)
                         && (myMessages.Count == 0 || myReceivers.Count == 0)
          , "Bad state for {0} ", this);
      }
    }

    public override string ToString()
    {
      lock (myLock)
      {
        return $"AsyncChannel<{typeof(T)}>[senders: {mySenders.Count}, receivers: {myReceivers.Count}, messages: {myMessages.Count}]";
      }
    }
    
    #endregion
    
    
    
    [PublicAPI] public Task SendAsync(T msg)
    {
      lock (myLock)
      {
        if (!myLifetime.IsAlive)
          return Task.FromCanceled(myLifetime);
        
        AssertState();
        myTotalMessagesSent++;
        
        if (myReceivers.TryDequeue(out var receiver))
        {
          receiver.SetResult(msg);
          return Task.CompletedTask;
        }
        else {
          myMessages.Enqueue(msg);
          return myMessages.Count > SendBufferSize ? 
            mySenders.Enqueued(new TaskCompletionSource<Unit>(TaskCreationOptions.RunContinuationsAsynchronously)).Task : 
            Task.CompletedTask;
        }
      }
    }
        

    [PublicAPI]
    public Task<T> ReceiveAsync()
    {      
      lock (myLock)
      {
        if (!myLifetime.IsAlive)
          return Task.FromCanceled<T>(myLifetime);
        
        AssertState();
        
        if (myMessages.TryDequeue(out var msg))
        {
          if (mySenders.TryDequeue(out var sender)) 
            sender.SetResult(Unit.Instance);

          return Task.FromResult(msg);
        }
        else
        {
          return myReceivers.Enqueued(new TaskCompletionSource<T>(TaskCreationOptions.RunContinuationsAsynchronously)).Task;
        }                     
      }
    } 
  }
}
#endif