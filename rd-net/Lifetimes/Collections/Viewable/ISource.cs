using System;
using JetBrains.Annotations;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Analogue of .NET event but subscription could be done only with <see cref="Lifetime"/>  
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public interface ISource<
#if !NET35
    out
#endif
    T>
  {
    
    /// <summary>
    /// Subscribes for this signal (if lifetime isn't terminated). <paramref name="handler"/> will be unsubscribed when lifetime become <see cref="LifetimeStatus.Terminated"/>   
    /// </summary>
    /// <param name="lifetime"></param>
    /// <param name="handler"></param>
    void Advise(Lifetime lifetime, [NotNull] Action<T> handler);
  }
}