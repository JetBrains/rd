using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Util;

namespace JetBrains.Threading;

#if !NET35

public class AsyncGroupingEvent
{
  #region Statics 
  
  internal static TaskScheduler ProcessingScheduler { get; private set; } = TaskScheduler.Default;
  
  public static TimeSpan MinDuration { get; private set; } = TimeSpan.FromMilliseconds(10);

  public static void OverrideMinDuration(Lifetime lifetime, TimeSpan minDuration)
  {
    lifetime.Bracket(() =>
    {
      var old = MinDuration;
      MinDuration = minDuration;
      return old;
    }, old => MinDuration = old);
  }

  // for tests only
  internal static void OverrideProcessingScheduler(Lifetime lifetime, TaskScheduler scheduler)
  {
    lifetime.Bracket(() =>
    {
      var old = ProcessingScheduler;
      ProcessingScheduler = scheduler;
      return old;
    }, old => ProcessingScheduler = old);
  }
  
  #endregion

  private readonly AsyncGroupingEvent<Unit> myGroupingEvent = new();

  public void Sample(Lifetime lifetime, TimeSpan duration, TaskScheduler scheduler, Action action)
  {
    myGroupingEvent.Sample(lifetime, duration, 0, scheduler, _ => action());
  }
  
  public void Sample(Lifetime lifetime, TimeSpan duration, TaskScheduler scheduler, Func<CancellationToken, Task> action)
  {
    myGroupingEvent.Sample(lifetime, duration, 0, scheduler, (_, token) => action(token));
  }

  public void Debounce(Lifetime lifetime, TimeSpan duration, TaskScheduler scheduler, Action action)
  {
    myGroupingEvent.Debounce(lifetime, duration, 0, scheduler, _ => action());  
  }
  
  public void Debounce(Lifetime lifetime, TimeSpan duration, TimeSpan maxDuration, TaskScheduler scheduler, Action action)
  {
    myGroupingEvent.Debounce(lifetime, duration, maxDuration, 0, scheduler, _ => action());  
  }

  public void Debounce(Lifetime lifetime, TimeSpan duration, TaskScheduler scheduler, Func<CancellationToken, Task> action)
  {
    myGroupingEvent.Debounce(lifetime, duration, 0, scheduler, (_, token) => action(token));
  }

  public void Debounce(Lifetime lifetime, TimeSpan duration, TimeSpan maxDuration, TaskScheduler scheduler, Func<CancellationToken, Task> action)
  {
    myGroupingEvent.Debounce(lifetime, duration, maxDuration, 0, scheduler, (_, token) => action(token));
  }

  public void Fire() => myGroupingEvent.Fire(Unit.Instance);
  public void Cancel() => myGroupingEvent.Cancel();
  public void Suspend(Lifetime lifetime) => myGroupingEvent.Suspend(lifetime);
}

public class AsyncGroupingEvent<T>
{
  private readonly Signal<T> myFireSignal = new();
  private readonly Signal<Unit> myCancellationSignal = new();
  private volatile int myVersion;
  private volatile int mySuspendCount;

  public void Sample(Lifetime lifetime, TimeSpan duration, TaskScheduler scheduler, Action<AppendOnlyList<T>> action)
  {
    Sample(lifetime, duration, scheduler, ToAsyncAction(action));
  }
  
  public void Sample(Lifetime lifetime, TimeSpan duration, int maxCount, TaskScheduler scheduler, Action<AppendOnlyList<T>?> action)
  {
    Sample(lifetime, duration, maxCount, scheduler, ToAsyncAction(action));
  }

  public void Sample(Lifetime lifetime, TimeSpan duration, TaskScheduler scheduler, Func<AppendOnlyList<T>, CancellationToken, Task> action)
  {
    Sample(lifetime, duration, int.MaxValue, scheduler, action!);
  }
  
  public void Sample(Lifetime lifetime, TimeSpan duration, int maxCount, TaskScheduler scheduler, Func<AppendOnlyList<T>?, CancellationToken, Task> action)
  {
    Debounce(lifetime, duration, duration, maxCount, scheduler, action!);
  }

