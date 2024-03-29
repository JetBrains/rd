using System;
using System.Collections.Generic;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable;

public interface IAppendOnlyViewableConcurrentSet<T> : IEnumerable<T>
{
  int Count { get; }
    
  bool Add(T value);
  bool Contains(T value);

  void View(Lifetime lifetime, Action<Lifetime, T> action);
}

public interface IViewableConcurrentSet<T> : IAppendOnlyViewableConcurrentSet<T>
{
  bool Remove(T value);
}