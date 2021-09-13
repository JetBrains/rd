using System;
using System.Collections.Generic;
using JetBrains.Annotations;

namespace JetBrains.Core
{
  /// <summary>
  /// Implementation of 'maybe' monad. Either <seealso cref="HasValue"/> is `true` and <seealso cref="Value"/> doesn't throw exception or `false`. 
  /// 
  /// default(Maybe) == Maybe.None
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public struct Maybe<T> : IEquatable<Maybe<T>>
  {
    [PublicAPI] public static readonly Maybe<T> None;
    public bool HasValue { get; }

    private readonly T myValue;
    
    /// <summary>
    /// 
    /// </summary>
    /// <exception cref="InvalidOperationException">if <seealso cref="HasValue"/> == `false`</exception>
    public T Value
    {
      get
      {
        if (!HasValue) throw new InvalidOperationException($"Can't invoke '{nameof(Maybe<T>)}.{nameof(Value)}' when {nameof(HasValue)} is `false`. Consider using {nameof(ValueOrDefault)}");
        return myValue;
      }
    }

    public T? ValueOrDefault => !HasValue ? default : myValue;

    public Maybe(T value) : this()
    {
      myValue = value;
      HasValue = true;
    }

    public T OrElseThrow(Func<Exception> func)
    {
      if (HasValue)
        return Value;
      
      throw func();
    }

    public Maybe<TK> Select<TK>(Func<T, TK> map) => HasValue ? new Maybe<TK>(map(Value)) : Maybe<TK>.None; 

    public override bool Equals(object obj)
    {
      if (!(obj is Maybe<T>)) return false;
      return Equals((Maybe<T>)obj);
    }

    public bool Equals(Maybe<T> other)
    {
      if (!other.HasValue) return !HasValue;
      return EqualityComparer<T>.Default.Equals(myValue, other.myValue);
    }

    public override int GetHashCode()
    {
      unchecked
      {
        if (!HasValue) return -1;
        return EqualityComparer<T>.Default.GetHashCode(myValue);
      }
    }

    public static bool operator ==(Maybe<T> left, Maybe<T> right)
    {
      return left.Equals(right);
    }

    public static bool operator !=(Maybe<T> left, Maybe<T> right)
    {
      return !left.Equals(right);
    }
  }
}