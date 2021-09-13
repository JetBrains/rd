using System;
using System.Collections;
using System.Collections.Generic;

namespace JetBrains.Collections
{
  /// <summary>
  /// Enumerator for empty collection.
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public sealed class EmptyEnumerator<T> : IEnumerator<T> 
  {
    public static readonly EmptyEnumerator<T> Instance = new EmptyEnumerator<T>();

    public T Current => throw new InvalidOperationException($"{nameof(EmptyEnumerator<T>)}.{nameof(Current)} is undefined");

    object? IEnumerator.Current => Current;

    public bool MoveNext() => false;

    public void Reset() { }

    public void Dispose() { }

    public IEnumerator<T> GetEnumerator() => this;
  }
}