using System;
using JetBrains.Annotations;
using JetBrains.Core;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable;

public interface IAsyncSource<T>
{
  void AdviseOn(Lifetime lifetime, IScheduler scheduler, Action<T> action);
}

class AsyncSignal<T> : IAsyncSource<T>
{
  private readonly ISignal<T> mySignal = new Signal<T>();
  
  public void AdviseOn(Lifetime lifetime, IScheduler scheduler, Action<T> action)
  {
    mySignal.Advise(lifetime, value =>
    {
      scheduler.Queue(() => action(value));
    });
  }

  public void Fire(T value) => mySignal.Fire(value);
}

public interface IReadonlyAsyncProperty<T> : IAsyncSource<T>
{
  IAsyncSource<T> Change { [NotNull] get; }

  Maybe<T> Maybe { [NotNull] get; }

  T Value { get; }
}

public interface IAsyncProperty<T> : IReadonlyAsyncProperty<T>
{
  new T Value { get; set; }
}