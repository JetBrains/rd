using System;
using JetBrains.Annotations;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Analogue of .NET event but subscription could be done only with <see cref="Lifetime"/>
  /// so no risk to forget to unsubscribe. 
  /// </summary>
  /// <typeparam name="T">type of event</typeparam>
  public interface ISource<
#if !NET35
    out
#endif
    T>
  {
    
    /// <summary>
    /// Subscribes for this sources (if lifetime isn't terminated). <paramref name="handler"/> will be unsubscribed when lifetime become <see cref="LifetimeStatus.Terminated"/>   
    /// </summary>
    /// <param name="lifetime">if lifetime <see cref="Lifetime.IsNotAlive"/> then no subscription will be done</param>
    /// <param name="handler">handler of events</param>
    void Advise(Lifetime lifetime, Action<T> handler);
  }
}