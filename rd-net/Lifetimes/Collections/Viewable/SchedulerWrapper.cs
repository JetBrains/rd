using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;

namespace JetBrains.Collections.Viewable
{
  public class SchedulerWrapper : TaskScheduler
  {
    private readonly SyncContext mySyncContext;
    private readonly IScheduler myRealScheduler;

    public SchedulerWrapper(IScheduler realScheduler)
    {
      myRealScheduler = realScheduler;
      mySyncContext = new SyncContext(this);
    }
    
    protected override void QueueTask(Task task)
    {
      myRealScheduler.Queue(() => TryExecuteTaskWithContext(task));
    }

    protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued)
    {
      return myRealScheduler.IsActive && myRealScheduler.OutOfOrderExecution && TryExecuteTaskWithContext(task);
    }

    private bool TryExecuteTaskWithContext(Task task)
    {
      var old = SynchronizationContext.Current;
      SynchronizationContext.SetSynchronizationContext(mySyncContext);
      try
      {
        return TryExecuteTask(task);
      }
      finally
      {
        SynchronizationContext.SetSynchronizationContext(old);
      }
    }
    
    protected override IEnumerable<Task> GetScheduledTasks() => new Task[0];
    
    private class SyncContext : SynchronizationContext
    {
      private readonly SchedulerWrapper myScheduler;

      public SyncContext(SchedulerWrapper scheduler)
      {
        myScheduler = scheduler;
      }

      public override void Post(SendOrPostCallback d, object? state)
      {
        var action = new Action<object?>(d);
        Task.Factory.StartNew(action, state, CancellationToken.None, TaskCreationOptions.None, myScheduler);
      }
    }
  }
}