using System.Collections.Generic;
using JetBrains.Annotations;

namespace JetBrains.Collections.Viewable
{
  

  /// <summary>
  /// Observable dictionary.
  /// Invocation <see cref="ISource{T}.Advise"/> for this list will synchronously invoke
  /// <c>handler</c> with <see cref="MapEvent{K,V}.Kind"/> == <see cref="AddUpdateRemove.Add"/> for
  /// each existing element of this collection.
  /// <remarks>See <see cref="ReactiveEx.View{K,V}(IViewableMap{K,V},JetBrains.Lifetimes.Lifetime,System.Action{JetBrains.Lifetimes.Lifetime,K,V})"/>
  /// for structured subscription.
  /// </summary>
  /// <typeparam name="K"></typeparam>
  /// <typeparam name="V"></typeparam>
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