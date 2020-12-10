using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Threading;

namespace JetBrains.Collections.Viewable
{
  public static class SchedulerEx
  {
    public static void AssertThread(this IScheduler scheduler, object debugInfo = null)
    {
      if (!scheduler.IsActive)
        Log.Root.Error("Illegal scheduler for current action, must be: {0}, current thread: {1}{2}", scheduler, Thread.CurrentThread.ToThreadString(), 
          debugInfo != null ? ", debug info: "+debugInfo : "");
    }


    public static void InvokeOrQueue(this IScheduler sc, Action action)
    {      
      if (sc.IsActive) action();
      else sc.Queue(action);
    }

    public static void InvokeOrQueue(this IScheduler sc, Lifetime lifetime, Action action)
    {
      InvokeOrQueue(sc, () =>
      {
        if (lifetime.Status >= LifetimeStatus.Terminating)
          return;
        action();
      });
    }

    public static void InvokeSync(this IScheduler sc, Action action)
    {
      if (sc.IsActive) action();
      else
      {
        var e = new ManualResetEvent(false);
        sc.Queue(() =>
        {
          try
          {
            action();
          }
          finally
          {
            e.Set();
          }
        });
        e.WaitOne();
      }
    }

    public static TaskScheduler AsTaskScheduler(this IScheduler scheduler) => scheduler as TaskScheduler ?? new SchedulerWrapper(scheduler);
  }
}