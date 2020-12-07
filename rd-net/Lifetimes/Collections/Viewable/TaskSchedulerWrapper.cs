using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Threading;

namespace JetBrains.Collections.Viewable
{
  public class TaskSchedulerWrapper : IScheduler
  {
    private readonly ThreadLocal<int> myExecutionCount = new ThreadLocal<int>();
    private readonly TaskScheduler myScheduler;

    public bool IsActive => myExecutionCount.Value > 0;
    public bool OutOfOrderExecution { get; }

    public TaskSchedulerWrapper(TaskScheduler scheduler, bool outOfOrderExecution)
    {
      myScheduler = scheduler;
      OutOfOrderExecution = outOfOrderExecution;
    }

    public void Queue(Action action)
    {
      Task.Factory.StartNew(() =>
      {
        myExecutionCount.Value++;
        try
        {
          action();
        }
        finally
        {
          myExecutionCount.Value--;
        }
      }, CancellationToken.None, TaskCreationOptions.None, myScheduler).NoAwait();
    }
  }
}