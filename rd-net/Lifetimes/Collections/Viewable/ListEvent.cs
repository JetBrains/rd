using System.Diagnostics.CodeAnalysis;
using JetBrains.Annotations;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Event of <see cref="IViewableList{T}"/>
  /// </summary>
  /// <typeparam name="V"></typeparam>
  public struct ListEvent<V> where V : notnull
  {
    public AddUpdateRemove Kind { get; private set; }
    public int Index { get; private set; }
    public V? OldValue { get; private set; }
    public V? NewValue { get; private set; }

    private ListEvent(AddUpdateRemove kind, int index, V? oldValue, V? newValue)
      : this()
    {
      Kind = kind;
      Index = index;
      OldValue = oldValue;
      NewValue = newValue;
    }

    public static ListEvent<V> Add(int index, V newValue)
    {
      return new ListEvent<V>(AddUpdateRemove.Add, index, default(V), newValue);
    }

    public static ListEvent<V> Update(int index, V oldValue, V newValue)
    {
      return new ListEvent<V>(AddUpdateRemove.Update, index, oldValue, newValue);
    }

    public static ListEvent<V> Remove(int index, V oldValue)
    {
      return new ListEvent<V>(AddUpdateRemove.Remove, index, oldValue, default(V));
    }

    public override string ToString()
    {
      return $"{nameof(Kind)}: {Kind}, {nameof(Index)}: {Index}, {nameof(OldValue)}: {OldValue}, {nameof(NewValue)}: {NewValue}";
    }
  }
}