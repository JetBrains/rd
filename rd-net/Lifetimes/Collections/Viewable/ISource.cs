using System;
using JetBrains.Annotations;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  public interface ISource<
#if !NET35
    out
#endif
    T>
  {
    
    void Advise(Lifetime lifetime, [NotNull] Action<T> handler);
  }
}