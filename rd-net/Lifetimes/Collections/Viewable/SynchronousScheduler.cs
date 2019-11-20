using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  public class SynchronousScheduler : TaskScheduler, IScheduler
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