using System;
using System.Collections.Generic;

namespace JetBrains.Core
{
  /// <summary>
  /// Implementation of 'maybe' monad.
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public struct Maybe<T> : IEquatable<Maybe<T>>
  {
    public static readonly Maybe<T> None = new Maybe<T>();
    public bool HasValue { get; private set; }

    private readonly T myValue;
    public T Value
    {
      get
      {
        if (!HasValue) throw new ArgumentException("!HasValue");
        return myValue;
      }
    }

    public T ValueOrDefault => !HasValue ? default(T) : myValue;

    public Maybe(T value) : this()
    {
      myValue = value;
      HasValue = true;
    }

    public T OrElseThrow(Func<Exception> func)
    {
      if (HasValue)
        return Value;
      else
        throw func();
    }

    public Maybe<K> Select<K>(Func<T, K> map) => HasValue ? new Maybe<K>(map(Value)) : Maybe<K>.None; 

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