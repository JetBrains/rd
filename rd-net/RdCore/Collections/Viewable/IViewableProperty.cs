namespace JetBrains.Collections.Viewable
{
  public interface IViewableProperty<T> : IReadonlyProperty<T>
  {
    new T Value { get;  set; }
  }
}