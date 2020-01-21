using JetBrains.Annotations;
using JetBrains.Core;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Special kind of <see cref="ISource{T}"/> that remembers last <see cref="Value"/>.
  /// There are bunch of differences with plain <see cref="ISource{T}"/>:
  /// <list type="number">
  /// <item>It's guaranteed by design that two sequential values received by <c>handler</c> from
  /// <see cref="ISource{T}.Advise"/> are not equal.</item>
  /// <item> if <see cref="Maybe"/>.<see cref="Maybe{T}.HasValue"/> then <see cref="Value"/> is 
  /// equals to <see cref="Maybe"/>.<see cref="Maybe{T}.Value"/>. Invocation of <see cref="ISource{T}.Advise"/> will execute <c>handler</c>
  /// synchronously with <see cref="Value"/> and then invoke <see cref="Change"/>.<see cref="ISource{T}.Advise"/>.
  /// </item>
  ///
  /// <item>
  /// If <see cref="Maybe"/> is <see cref="Maybe.None"/> (no one set this property before) then
  /// <see cref="Value"/> will throw <see cref="System.InvalidOperationException"/>. 
  /// Invocation of <see cref="ISource{T}.Advise"/>  will just invoke invoke <see cref="Change"/>.<see cref="ISource{T}.Advise"/>
  /// without synchronous invocation.  
  ///</item> 
  /// </list> 
  /// </summary>
  /// <typeparam name="T"></typeparam>
  /// <remarks>
  /// If value is set once then it's guaranteed that <see cref="Maybe"/>.<see cref="Maybe{T}.HasValue"/> <c> == true</c>
  /// and <see cref="Value"/> won't throw exception. It's impossible to return state to <see cref="Maybe.None"/> after
  /// it was set.
  /// <remarks>See <see cref="ReactiveEx.View{T}(JetBrains.Collections.Viewable.IReadonlyProperty{T},JetBrains.Lifetimes.Lifetime,System.Action{JetBrains.Lifetimes.Lifetime,T})"/>
  /// for structured subscription.
  /// </remarks> 
  /// 
  /// </remarks>
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