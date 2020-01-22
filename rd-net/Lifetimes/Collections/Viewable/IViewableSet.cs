using System.Collections.Generic;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Observable set.
  /// Invocation <see cref="ISource{T}.Advise"/> for this list will synchronously invoke
  /// <c>handler</c> with <see cref="SetEvent{T}.Kind"/> == <see cref="AddRemove.Add"/> for
  /// each existing element of this collection.
  /// <remarks>See <see cref="ReactiveEx.View{T}(IViewableSet{T},JetBrains.Lifetimes.Lifetime,System.Action{JetBrains.Lifetimes.Lifetime,T})"/>
  /// </summary>
  /// <typeparam name="T"></typeparam>
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