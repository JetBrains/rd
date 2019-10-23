using System;
using System.Collections.Generic;
using JetBrains.Lifetimes;

namespace JetBrains.Rd.Base
{
  /// <summary>
  /// <para>
  /// A collection that automatically maps values to keys from RdContextKey's value set.
  /// Key-value pairs in this map are automatically managed based on possible values of RdContextKey.
  /// </para>
  ///
  /// <para>
  /// As context key value sets are protocol-specific, this map will behave differently depending on whether or not it's bound to a <see cref="IProtocol"/>.
  /// An unbound map will automatically create mappings for all context values it's accessed with. When a map is bound later, all values not present in protocol value set will be silently dropped.
  /// </para>
  /// </summary>
  /// <seealso cref="Impl.ProtocolContextHandler.GetValueSet"/>
  public interface IPerContextMap<K, V> : IRdDynamic
  {
    /// <summary>
    /// The context key that is used by this map. Must be heavy.
    /// </summary>
    RdContextKey<K> Key { get; }
    
    /// <summary>
    /// Gets the value associated with current context value, equivalent to this[Key.Value].
    /// If the context key doesn't have a value set, or key's protocol value set does not contain the current context value, this will throw an exception
    /// </summary>
    V GetForCurrentContext();
    
    void View(Lifetime lifetime, Action<Lifetime, KeyValuePair<K, V>> handler);
    void View(Lifetime lifetime, Action<Lifetime, K, V> handler);
    
    /// <summary>
    /// Gets the value associated with specified context value, or throws an exception if none is associated.
    /// When this map is not bound, this will automatically create a new mapping instead of throwing
    /// </summary>
    V this[K key] { get; }
    
    /// <summary>
    /// Gets the value associated with specified context value and returns true, or returns false if none is associated
    /// When this map is not bound, this will automatically create a new mapping instead of returning false
    /// </summary>
    bool TryGetValue(K key, out V value);
  }
}