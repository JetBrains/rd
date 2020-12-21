using System;
using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Base;

namespace JetBrains.Rd.Reflection
{
  /// <summary>
  /// A special scheduler which can be globally temporarily switched to another implementation.
  /// </summary>
  public class SwitchingScheduler : IScheduler
  {
    private readonly IRdDynamic myFallbackSchedulerSource;

    private static readonly object ourLock = new object();
    private static readonly Stack<IScheduler> ourSchedulersOverride = new Stack<IScheduler>();

    public bool IsActive  => ActiveScheduler.IsActive;
    public bool OutOfOrderExecution  => ActiveScheduler.OutOfOrderExecution;

    public IScheduler ActiveScheduler
    {
      get
      {
        IScheduler scheduler;
        lock (ourLock)
        {
          scheduler = myFallbackSchedulerSource.Proto.Scheduler;
          if (ourSchedulersOverride.Count > 0)
            scheduler = ourSchedulersOverride.Peek();
        }

        return scheduler;
      }
    }

    public SwitchingScheduler(IRdDynamic fallbackSchedulerSource)
    {
      myFallbackSchedulerSource = fallbackSchedulerSource;
    }

    public void Queue(Action action)
    {
      ActiveScheduler.Queue(action);
    }

    public readonly struct SwitchCookie : IDisposable
    {
      /// <summary>
      /// Default constructor detector
      /// </summary>
      private readonly bool myIsValid;

      public SwitchCookie(IScheduler scheduler)
      {
        myIsValid = true;
        lock (ourLock)
          ourSchedulersOverride.Push(scheduler);
      }

      public void Dispose()
      {
        if (myIsValid)
        {
          lock (ourLock)
            ourSchedulersOverride.Pop();
        }
      }
    }
  }
}
