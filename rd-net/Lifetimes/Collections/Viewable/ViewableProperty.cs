using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Threading;
using JetBrains.Util.Internal;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Default implementation of <see cref="IViewableProperty{T}"/>. Synchronized internally.
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public class ViewableProperty<T> : IViewableProperty<T>
  {
    private static readonly bool ourIsReadWriteAtomic = Memory.IsReadWriteAtomic<T>();

    private readonly Signal<T> myChange = new Signal<T>();

    private T myValue = default!;
    private const long ValueNotSetFlag = 0;
    private const long InProgressIncrement = 1;
    private const long SetIncrement = 2;
    private long myTimestamp = ValueNotSetFlag;

    public ISource<T> Change => myChange;

    private bool HasValue => Volatile.Read(ref myTimestamp) >= (ValueNotSetFlag + SetIncrement);

    public Maybe<T> Maybe 
    {
      get
      {
        if (!HasValue)
        {
          return Maybe<T>.None;
        }
        
        if (ourIsReadWriteAtomic)
        {
          return new Maybe<T>(myValue);
        }
        
        return GetBigValueSlowNoLock();
      }
    }

    private Maybe<T> GetBigValueSlowNoLock()
    {
      var spinWait = new SpinWait();
      while (true)
      {
        var timestamp = Volatile.Read(ref myTimestamp);
        if ((timestamp & InProgressIncrement) != 0) // myValue is being written not -> wait
        {
          spinWait.SpinOnceWithoutSleep();
          continue;
        }

        var value = myValue;
        Interlocked.MemoryBarrier();
        var timestampAfter = Volatile.Read(ref myTimestamp);
        if (timestampAfter != timestamp) // changed during read, retry ->
        {
          spinWait.SpinOnceWithoutSleep();
          continue;
        }
        
        return new Maybe<T>(value);
      }
    }

    public ViewableProperty() {}

    public ViewableProperty(T value) : this()
    {
      // ReSharper disable once VirtualMemberCallInConstructor
      Value = value;
    }


    public virtual T Value
    {
      get { return Maybe.OrElseThrow(() => new InvalidOperationException("Not initialized")); }

      set
      {
        lock (myChange)
        {
          if (HasValue && EqualityComparer<T>.Default.Equals(myValue, value))
          {
            return;
          }
          
          var timestampBefore = Volatile.Read(ref myTimestamp);
          // use volatile for atomic write on x86
          Volatile.Write(ref myTimestamp, timestampBefore + InProgressIncrement);
          Interlocked.MemoryBarrier(); // myValue must be set strictly after myTimestamp  
          myValue = value;
          Volatile.Write(ref myTimestamp, timestampBefore + SetIncrement); // myTimestamp must be updated strictly after myValue is set 
          
          // After optimizing signal, `Fire` no longer provides a full memory fence (triggered by `Interlocked.CompareExchange`).
          // This caused our tests to become flaky because we rely on `Fire(value)` being observed strictly after `myValue = value`;
          // To enforce this ordering, we explicitly add a memory barrier here.
          Interlocked.MemoryBarrier();
          // todo move fire out of lock to avoid deadlocks (breaking change)
          myChange.Fire(value);
        }
      }
    }

    public void Advise(Lifetime lifetime, Action<T> handler)
    {
      //todo replace by IsAlive after tests
      if (lifetime.Status >= LifetimeStatus.Terminating) return;

      lock (myChange)
      {
        myChange.Advise(lifetime, handler);
        try
        {
          // todo move handler our of lock to avoid deadlock (breaking change)
          if (Maybe.HasValue) handler(Value);
        }
        catch (Exception e)
        {
          Log.Root.Error(e);
        }
      }
    }

    
    //todo make interlocked
    public bool SetIfEmpty(T value)
    {
      if (Maybe.HasValue)
        return false;

      Value = value;
      return true;
    }
  }
}