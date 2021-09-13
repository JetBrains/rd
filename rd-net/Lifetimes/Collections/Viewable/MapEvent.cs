using System.Diagnostics.CodeAnalysis;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Event of <see cref="IViewableMap{K,V}"/>
  /// </summary>
  /// <typeparam name="K"></typeparam>
  /// <typeparam name="V"></typeparam>
  public struct MapEvent<K, V> where K : notnull
  {    
    public AddUpdateRemove Kind { get; private set; }
    public K Key { get; private set; }
    public V? OldValue { get; private set; }
    public V? NewValue { get; private set; }

    [MemberNotNullWhen(true, nameof(NewValue))]
    public bool IsAdd => Kind == AddUpdateRemove.Add;

    [MemberNotNullWhen(true, nameof(NewValue))]
    [MemberNotNullWhen(true, nameof(OldValue))]
    public bool IsUpdate => Kind == AddUpdateRemove.Update;

    [MemberNotNullWhen(true, nameof(OldValue))]
    public bool IsRemove => Kind == AddUpdateRemove.Remove;

    private MapEvent(AddUpdateRemove kind, K key, V oldValue, V newValue)
      : this()
    {
      Kind = kind;
      Key = key;
      OldValue = oldValue;
      NewValue = newValue;
    }

    public static MapEvent<K, V> Add(K key, V newValue)
    {
      return new MapEvent<K, V>(AddUpdateRemove.Add, key, default!, newValue);
    }

    public static MapEvent<K, V> Update(K key, V oldValue, V newValue)
    {
      return new MapEvent<K, V>(AddUpdateRemove.Update, key, oldValue, newValue);
    }

    public static MapEvent<K, V> Remove(K key, V oldValue)
    {
      return new MapEvent<K, V>(AddUpdateRemove.Remove, key, oldValue, default!);
    }

    public override string ToString()
    {
      return $"{nameof(Kind)}: {Kind}, {nameof(Key)}: {Key}, {nameof(OldValue)}: {OldValue}, {nameof(NewValue)}: {NewValue}";
    }
  }
}