using JetBrains.Annotations;
using JetBrains.Core;

namespace JetBrains.Collections.Viewable
{
  public interface IReadonlyProperty<T> : ISource<T>
  {
    ISource<T> Change { [NotNull] get; } 
    Maybe<T> Maybe { [NotNull] get; }
    T Value { get; }
  }
}