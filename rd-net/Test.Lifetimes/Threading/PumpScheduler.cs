using System;
using System.Collections.Concurrent;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.Lifetimes.Threading;

public class PumpScheduler : IScheduler
{
  private volatile ConcurrentQueue<Action>? myQueue = new();
  private readonly ThreadLocal<int> myActive = new(() => 0);
  private readonly TaskSchedulerSyncContext mySyncContext;

  public bool IsActive => myActive.Value > 0;
  public bool OutOfOrderExecution { get; set; }

  public PumpScheduler(Lifetime lifetime)
  {
    mySyncContext = new(this.AsTaskScheduler());

    lifetime.OnTermination(() =>
    {
      var queue = Interlocked.Exchange(ref myQueue, null).NotNull();
      if (queue.IsEmpty)
        return;

      while (queue.TryDequeue(out var action))
        Execute(action);

      Assert.Fail("Queue must be empty");
    });
  }

  public void Queue(Action action) { myQueue.NotNull().Enqueue(action); }

  public bool PumpOnce()
  {
    if (myQueue.NotNull().TryDequeue(out var action))
    {
      Execute(action);
      return true;
    }

    return false;
  }

  private void Execute(Action action)
  {
    myActive.Value++;
    try
    {
      using var _ = mySyncContext.Cookie();
      action();
    }
    finally
    {
      myActive.Value--;
    }
  }
}