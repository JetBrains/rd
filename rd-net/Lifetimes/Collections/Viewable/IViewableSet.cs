using System.Collections.Generic;

namespace JetBrains.Collections.Viewable
{
  public interface IViewableSet<T> : ISource<SetEvent<T>>,
    #if NET35
      ICollection<T>
    #else
      ISet<T>
    #endif
  {
    ISource<SetEvent<T>> Change { get; }
  }
}