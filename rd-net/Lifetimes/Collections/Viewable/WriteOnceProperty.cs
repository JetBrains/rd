using System;
using JetBrains.Core;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  public sealed class WriteOnceProperty<T> : IViewableProperty<T>
  {
    private readonly WriteOnceSignal mySignal = new WriteOnceSignal();
    private readonly object myLock = new object();
    private Maybe<T> myMaybe;
    
    public ISource<T> Change => mySignal;

    public Maybe<T> Maybe
    {
      get
      {
        lock (myLock) // to avoid non-atomic read
          return myMaybe;
      }
    }

    public T Value
    {
      get
      {
        return Maybe.OrElseThrow(() => new InvalidOperationException("Not initialized"));
      }
      set
      {
        if (!SetIfEmpty(value))
          throw new InvalidOperationException($"WriteOnceProperty is already set with `{value}`, but you're trying to rewrite it with `{value}`");
      }
    }
    
    public bool SetIfEmpty(T newValue)
    {
      lock(myLock)
      {
        if (myMaybe.HasValue)
          return false;

        myMaybe = new Maybe<T>(newValue);
      }

      mySignal.Fire(newValue);
      return true;
    }

    public void Advise(Lifetime lifetime, Action<T> handler)
    {
      if (lifetime.IsNotAlive)
        return;

      Maybe<T> local;
      lock (myLock)
      {
        local = myMaybe;
        if (!local.HasValue) // to avoid calling the handler twice
        {
          mySignal.Advise(lifetime, handler);
          return;
        }
      }

      handler(local.Value);
    }

    public void AdviseOn(Lifetime lifetime, IScheduler scheduler, Action<T> handler)
    {
      Advise(lifetime, value => scheduler.InvokeOrQueue(() => handler(value)));
    }

    public void WithValue<TContext>(Lifetime lifetime, IScheduler scheduler, TContext context, Action<TContext, T> handler)
    {
      var maybe = Maybe;
      if (maybe.HasValue && scheduler.IsActive)
      {
        handler(context, maybe.Value);
      }
      else
      {
        Dispatch(lifetime, scheduler, context, handler);
      }
    }

    private void Dispatch<TContext>(Lifetime lifetime, IScheduler scheduler, TContext context, Action<TContext, T> handler)
    {
      AdviseOn(lifetime, scheduler, value => handler(context, value));
    }

    // for test
    internal void fireInternal(T value) => mySignal.Fire(value);
    
    private sealed class WriteOnceSignal : SignalBase<T>
    {
      private readonly LifetimeDefinition myDef = new LifetimeDefinition();
      public Lifetime Lifetime => myDef.Lifetime;
      
      public override void Advise(Lifetime lifetime, Action<T> handler)
      {
        if (Lifetime.IsNotAlive || lifetime.IsNotAlive) return;

        var nestedLifetime = Lifetime.Intersect(lifetime);
        base.Advise(nestedLifetime, handler);
        
      }
      public override void Fire(T value)
      {
        try
        {
          base.Fire(value);
        }
        finally
        {
          myDef.Terminate();
        }
      }
    }
  }
}