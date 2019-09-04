using JetBrains.Annotations;

namespace JetBrains.Collections.Viewable
{
  public struct SetEvent<T>
  {

    public AddRemove Kind { get; private set; }
    public T Value { [NotNull] get; private set; }

    private SetEvent(AddRemove kind, T value) : this()
    {
      Kind = kind;
      Value = value;
    }

    public static SetEvent<T> Add(T value)
    {
      return new SetEvent<T>(AddRemove.Add, value);
    }

    public static SetEvent<T> Remove(T value)
    {
      return new SetEvent<T>(AddRemove.Remove, value);
    }

    public override string ToString()
    {
      return $"{nameof(Kind)}: {Kind}, {nameof(Value)}: {Value}";
    }
  }
}