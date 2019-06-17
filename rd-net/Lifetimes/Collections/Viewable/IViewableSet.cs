using System.Collections.Generic;

namespace JetBrains.Collections.Viewable
{
  public interface IViewableSet<T> : ICollection<T>, ISource<SetEvent<T>>
  {
    new int Count { get; }

    ISource<SetEvent<T>> Change { get; }
  }
}