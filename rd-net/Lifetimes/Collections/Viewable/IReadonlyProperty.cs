using JetBrains.Annotations;
using JetBrains.Core;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Special kind of <see cref="ISource{T}"/> that remembers last <see cref="Value"/>.
  /// <see cref="ISource{T}.Advise"/> will execute handler synchronously with <see cref="Value"/> if it was set
  /// (otherwise no synchronous execution is expected)
  /// and then subscribe for <see cref="Change"/>
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public interface IReadonlyProperty<T> : ISource<T>
  {
    /// <summary>
    /// Underlying <see cref="ISource{T}"/>. You can advise on this field if you don't want synchronous execution of
    /// handler with  <see cref="Value"/>.
    /// </summary>
    ISource<T> Change { [NotNull] get; }
    
    /// <summary>
    /// Before property was set once this field is <see cref="Maybe{T}.None"/>. After each set 
    /// <see cref="Maybe{T}.Value"/> matches <see cref="Value"/>.
    /// </summary>
    Maybe<T> Maybe { [NotNull] get; }
    
    /// <summary>
    /// The last remembered value from <see cref="Change"/>
    /// </summary>
    T Value { get; }
  }
}