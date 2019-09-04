using JetBrains.Annotations;

namespace JetBrains.Collections.Viewable
{
  public struct MapEvent<K, V>
  {    
    public AddUpdateRemove Kind { get; private set; }
    public K Key { [NotNull] get; private set; }
    public V OldValue { [CanBeNull] get; private set; }
    public V NewValue { [CanBeNull] get; private set; }

    private MapEvent(AddUpdateRemove kind, [NotNull] K key, [CanBeNull] V oldValue, [CanBeNull] V newValue)
      : this()
    {
      Kind = kind;
      Key = key;
      OldValue = oldValue;
      NewValue = newValue;
    }

    public static MapEvent<K, V> Add([NotNull] K key, [NotNull] V newValue)
    {
      return new MapEvent<K, V>(AddUpdateRemove.Add, key, default(V), newValue);
    }

    public static MapEvent<K, V> Update([NotNull] K key, [NotNull] V oldValue, [NotNull] V newValue)
    {
      return new MapEvent<K, V>(AddUpdateRemove.Update, key, oldValue, newValue);
    }

    public static MapEvent<K, V> Remove([NotNull] K key, [NotNull] V oldValue)
    {
      return new MapEvent<K, V>(AddUpdateRemove.Remove, key, oldValue, default(V));
    }

    public override string ToString()
    {
      return $"{nameof(Kind)}: {Kind}, {nameof(Key)}: {Key}, {nameof(OldValue)}: {OldValue}, {nameof(NewValue)}: {NewValue}";
    }
  }
}