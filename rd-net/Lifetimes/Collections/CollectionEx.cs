using System.Collections.Generic;
using JetBrains.Annotations;

namespace JetBrains.Collections
{
  /// <summary>
  /// Extension methods for collections
  /// </summary>
  public static class CollectionEx
  {
    /// <summary>
    /// Calculates polynomial hash code for collection.
    /// Return 0 if collection == null.
    /// Return `seed` (in current implementation 0x2D2816FE) if collection is empty.
    /// In current implementation polynomial factor is 31.
    /// </summary>
    /// <param name="collection"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns>((seed * factor + collection[0]?.GetHashCode() ?? 0) * factor + collection[1]?.GetHashCode() ?? 0) * factor + ... </returns>
    [Pure, CollectionAccess(CollectionAccessType.Read)]
    public static int ContentHashCode<T>([CanBeNull] this ICollection<T> collection)
    {
      if (collection == null) return 0;

      var hashCode = 0x2D2816FE;
      foreach (var item in collection)
      {
        hashCode = hashCode * 31 + (item == null ? 0 : item.GetHashCode());
      }

      return hashCode;
    }


    /// <summary>
    /// Dequeue <paramref name="queue"/> if it's not empty (or do nothing).
    /// </summary>
    /// <param name="queue"></param>
    /// <param name="res"><see cref="Queue{T}.Dequeue"/> if <paramref name="queue"/>.Count > 0 at method start, `default{T}` otherwise</param>
    /// <typeparam name="T"></typeparam>
    /// <returns>`true` if <paramref name="queue"/>.Count > 0 at method start, `false` otherwise</returns>
    public static bool TryDequeue<T>(this Queue<T> queue, out T res)
    {
      if (queue.Count > 0)
      {
        res = queue.Dequeue();
        return true;
      }
      else
      {
        res = default(T);
        return false;
      }
    }
    
    /// <summary>
    /// Same as <see cref="Queue{T}.Enqueue"/> but returns added element
    /// </summary>
    /// <param name="queue"></param>
    /// <param name="toEnqueue">Element to enqueue into <paramref name="queue"/></param>
    /// <typeparam name="T"></typeparam>
    /// <returns>Added element <paramref name="toEnqueue"/></returns>
    public static T Enqueued<T>(this Queue<T> queue, T toEnqueue)
    {
      queue.Enqueue(toEnqueue);
      return toEnqueue;
    }
  }
}