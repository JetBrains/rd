#pragma warning disable 1998
using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Util;


namespace JetBrains.Threading
{

  /// <summary>
  /// Actor is a channel with predefined (possible async) handler. All messages from the channel are
  /// processed sequentially so handling of new message from channel can only start when handling of previous one is completed.  
  /// </summary>
  /// <typeparam name="T">type of message</typeparam>
  public class Actor<T> : ISendChannel<T>
  {
    [PublicAPI] public string Id;
    private readonly Func<T, Task> myProcessor;
    private readonly AsyncChannel<T> myChannel;
    private readonly ILog myLog;


    private readonly AsyncLocal<Unit?> myInsideProcessingFlow = new(); 

    private long myTotalMessagesProcessed;

    /// <summary>
    /// You can use this field for composing this actor's channel with other channels.
    /// <see cref="SendAsync"/> and <see cref="SendBlocking"/> simply delegates to it.
    /// </summary>
    [PublicAPI] public ISendChannel<T> Channel => myChannel;

    /// <summary>
    /// Channel becomes empty when last message is processed and no new messages enqueued
    /// or when actor's lifetime is terminated.
    /// enqueued.
    /// </summary>
    [PublicAPI]
    public bool IsEmpty => myTotalMessagesProcessed == myChannel.TotalMessagesSent;
    
    
    /// <summary>
    /// There are some messages that are not processed. Actor becomes not empty synchronously
    /// when one sends message to it. 
    /// </summary>
    [PublicAPI]
    public bool IsNotEmpty => !IsEmpty;


    /// <summary>
    /// Returns true if called inside message processing (in logical flow / async locally).  
    /// <see cref="AsyncLocal{T}"/>
    /// </summary>
    public bool IsInsideProcessing => myInsideProcessingFlow.Value != null;

    
    /// <summary>
    /// Creates new actor with synchronous handler
    /// </summary>
    /// <param name="id">for logging purposes</param>
    /// <param name="lifetime">cancels all inner messages after this lifetime's termination and make this actor <see cref="IsEmpty"/></param>
    /// <param name="processor">handler of messages</param>
    /// <param name="scheduler"><paramref name="processor"/>'s body scheduler</param>
    /// <param name="maxQueueSize">upper bound for channel after that <see cref="SendBlocking"/> will block</param>
    public Actor(string id, Lifetime lifetime, Action<T> processor, TaskScheduler? scheduler = null,
      int maxQueueSize = int.MaxValue) :
      this(id, lifetime, async item => processor(item), scheduler, maxQueueSize) {}
    
    /// <summary>
    /// Creates new actor with asynchronous handler
    /// </summary>
    /// <param name="id"></param>
    /// <param name="lifetime"></param>
    /// <param name="processor"></param>
    /// <param name="scheduler"></param>
    /// <param name="maxQueueSize"></param>
    //todo move lifetime into Send
    public Actor(string id, Lifetime lifetime, Func<T, Task> processor, TaskScheduler? scheduler = null, int maxQueueSize = int.MaxValue)
    {
      Id = id;
      myProcessor = processor;
      myLog = Log.GetLog<Actor<Unit>>().GetSublogger(id);
      myChannel = new AsyncChannel<T>(lifetime, maxQueueSize);

      Task.Factory.StartNew(async () =>
        {
          try
          {
            myInsideProcessingFlow.Value = Unit.Instance;
            while (lifetime.IsAlive)
              await Process();
          }
          finally
          {
            Assertion.Require(IsEmpty, "Not empty: sent items: {0}, processed items: {1}", myChannel.TotalMessagesSent, myTotalMessagesProcessed);
          }
        }, lifetime, TaskCreationOptions.None, scheduler ?? TaskScheduler.Default);
    }

    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
    
    
    
    private async Task Process()
    {
      var receiveItemTask = myChannel.ReceiveAsync();
      
      var item = await receiveItemTask;
      try
      {
        await myProcessor(item);
      }
      catch (Exception e)
      {
        myLog.Error(e);
      }
      finally
      {
        Interlocked.Increment(ref myTotalMessagesProcessed);
      }
    }

    /// <summary>
    /// Send message to actor. If underlying channel is full than blocks.
    /// </summary>
    /// <param name="msg"></param>
    public void SendBlocking(T msg) => myChannel.SendBlocking(msg);
    
    /// <summary>
    /// Send message to actor. If underlying channel is full than return task that will be completed when
    /// message is finally gets into buffer.
    /// 
    /// Otherwise return <see cref="Task"/>.<see cref="Task.CompletedTask"/>
    /// </summary>
    /// <param name="msg"></param>
    /// <returns></returns>
    /// <remarks>Note than returned task completes when message is put into buffer, not when it's processed. </remarks>
    public Task SendAsync(T msg) => myChannel.SendAsync(msg);

    /// <summary>
    /// If <see cref="IsInsideProcessing"/> is true than process message inline.
    /// Otherwise do <see cref="SendBlocking"/>.
    /// </summary>
    /// <param name="msg"></param>
    /// <returns></returns>
    public async Task SendOrExecuteInline(T msg)
    {
      if (IsInsideProcessing)
      {
        await myProcessor(msg);
      }
      else
      {
        SendBlocking(msg);
      }
    } 

    /// <summary>
    /// Synchronously wait until <see cref="IsEmpty"/> is true
    /// </summary>
    public void WaitForEmpty()
    {
      if (IsEmpty)
        return;

      SpinWaitEx.SpinUntil(() => IsEmpty);
    }
  }
}