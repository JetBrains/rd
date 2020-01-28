namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Mutable <see cref="IReadonlyProperty{T}"/>.
  /// If you put not equal values (in terms of <see cref="object.Equals(object)"/>) into
  /// <see cref="Value"/> than no event will be fired and <see cref="ISource{T}.Advise"/>'s handler
  /// won't be triggered. So code
  /// <code>
  /// //suppose property.Value != 1
  /// property.Value = 1;
  /// property.Value = 1;
  /// </code>
  /// will fire only one event with value 1.
  /// 
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