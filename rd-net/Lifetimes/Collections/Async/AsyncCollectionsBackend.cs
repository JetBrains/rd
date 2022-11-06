using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Threading;

namespace JetBrains.Collections.Async;
#if !NET35

internal abstract class AsyncCollectionsBackend<TState, TKey, TValue> : ITerminationHandler
{
  private readonly object myLocker = new();
  private IScheduler myScheduler;
  
  private TState myState;
  private int myReadersCount;
  private int myVersion;
  private bool myHasValue;
  private int myUnprocessedChangesCount;

  private LifetimedList<Action<AddUpdateRemove,TKey,  TValue>> myListeners;

  protected AsyncCollectionsBackend(TState defaultState, IScheduler scheduler)
  {
    myState = defaultState ?? throw new ArgumentNullException(nameof(defaultState));
    myScheduler = scheduler;

    Assertion.Assert(scheduler is not SynchronousScheduler);
    Assertion.Assert(!scheduler.OutOfOrderExecution);
  }

  [MustUseReturnValue]
  public ReadonlyStateCookie GetSnapshotCookie() => new(this);

  public Task UpdateValueAsync(AddUpdateRemove kind, TKey key, TValue value)
  {
    LifetimedList<Action<AddUpdateRemove, TKey, TValue>>.Snapshot listeners;
    
    lock (myLocker)
    {
      var maybe = myHasValue && myReadersCount > 0 ? Copy(myState!) : myState!;

      myState = DoUpdate(maybe, kind, key, value);
      myHasValue = true;
      myVersion++;
      myReadersCount = 0;

      listeners = myListeners.GetSnapshot();
      if (listeners.Count == 0)
        return Task.CompletedTask;
      
      if (myUnprocessedChangesCount > 0 || !myScheduler.IsActive)
        return FireAsync(kind, key, value, listeners);
    }

    foreach (var listener in listeners)
    {
      try
      {
        listener(kind, key, value);
      }
      catch (Exception e)
      {
        Log.Root.Error(e);
      }
    }
    
    return Task.CompletedTask;
  }

  // do not inline this method to avoid creating an object with closure
  private Task FireAsync(AddUpdateRemove kind, TKey key, TValue value, LifetimedList<Action<AddUpdateRemove, TKey, TValue>>.Snapshot listeners)
  {
    AssertMonitorIsEntered();
    var tcs = new TaskCompletionSource<Unit>();
    
    myUnprocessedChangesCount++;
    myScheduler.Queue(() =>
    {
      lock (myLocker)
        myUnprocessedChangesCount--;

      foreach (var listener in listeners)
      {
        try
        {
          listener(kind, key, value);
        }
        catch (Exception e)
        {
          Log.Root.Error(e);
        }
      }

      tcs.SetResult(Unit.Instance);
    });
    return tcs.Task;
  }

  [Conditional("JET_MODE_ASSERT")]
  private void AssertMonitorIsEntered() => Assertion.Assert(Monitor.IsEntered(myLocker));

  public Task AdviseAsync(Lifetime lifetime, Action<AddUpdateRemove, TKey, TValue> action)
  {
    ReadonlyStateCookie readonlyStateCookie;

    using (var cookie = new MutexCookie(lifetime, myLocker))
    {
      if (!cookie.Success)
        return Task.FromCanceled<LifetimeCanceledException>(lifetime);

      myListeners.Add(lifetime, action);
      
      if (!myHasValue)
        return Task.CompletedTask;

      readonlyStateCookie = GetSnapshotCookie();

      if (myUnprocessedChangesCount > 0 || !myScheduler.IsActive)
        return FireStateAsync(lifetime, readonlyStateCookie, action);
    }

    using (readonlyStateCookie)
      DoFireState(readonlyStateCookie.State, action);

    return Task.CompletedTask;
  }

  private Task FireStateAsync(Lifetime lifetime, ReadonlyStateCookie readonlyStateCookie, Action<AddUpdateRemove, TKey, TValue> action)
  {
    var nested = lifetime.CreateNested(); // to avoid memory leak
    var tcs = new TaskCompletionSource<Unit>();
        
    if (!nested.Lifetime.TryOnTermination(() => Interlocked.Exchange(ref tcs, null)?.SetCanceled()))
      return Task.CompletedTask;

    myUnprocessedChangesCount++;
    myScheduler.Queue(() =>
    {
      lock (myLocker) 
        myUnprocessedChangesCount--;

      var localTcs = Interlocked.Exchange(ref tcs, null);
      if (localTcs == null)
        return;

      using (readonlyStateCookie)
        DoFireState(readonlyStateCookie.State, action);

      localTcs.SetResult(Unit.Instance);
      nested.Terminate();
    });
    
    return tcs.Task;
  }

  protected static void Execute(AddUpdateRemove kind, TKey key, TValue change, Action<AddUpdateRemove, TKey, TValue> listener)
  {
    try
    {
      listener(kind, key, change);
    }
    catch (Exception e)
    {
      Log.Root.Error(e);
    }
  }

  public void OnTermination(Lifetime lifetime)
  {
    myListeners.ClearValuesIfNotAlive();
  }

  protected abstract TState DoUpdate(TState state, AddUpdateRemove kind, TKey key, TValue element);
  protected abstract TState Copy(TState state);
  protected abstract void DoFireState(TState? state, Action<AddUpdateRemove, TKey, TValue> listener);

  public readonly struct ReadonlyStateCookie : IDisposable
  {
    private readonly int myVersion;
    private readonly AsyncCollectionsBackend<TState, TKey, TValue> myBackend;
    
    public readonly TState State;

    public ReadonlyStateCookie(AsyncCollectionsBackend<TState, TKey, TValue> backend)
    {
      lock (backend.myLocker)
      {
        myVersion = backend.myVersion;
        myBackend = backend;
        State = backend.myState;
        myBackend.myReadersCount++;
      }
    }
    
    public void Dispose()
    {
      if (myBackend == null) 
        return;
      
      lock (myBackend.myLocker)
      {
        if (myVersion != myBackend.myVersion)
          return;
        
        Assertion.Assert(myBackend.myReadersCount > 0);
        myBackend.myReadersCount--;
      }
    }
  }
}
#endif
