using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading.Tasks;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Scheduler that executes task and action synchronously
  /// Perfect candidate for <see cref="Task.ContinueWith(System.Action{System.Threading.Tasks.Task,object},object)"/>
  /// if you want to guarantee synchronous continuation
  /// </summary>
  public class SynchronousScheduler : TaskScheduler, IRunWhileScheduler
  {
    public static readonly SynchronousScheduler Instance = new SynchronousScheduler();

    [ThreadStatic] private static int ourActive;

    public void SetActive(Lifetime lifetime)
    {      
      lifetime.Bracket(() => { ourActive++; }, () => { ourActive--; });
    }

    private SynchronousScheduler(){}

    public void Queue(Action action) => Execute(action);

    private static void Execute(Action action)
    {
      try
      {
        ourActive++;
        action();
      }
      finally
      {
        ourActive--;
      }
    }

    public bool IsActive => ourActive > 0;
    public bool OutOfOrderExecution => false;

    public bool RunWhile(Func<bool> condition, TimeSpan timeout)
    {
      // SynchronousScheduler executes actions inline when queued, so by the time
      // RunWhile is called the condition is typically already satisfied.
      var stopwatch = timeout == TimeSpan.MaxValue ? null : Stopwatch.StartNew();
      while (condition())
      {
        if (stopwatch != null && stopwatch.Elapsed >= timeout)
        {
          return false;
        }
      }
      return true;
    }


    
    #region Implementation of TaskScheduler
    
    protected override IEnumerable<Task> GetScheduledTasks()
    {
      yield break;
    }

    protected override void QueueTask(Task task)
    {
      Execute(() => TryExecuteTask(task));
    }

    protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued)
    {
      try
      {
        ourActive++;
        return TryExecuteTask(task);
      }
      finally
      {
        ourActive--;
      }
    }
    
    #endregion
  }
}