  public void Debounce(Lifetime lifetime, TimeSpan duration, TaskScheduler scheduler, Action<AppendOnlyList<T>> action)
  {
    Debounce(lifetime, duration, scheduler, ToAsyncAction(action));
  }
  
  public void Debounce(Lifetime lifetime, TimeSpan duration, int maxCount, TaskScheduler scheduler, Action<AppendOnlyList<T>?> action)
  {
    Debounce(lifetime, duration, maxCount, scheduler, ToAsyncAction(action));  
  }

  public void Debounce(Lifetime lifetime, TimeSpan duration, TimeSpan maxDuration, TaskScheduler scheduler, Action<AppendOnlyList<T>> action)
  {
    Debounce(lifetime, duration, maxDuration, scheduler, ToAsyncAction(action));
  }
  
  public void Debounce(Lifetime lifetime, TimeSpan duration, TimeSpan maxDuration, int maxCount, TaskScheduler scheduler, Action<AppendOnlyList<T>?> action)
  {
    Debounce(lifetime, duration, maxDuration, maxCount, scheduler, ToAsyncAction(action));  
  }

  public void Debounce(Lifetime lifetime, TimeSpan duration, TaskScheduler scheduler, Func<AppendOnlyList<T>, CancellationToken, Task> action)
  {
    Debounce(lifetime, duration, int.MaxValue, scheduler, action!);  
  }
  
  public void Debounce(Lifetime lifetime, TimeSpan duration, int maxCount, TaskScheduler scheduler, Func<AppendOnlyList<T>?, CancellationToken, Task> action)
  {
    Debounce(lifetime, duration, TimeSpan.MaxValue, maxCount, scheduler, action);  
  }

  public void Debounce(Lifetime lifetime, TimeSpan duration, TimeSpan maxDuration, TaskScheduler scheduler, Func<AppendOnlyList<T>, CancellationToken, Task> action)
  {
    Debounce(lifetime, duration, maxDuration, int.MaxValue, scheduler, action!);
  }
  
  public void Debounce(Lifetime lifetime, TimeSpan duration, TimeSpan maxDuration, int maxCount, TaskScheduler scheduler, Func<AppendOnlyList<T>?, CancellationToken, Task> action)
  {
    if (duration < AsyncGroupingEvent.MinDuration) 
      duration = AsyncGroupingEvent.MinDuration;
    if (maxDuration < duration)
      maxDuration = duration;

    var subscription = new Subscription(duration, maxDuration, action, maxCount, this);
    var lifetimes = new SequentialLifetimes(lifetime);
    
    if (!lifetime.TryOnTermination(() => subscription.Close()))
      return;

    myCancellationSignal.Advise(lifetime, _ =>
    {
      subscription.Reset();
      lifetimes.TerminateCurrent(); // cancel all Task.Delay and user handler
    });
    
    myFireSignal.Advise(lifetime, subscription.Fire);
      
    lifetime.StartAsync(AsyncGroupingEvent.ProcessingScheduler, async () =>
    {
      while (lifetime.IsAlive)
      {
        try
        {
          var next = lifetimes.Next();
          await subscription.RunAsync(scheduler, next);
        }
        catch (Exception e) when (e.IsOperationCanceled())
        {
        }
        catch (Exception e)
        {
          Log.Root.Error(e);
        }
      }
      
    }).NoAwait();
  }

  public void Fire(T value)
  {
    Interlocked.Increment(ref myVersion);
    myFireSignal.Fire(value);
  }

  public void Cancel() => myCancellationSignal.Fire(Unit.Instance);

  public void Suspend(Lifetime lifetime)
  {
    lifetime.TryBracket(
      () => Interlocked.Increment(ref mySuspendCount),
      () => Interlocked.Decrement(ref mySuspendCount));
  }

  private static Func<AppendOnlyList<T>?, CancellationToken, Task> ToAsyncAction(Action<AppendOnlyList<T>> action)
  {
    return (list, _) =>
    {
      action(list!);
      return Task.CompletedTask;
    };
  }

