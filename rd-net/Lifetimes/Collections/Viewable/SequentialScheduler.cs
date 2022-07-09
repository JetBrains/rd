#if !NET35
using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Channels;
using System.Threading.Tasks;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Threading;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Lightweight task scheduler that executes items sequentially on a passed scheduler
  /// </summary>
  public sealed class SequentialScheduler : TaskScheduler, IScheduler
  {
    private readonly bool myAllowInlining;
    private readonly ThreadLocal<uint> myExecutionCount;
    
    private readonly SyncContext mySyncContext;
    private readonly Channel<Task> myChannel;
    private readonly ILog myLog;

    public bool IsActive => myExecutionCount.Value > 0;
    public bool OutOfOrderExecution => false;

    [Obsolete("Use the overload without lifetime")]
    public static SequentialScheduler FromIScheduler(string id, Lifetime lifetime, IScheduler scheduler) => FromIScheduler(id, scheduler);

    public static SequentialScheduler FromIScheduler(string id, IScheduler scheduler, bool allowInlining = false)
    {
      return new SequentialScheduler(id, scheduler as TaskScheduler ?? new SchedulerWrapper(scheduler), allowInlining);
    }

    [Obsolete("Use the overload without lifetime")]
    public SequentialScheduler(string id, Lifetime lifetime, TaskScheduler? scheduler = null) : this(id, scheduler)
    {
    }

    public SequentialScheduler(string id, TaskScheduler? scheduler = null, bool allowInlining = false)
    {
      myAllowInlining = allowInlining;
      mySyncContext = new SyncContext(this);
      myExecutionCount = new ThreadLocal<uint>();
      myLog = Log.GetLog<SequentialScheduler>().GetSublogger(id);
      myChannel = Channel.CreateUnbounded<Task>(new UnboundedChannelOptions
      {
        SingleReader = true,
        AllowSynchronousContinuations = false
      });
      
      Lifetime.Eternal.StartAsync(scheduler ?? Default, async () =>
      {
        // do not use lifetime here, as this can lead to loss of tasks and consequently to asynchronous deadlocks or breaking tasks flow
        // in addition, this scheduler is very lightweight, and we can afford not to collect resources manually, and leave everything to the garbage collector
        while (true)
        {
          var task = await myChannel.Reader.ReadAsync();
          TryExecuteTaskWithContext(task);
        }
      }).NoAwait();
    }

    public void Queue(Action action)
    {
      // use TPL to keep correct context-capture behavior
      Task.Factory.StartNew(() => myLog.Catch(action), CancellationToken.None, TaskCreationOptions.None, this);
    }

    protected override void QueueTask(Task task) => Assertion.Assert(myChannel.Writer.TryWrite(task));

    private bool TryExecuteTaskWithContext(Task task)
    {
      var old = SynchronizationContext.Current;
      SynchronizationContext.SetSynchronizationContext(mySyncContext);
      myExecutionCount.Value++;
      try
      {
        return TryExecuteTask(task);
      }
      finally
      {
        myExecutionCount.Value--;
        SynchronizationContext.SetSynchronizationContext(old);
      }
    }

    protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued)
    {
      return myAllowInlining && IsActive && TryExecuteTaskWithContext(task);
    }

    protected override IEnumerable<Task> GetScheduledTasks() => Array.Empty<Task>();

    private class SyncContext : SynchronizationContext
    {
      private readonly SequentialScheduler myScheduler;

      public SyncContext(SequentialScheduler scheduler)
      {
        myScheduler = scheduler;
      }

      public override void Post(SendOrPostCallback d, object state)
      {
        var action = new Action<object>(d);
        Task.Factory.StartNew(action, state, CancellationToken.None, TaskCreationOptions.None, myScheduler);
      }
    }

    private class SchedulerWrapper : TaskScheduler
    {
      private readonly IScheduler myScheduler;

      public SchedulerWrapper(IScheduler scheduler)
      {
        myScheduler = scheduler;
      }

      protected override void QueueTask(Task task) => myScheduler.Queue(() => TryExecuteTask(task));
      protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued) => false;
      protected override IEnumerable<Task> GetScheduledTasks() => Array.Empty<Task>();
    }
  }
}
#endif
