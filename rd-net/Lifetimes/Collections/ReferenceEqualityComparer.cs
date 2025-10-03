using System.Collections.Generic;
using System.Runtime.CompilerServices;
using JetBrains.Annotations;

namespace JetBrains.Collections
{
  /// <summary>
  /// Comparer that uses reference equality.
  /// Usage: `ReferenceEqualityComparer{T}.Default`
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public sealed class ReferenceEqualityComparer<T> : IEqualityComparer<T>
    where T : class
  {
    private static readonly ReferenceEqualityComparer<T> ourDefault = new ReferenceEqualityComparer<T>();
    private ReferenceEqualityComparer() { }

    public bool Equals(T? x, T? y) => ReferenceEquals(x, y);

    public int GetHashCode(T obj) => RuntimeHelpers.GetHashCode(obj);

    public static IEqualityComparer<T> Default => ourDefault;
  }
}