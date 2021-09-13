using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Lifetimes;
using JetBrains.Threading;

namespace JetBrains.Collections.Viewable
{
#if !NET35
  public sealed class SequentialScheduler : TaskScheduler, IScheduler
  {
    private readonly ThreadLocal<uint> myExecutionCount;
    
    private readonly Lifetime myLifetime;
    private readonly SyncContext mySyncContext;
    private readonly Actor<Action> myActor;
    
    public bool IsActive => myExecutionCount.Value > 0;
    public bool OutOfOrderExecution => false;

    public static SequentialScheduler FromIScheduler(string id, Lifetime lifetime, IScheduler scheduler)
    {
      return new SequentialScheduler(id, lifetime, scheduler as TaskScheduler ?? new SchedulerWrapper(scheduler));
    }

    public SequentialScheduler(string id, Lifetime lifetime, TaskScheduler? scheduler = null)
    {
      myLifetime = lifetime;
      mySyncContext = new SyncContext(this);
      myExecutionCount = new ThreadLocal<uint>();
      var def = Lifetime.Define();
      myActor = new Actor<Action>($"{id} Actor", def.Lifetime, action => action(), scheduler);
      lifetime.OnTermination(() => myActor.SendBlocking(() => def.Terminate()));
    }

    public void Queue(Action action)
    {
      using var cookie = myLifetime.UsingExecuteIfAlive();
      if (!cookie.Succeed) throw new LifetimeCanceledException(myLifetime);
      
      myActor.SendBlocking(() => ExecuteWithContext(action));
    }

    protected override void QueueTask(Task task) => Queue(() => TryExecuteTask(task));

    private void ExecuteWithContext(Action action)
    {
      var old = SynchronizationContext.Current;
      SynchronizationContext.SetSynchronizationContext(mySyncContext);
      myExecutionCount.Value++;
      try
      {
        action();
      }
      finally
      {
        myExecutionCount.Value--;
        SynchronizationContext.SetSynchronizationContext(old);
      }
    }

    protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued) => false;
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
#endif
}