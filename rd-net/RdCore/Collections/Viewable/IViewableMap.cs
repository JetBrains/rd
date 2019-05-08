using System.Collections.Generic;
using JetBrains.Annotations;

namespace JetBrains.Collections.Viewable
{
  public interface IViewableMap<K, V> : IDictionary<K, V>, ISource<MapEvent<K, V>>
  {
    ISource<MapEvent<K, V>> Change { get; }

    // note: solve interface ambiguity
    new int Count { get; }
    [NotNull] new ICollection<K> Keys { get; }
    [NotNull] new ICollection<V> Values { get; }
    new bool ContainsKey([NotNull] K key);
    new V this[[NotNull] K key] { get; set; }
    new bool TryGetValue([NotNull] K key, out V value);
  }
}