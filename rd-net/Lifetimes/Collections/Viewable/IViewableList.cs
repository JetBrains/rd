using System.Collections.Generic;

// ReSharper disable InconsistentNaming

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Observable list.
  /// Invocation <see cref="ISource{T}.Advise"/> for this list will synchronously invoke
  /// <c>handler</c> with <see cref="ListEvent{V}.Kind"/> == <see cref="AddRemove.Add"/> for
  /// each existing element of this collection.
  /// <remarks>See <see cref="ReactiveEx.View{T}(JetBrains.Collections.Viewable.IViewableList{T},JetBrains.Lifetimes.Lifetime,System.Action{JetBrains.Lifetimes.Lifetime,int,T})"/>
  /// for structured subscription.
  /// </remarks> 
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public interface IViewableList<T> : IList<T>, ISource<ListEvent<T>> where T : notnull
  {
    /// <summary>
    /// Advise this source if you don't want synchronous execution of <c>handler</c> on existing
    /// items of this <see cref="ViewableList{T}"/>. 
    /// </summary>
    ISource<ListEvent<T>> Change { get; } 
  }

}