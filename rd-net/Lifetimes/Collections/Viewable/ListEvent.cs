using JetBrains.Annotations;

namespace JetBrains.Collections.Viewable
{
  public struct ListEvent<V>
  {    
    public AddUpdateRemove Kind { get; private set; }
    public int Index { get; private set; }
    public V OldValue { [CanBeNull] get; private set; }
    public V NewValue { [CanBeNull] get; private set; }

    private ListEvent(AddUpdateRemove kind, int index, [CanBeNull] V oldValue, [CanBeNull] V newValue)
      : this()
    {
      Kind = kind;
      Index = index;
      OldValue = oldValue;
      NewValue = newValue;
    }

    public static ListEvent<V> Add(int index, [NotNull] V newValue)
    {
      return new ListEvent<V>(AddUpdateRemove.Add, index, default(V), newValue);
    }

    public static ListEvent<V> Update(int index, [NotNull] V oldValue, [NotNull] V newValue)
    {
      return new ListEvent<V>(AddUpdateRemove.Update, index, oldValue, newValue);
    }

    public static ListEvent<V> Remove(int index, [NotNull] V oldValue)
    {
      return new ListEvent<V>(AddUpdateRemove.Remove, index, oldValue, default(V));
    }

    public override string ToString()
    {
      return $"{nameof(Kind)}: {Kind}, {nameof(Index)}: {Index}, {nameof(OldValue)}: {OldValue}, {nameof(NewValue)}: {NewValue}";
    }
  }
}