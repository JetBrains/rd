using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;

namespace JetBrains.Rd.Impl
{
  /// <summary>
  /// A scheduler with a ability to join all queued tasks at some point and execute them synchronously when necessary
  /// </summary>
  public class StealingScheduler : TaskScheduler
  {
    private readonly bool myAllowParallelProcessing;
    [NotNull] private readonly TaskScheduler myScheduler;

    /// <summary>
    /// >0: The number of currently simultaneously running tasks.
    /// -1: exclusive mode taken by Join when <see cref="myAllowParallelProcessing"/> is false, no tasks allowed to process 
    /// </summary>
    private volatile int myActive;
    [NotNull] private readonly ConcurrentQueue<Task> myActions = new ConcurrentQueue<Task>();

    /// <summary>
    /// Creates stealing scheduler
    /// </summary>
    /// <param name="scheduler">Scheduler which used to queue actions in <see cref="ConcurrentQueue"/></param>
    /// <param name="allowParallelProcessing">
    /// Indicates whether is it safe to execute tasks simultaneously in <see cref="Join"/> method and provided scheduler.
    /// Note, that this is your responsibility to provide limited concurrency scheduler when concurrent processing is forbidden.
    /// </param>
    public StealingScheduler([CanBeNull] TaskScheduler scheduler, bool allowParallelProcessing = true)
    {
      myAllowParallelProcessing = allowParallelProcessing;
      myScheduler = scheduler ?? Default;
    }

    protected override IEnumerable<Task> GetScheduledTasks()
    {
      return myActions;
    }

    protected override void QueueTask(Task task)
    {
      myActions.Enqueue(task);
      if (myActive < myScheduler.MaximumConcurrencyLevel)
        new Task(ProcessTasks).Start(myScheduler);
    }

    protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued)
    {
      return false;
    }

    public void Join()
    {
      while (true)
      {
        if (myActions.Count == 0 && myActive == 0)
          return;

        if (myAllowParallelProcessing)
        {
          ProcessTasks();
        }
        else
        {
          // try to enter exclusive mode
          if (Interlocked.CompareExchange(ref myActive, -1, 0) == 0)
          {
            try
            {
              while (myActions.TryDequeue(out var task))
              {
                TryExecuteTask(task);
              }
            }
            finally
            {
              myActive = 0;
            }
          }
        }
        SpinWait.SpinUntil(() => myActive == 0);
      }
    }

    /// <summary>
    /// Process task from <see cref="myScheduler"/> thread or Join method when parallel processing is allowed.
    /// </summary>
    private void ProcessTasks()
    {
      while (myActions.Count > 0 && myActive != -1)
      while (ExecuteOne())
      {
      }
    }

    private bool ExecuteOne()
    {
      // action counter should always be incremented before dequeuing task to avoid race condition in Join
      while (true)
      {
        var oldVal = myActive;
        if (oldVal < 0)
          return false;
        if (Interlocked.CompareExchange(ref myActive, oldVal + 1, oldVal) == oldVal)
          break;
      }

      if (myActions.TryDequeue(out var action))
      {
        try
        {
          TryExecuteTask(action);
        }
        finally
        {
          Interlocked.Decrement(ref myActive);
        }

        return true;
      }
      else
      {
        // no more tasks available
        Interlocked.Decrement(ref myActive);
      }

      return false;
    }
  }
}