using System;
using System.Collections.Generic;
using JetBrains.Lifetimes;

namespace JetBrains.Rd.Base
{
  public interface IPerContextMap<K, V>
  {
    RdContextKey<K> Key { get; }
    V GetForCurrentContext();
    void View(Lifetime lifetime, Action<Lifetime, KeyValuePair<K, V>> handler);
    void View(Lifetime lifetime, Action<Lifetime, K, V> handler);
    V this[K key] { get; }
    bool TryGetValue(K key, out V value);
  }
}