  private class Subscription
  {
    private readonly TimeSpan myDuration;
    private readonly TimeSpan myMaxDuration;
    private readonly Func<AppendOnlyList<T>?, CancellationToken, Task> myAction;
    private readonly AsyncGroupingEvent<T> myGroupingEvent;

    private volatile Pair? myPair;

    public Subscription(
      TimeSpan duration,
      TimeSpan maxDuration,
      Func<AppendOnlyList<T>?, CancellationToken, Task> action,
      int maxCount,
      AsyncGroupingEvent<T> groupingEvent)
    {
      myDuration = duration;
      myMaxDuration = maxDuration;
      myAction = action;
      myGroupingEvent = groupingEvent;
      myPair = new Pair(maxCount);
    }

    // do not pass a lifetime so as not to mix up myLifetimes and the passed lifetime
    public async Task RunAsync(TaskScheduler taskScheduler, Lifetime lifetime)
    {
      while (lifetime.IsAlive)
      {
        var pair = myPair;
        if (pair == null)
          throw new OperationCanceledException(); // closed
        
        await pair.Tcs.Task;

        var stopwatch = LocalStopwatch.StartNew();
        var oldVersion = myGroupingEvent.myVersion;
        var duration = myDuration;
        while (true)
        {
          await Task.Delay(duration, lifetime);

          var newVersion = myGroupingEvent.myVersion;
          if (oldVersion == newVersion)
            break;
          
          var elapsed = stopwatch.Elapsed;
          if (elapsed >= myMaxDuration)
            break;

          var delta = myMaxDuration - elapsed;
          if (duration > delta)
            duration = delta;
          if (duration < AsyncGroupingEvent.MinDuration)
            duration = AsyncGroupingEvent.MinDuration;
          
          oldVersion = newVersion;
        }

        if (myGroupingEvent.mySuspendCount > 0)
        {
          await Task.Yield();
          continue;
        }
          
        await lifetime.StartAsync(taskScheduler, async () =>
        {
          if (myGroupingEvent.mySuspendCount > 0)
            return;
          
          try
          {
            pair.List?.Freeze();
            
            // try set the next pair to await for the next fire
            if (Interlocked.CompareExchange(ref myPair, pair.Next(), pair) != pair)
              throw new OperationCanceledException(); // cancelled or closed

            var list = pair.List;
            if (list is { IsFull: true }) 
              list = null; // overflow

            await myAction(list, lifetime);
          }
          catch (Exception e) when (e.IsOperationCanceled())
          {
            throw;
          }
          catch (Exception e)
          {
            Log.Root.Error(e);
          }
        });
      }
    }

    public void Reset()
    {
      if (myPair is { Tcs.Task.IsCompleted: true } pair)
      {
        // it is possible multi-thread access here, so use Interlocked.Exchange
        Interlocked.CompareExchange(ref myPair, pair.Next(), pair);
      }
    }
    
    public void Fire(T value)
    {
      var spinner = new SpinWaitEx();
      while (myPair is { } pair)
      {
        if (pair.List is { } list && !list.TryAppend(value) && list.IsFrozen)
        {
          spinner.SpinOnce(false);
          continue;
        }

        pair.Tcs.TrySetResult(Unit.Instance);
        return;
      }
    }

    public void Close() => Interlocked.Exchange(ref myPair, null)?.Tcs.TrySetCanceled();
    
    private class Pair
    {
      private readonly int myMaxCount;
      
      public readonly TaskCompletionSource<Unit> Tcs;
      public readonly AppendOnlyList<T>? List;

      public Pair(int maxCount)
      {
        myMaxCount = maxCount;
        List = maxCount > 0 ? new AppendOnlyList<T>(maxCount: maxCount == int.MaxValue ? maxCount : maxCount + 1) : null;
        Tcs = new TaskCompletionSource<Unit>(TaskCreationOptions.RunContinuationsAsynchronously);
      }

      public Pair Next() => new(myMaxCount);
    }
  }
}

#endif