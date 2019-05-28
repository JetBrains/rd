using System;
using System.Collections;
using System.Collections.Generic;
using JetBrains.Annotations;

namespace JetBrains.Collections.Synchronized
{
  #if !NET35
  /// <summary>
  /// This is a thread-safe set with all methods synchronized.
  /// <see cref="GetEnumerator()"/> copies whole content so
  /// you can change collection as you want during enumeration.
  ///
  /// Useful pattern (in place filtering):
  /// <code>
  ///   using (en = syncSet.GetEnumerator()) {
  ///     while (en.MoveNext()) {
  ///       if (some_condition(en.Current))
  ///         syncSet.Remove(en.Current);
  ///     }
  ///   }
  /// </code>
  /// </summary>
  /// <typeparam name="T"></typeparam>
  [PublicAPI] public class SynchronizedSet<T> : ISet<T>, IReadOnlyCollection<T>
  {
    private readonly HashSet<T> mySet;

    public SynchronizedSet() : this(null, null) {}
    public SynchronizedSet(IEnumerable<T> values) : this(values, null) {}
    public SynchronizedSet(IEqualityComparer<T> comparer) : this(null, comparer) {}
    public SynchronizedSet(IEnumerable<T> values, IEqualityComparer<T> comparer)
    {
      mySet = new HashSet<T>(comparer);
      if (values == null) return;
      
      foreach (var item in values)
        mySet.Add(item);
    }

    private T[] CopyToArray()
    {
      var clone = new T[mySet.Count];
      var i = 0;
      lock (mySet)
      {
        foreach (var val in mySet)
          clone[i++] = val;
      }
      return clone;
    }

    public IEnumerator<T> GetEnumerator()
    {
      T[] clone;
      lock (mySet)
      {
        clone = CopyToArray();
      }

      return ((IEnumerable<T>) clone).GetEnumerator();
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    public void Add(T item)
    {
      lock (mySet)
      {
        mySet.Add(item);
      }
    }

    public void Clear()
    {
      lock (mySet)
      {
        mySet.Clear();
      }
    }

    public bool Contains(T item)
    {
      lock (mySet)
      {
        return mySet.Contains(item);
      }
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      lock (mySet)
      {
        // Linq calls on ConcurrentSet are not thread-safe
        // E.g., Enumerable.ToList calls List`1.ctor which contains the following race:
        //   int count = collection.Count;
        //   _items = new T[count];
        //   collection.CopyTo(_items, 0);
        // In order to prevent IndexOutOfRangeException for this code,
        // we shouldn't copy more elements that we have in the array.
        mySet.CopyTo(array, arrayIndex, Math.Min(mySet.Count, array.Length - arrayIndex));
      }
    }

    public bool Remove(T item)
    {
      lock (mySet)
      {
        return mySet.Remove(item);
      }
    }

    public int Count
    {
      get
      {
        lock (mySet)
        {
          return mySet.Count;
        }
      }
    }

    public bool IsReadOnly => false;

    bool ISet<T>.Add(T item)
    {
      lock (mySet)
      {
        return mySet.Add(item);
      }
    }

    public void UnionWith(IEnumerable<T> other)
    {
      lock (mySet)
      {
        mySet.UnionWith(other);
      }
    }

    public void IntersectWith(IEnumerable<T> other)
    {
      lock (mySet)
      {
        mySet.IntersectWith(other);
      }
    }

    public void ExceptWith(IEnumerable<T> other)
    {
      lock (mySet)
      {
        mySet.ExceptWith(other);
      }
    }

    public void SymmetricExceptWith(IEnumerable<T> other)
    {
      lock (mySet)
      {
        mySet.SymmetricExceptWith(other);
      }
    }

    public bool IsSubsetOf(IEnumerable<T> other)
    {
      lock (mySet)
      {
        return mySet.IsSubsetOf(other);
      }
    }

    public bool IsSupersetOf(IEnumerable<T> other)
    {
      lock (mySet)
      {
        return mySet.IsSupersetOf(other);
      }
    }

    public bool IsProperSupersetOf(IEnumerable<T> other)
    {
      lock (mySet)
      {
        return mySet.IsProperSupersetOf(other);
      }
    }

    public bool IsProperSubsetOf(IEnumerable<T> other)
    {
      lock (mySet)
      {
        return mySet.IsProperSubsetOf(other);
      }
    }

    public bool Overlaps(IEnumerable<T> other)
    {
      lock (mySet)
      {
        return mySet.Overlaps(other);
      }
    }

    public bool SetEquals(IEnumerable<T> other)
    {
      lock (mySet)
      {
        return mySet.SetEquals(other);
      }
    }

    public IReadOnlyList<T> ExtractAll()
    {
      lock (mySet)
      {
        var elements = CopyToArray();
        mySet.Clear();
        return elements;
      }
    }
  }
#endif
}