namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Mutable <see cref="IReadonlyProperty{T}"/>.
  /// You must set distinct values (in terms of <see cref="object.Equals(object)"/>) or no event will be fired. 
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public interface IViewableProperty<T> : IReadonlyProperty<T>
  {
    /// <summary>
    /// If value being set is equal to <see cref="Value"/> no event is being fired and handler
    /// subscribed by <see cref="ISource{T}.Advise"/> are not triggered.
    /// </summary>
    new T Value { get;  set; }
  }
}