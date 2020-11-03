using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Serialization;
using JetBrains.Threading;

namespace JetBrains.Collections.Viewable
{
  public class PrioritizedAction : IComparable<PrioritizedAction>
  {
    public const int HighPriority = 32;
    public const int NormalPriority = 0;
    public const int LowPriority = -32;
    
    public PrioritizedAction([NotNull] Action action, int priority = NormalPriority)
    {
      Priority = priority;
      Action = action ?? throw new ArgumentNullException(nameof(action));
    }

    public int Priority { get; }
    public Action Action { get; }

    public int CompareTo(PrioritizedAction other)
    {
      if (ReferenceEquals(this, other)) return 0;
      if (ReferenceEquals(null, other)) return 1;
      return - Priority.CompareTo(other.Priority);
    }
  }

  /// <summary>
  /// Task scheduler that either creates separate thread (via <see cref="RunOnSeparateThread"/> or use current
  /// (via <see cref="CreateOverExisting"/>). All enqueued tasks are executed sequentially. 
  /// </summary>
  public class SingleThreadScheduler : TaskScheduler, IScheduler
  {
    class ActionQueue
    {
      internal readonly BlockingPriorityQueue<PrioritizedAction> Storage;
      internal volatile int ToProcessCount;

      public ActionQueue(Lifetime lifetime)
      {
        Storage = new BlockingPriorityQueue<PrioritizedAction>(lifetime);
      }
    }
    
    
    private readonly ILog myLog;
    
    public string Name { get; }        
    public int ActionPriority { get; }
    public Thread Thread { get; private set; }

    private ActionQueue myQueue;


    private SingleThreadScheduler([NotNull] string name, [NotNull] ActionQueue queue, int actionPriority = PrioritizedAction.NormalPriority)
    {
      myQueue = queue ?? throw new ArgumentNullException(nameof(queue));
      Name = name ?? throw new ArgumentNullException(nameof(name));

      ActionPriority = actionPriority;

      myLog = Log.GetLog<SingleThreadScheduler>().GetSublogger(Name);
    }


    [PublicAPI]
    public static void RunInCurrentStackframe([NotNull] Lifetime lifetime, [NotNull] string name, Action<SingleThreadScheduler> beforeStart = null)
    {
      var res = new SingleThreadScheduler(name, new ActionQueue(lifetime)) { Thread = Thread.CurrentThread };

      beforeStart?.Invoke(res);

      res.Run();
    }

    [PublicAPI]
    public static SingleThreadScheduler RunOnSeparateThread([NotNull] Lifetime lifetime, [NotNull] string name, Action<SingleThreadScheduler> beforeStart = null)
    {

      var res = new SingleThreadScheduler(name, new ActionQueue(lifetime));
      var thread = new Thread(() => res.Run()) { Name = name };
      res.Thread = thread;

      beforeStart?.Invoke(res);

      thread.Start();
      return res;
    }


    public static SingleThreadScheduler CreateOverExisting([NotNull] SingleThreadScheduler existingScheduler, [NotNull] string name, int actionPriority = PrioritizedAction.NormalPriority)
    {
      if (existingScheduler == null) throw new ArgumentNullException(nameof(existingScheduler));
      if (name == null) throw new ArgumentNullException(nameof(name));

      return new SingleThreadScheduler(name, existingScheduler.myQueue, actionPriority) { Thread = existingScheduler.Thread};
    }



    public bool IsIdle => myQueue.ToProcessCount == 0;

    //Could be annotated by CallStackAnnotation.AnnotateAction()
    //could throw OCE as a valid result
    private void ExecuteOneAction(bool blockIfNoActionAvailable)
    {
      Assertion.AssertCurrentThread(Thread);

      PrioritizedAction prioritizedAction = blockIfNoActionAvailable ? myQueue.Storage.ExtractOrBlock() : myQueue.Storage.ExtractOrDefault();

      if (prioritizedAction == null) return;
      var action = prioritizedAction.Action;

      try
      {        
        action();
      }
      catch (Exception e)
      {
        myLog.Error(e);
      }
      finally
      {
        Interlocked.Decrement(ref myQueue.ToProcessCount);
      }
    }


    private void Run()
    {
      using (new FirstChanceExceptionInterceptor.ThreadLocalDebugInfo(this))
      {
        try
        {
          while (true)
          {
            ExecuteOneAction(blockIfNoActionAvailable: true);
          }
        }
        catch (OperationCanceledException) //that's ok
        {
        }
        catch (Exception e)
        {
          myLog.Error(e, $"Abnormal termination of {this}");
        }
      }
    }

#if !NET35    
    public bool PumpAndWaitFor(Lifetime lifetime, TimeSpan timeout, Func<bool> condition)
    {
      var shouldPump = IsActive;

      return SpinWaitEx.SpinUntil(lifetime, timeout, () =>
      {
        if (condition()) return true;

        if (shouldPump)
        {
          ExecuteOneAction(blockIfNoActionAvailable: false);
        }

        return false;
      });
    }

    public bool PumpAndWaitFor(Lifetime lifetime, Func<bool> condition)
    {
      return PumpAndWaitFor(lifetime, TimeSpan.MaxValue, condition);
    }

    public bool PumpAndWaitFor(Func<bool> condition)
    {
      return PumpAndWaitFor(Lifetime.Eternal, TimeSpan.MaxValue, condition);
    }
#endif



    public override string ToString()
    {
      return $"Scheduler: '{Name}' on thread `{Thread.ToThreadString()}`";
    }

    public void Queue([NotNull] Action action)
    {
      if (action == null) throw new ArgumentNullException(nameof(action));

      Interlocked.Increment(ref myQueue.ToProcessCount);
      myQueue.Storage.Add(new PrioritizedAction(action, ActionPriority));
    }



    public bool IsActive => Thread.CurrentThread == Thread;

    public bool OutOfOrderExecution => false;

    

    #region TaskScheduler
    
    protected override void QueueTask(Task task)
    {
      Queue(() => TryExecuteTask(task));
    }

    protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued)
    {
      return false;
    }

    protected override IEnumerable<Task> GetScheduledTasks()
    {
      yield break;
    }
    
    #endregion
  }
}