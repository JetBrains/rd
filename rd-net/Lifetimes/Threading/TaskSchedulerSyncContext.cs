using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;

namespace JetBrains.Threading;

public class TaskSchedulerSyncContext : SynchronizationContext
{
  private readonly TaskScheduler myScheduler;

  public TaskSchedulerSyncContext(TaskScheduler scheduler)
  {
    myScheduler = scheduler;
  }

  public override void Post(SendOrPostCallback d, object state)
  {
    Task.Factory.StartNew(() =>
    {
      try
      {
        d(state);
      }
      catch (Exception e)
      {
        Log.Root.Error(e);
      }
    }, CancellationToken.None, TaskCreationOptions.None, myScheduler);
  }
}

public readonly ref struct SyncContextCookie
{
  private readonly SynchronizationContext? myOld;
  private readonly bool myIsNonEmpty;

  [Obsolete("Do not use default constructor", true)]
  public SyncContextCookie()
  {
    myIsNonEmpty = false;
    myOld = null;
  }

  public SyncContextCookie(SynchronizationContext? context)
  {
    myIsNonEmpty = true;
    myOld = SynchronizationContext.Current;
    SynchronizationContext.SetSynchronizationContext(context);
  }

  public void Dispose()
  {
    if (myIsNonEmpty)
      SynchronizationContext.SetSynchronizationContext(myOld);
  }
}

public static class SyncContextEx
{
  public static SyncContextCookie Cookie(this SynchronizationContext context) => new(context);
}