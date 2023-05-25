using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Diagnostics;

namespace JetBrains.Collections.Synchronized
{
  /// <summary>
  /// This is a thread-safe set with all methods synchronized.
  /// You can change collection as you want during enumeration.
  ///
  /// Useful pattern for filtering):
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
  [PublicAPI] public class SynchronizedSet<T> :
#if NET35
    ICollection<T>
#else
    ISet<T>, IReadOnlyCollection<T>
#endif
  {
    private HashSet<T> mySet;
    private readonly object myLocker = new();
    private int myIsUnderReadingCount;

    public SynchronizedSet() : this(null, null) {}
    public SynchronizedSet(IEnumerable<T> values) : this(values, null) {}
    public SynchronizedSet(IEqualityComparer<T> comparer) : this(null, comparer) {}
    public SynchronizedSet(IEnumerable<T>? values, IEqualityComparer<T>? comparer)
    {
      mySet = values == null ? new HashSet<T>(comparer) : new HashSet<T>(values, comparer);
    }

    /// <summary>
    /// Returns a snapshot of the collection. Does not introduce overhead, such as copying the collection if there are no concurrent modifications.
    /// </summary>
    /// <returns></returns>
    public IEnumerator<T> GetEnumerator()
    {
      HashSet<T> set;
      lock (myLocker)
      {
        set = mySet;
        myIsUnderReadingCount++;
      }

      try
      {
        foreach (var pair in set)
          yield return pair;
      }
      finally
      {
        lock (myLocker)
        {
          if (mySet == set) 
          {
            var count = myIsUnderReadingCount--;
            Assertion.Assert(count >= 0);
          }
        }
      }
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    public bool Add(T item)
    {
      lock(myLocker)
      {
        return GetOrCloneSetNoLock().Add(item);
      }
    }

    void ICollection<T>.Add(T item)
    {
      Add(item);
    }

    public void Clear()
    {
      lock(myLocker)
      {
        GetOrCloneSetNoLock().Clear();
      }
    }

    public bool Contains(T item)
    {
      lock(myLocker)
      {
        return mySet.Contains(item);
      }
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      lock(myLocker)
      {
        // Linq calls on SynchronizedSet are not thread-safe
        // E.g., Enumerable.ToList calls List`1.ctor which contains the following race:
        //   int count = collection.Count;
        //   _items = new T[count];
        //   collection.CopyTo(_items, 0);
        // In order to prevent IndexOutOfRangeException for this code,
        // we shouldn't copy more elements that we have in the array.
        var set = mySet;
        set.CopyTo(array, arrayIndex, Math.Min(set.Count, array.Length - arrayIndex));
      }
    }

    public bool Remove(T item)
    {
      lock(myLocker)
      {
        return GetOrCloneSetNoLock().Remove(item);
      }
    }

    public int Count
    {
      get
      {
        lock(myLocker)
        {
          return mySet.Count;
        }
      }
    }

    public bool IsReadOnly => false;

#if !NET35
    bool ISet<T>.Add(T item)
    {
      lock(myLocker)
      {
        return GetOrCloneSetNoLock().Add(item);
      }
    }
#endif

    public void UnionWith(IEnumerable<T> other)
    {
      lock(myLocker)
      {
        GetOrCloneSetNoLock().UnionWith(other);
      }
    }

    public void IntersectWith(IEnumerable<T> other)
    {
      lock(myLocker)
      {
        GetOrCloneSetNoLock().IntersectWith(other);
      }
    }

    public void ExceptWith(IEnumerable<T> other)
    {
      lock(myLocker)
      {
        GetOrCloneSetNoLock().ExceptWith(other);
      }
    }

    public void SymmetricExceptWith(IEnumerable<T> other)
    {
      lock(myLocker)
      {
        GetOrCloneSetNoLock().SymmetricExceptWith(other);
      }
    }

    public bool IsSubsetOf(IEnumerable<T> other)
    {
      lock(myLocker)
      {
        return mySet.IsSubsetOf(other);
      }
    }

    public bool IsSupersetOf(IEnumerable<T> other)
    {
      lock(myLocker)
      {
        return mySet.IsSupersetOf(other);
      }
    }

    public bool IsProperSupersetOf(IEnumerable<T> other)
    {
      lock(myLocker)
      {
        return mySet.IsProperSupersetOf(other);
      }
    }

    public bool IsProperSubsetOf(IEnumerable<T> other)
    {
      lock(myLocker)
      {
        return mySet.IsProperSubsetOf(other);
      }
    }

    public bool Overlaps(IEnumerable<T> other)
    {
      lock(myLocker)
      {
        return mySet.Overlaps(other);
      }
    }

    public bool SetEquals(IEnumerable<T> other)
    {
      lock(myLocker)
      {
        return mySet.SetEquals(other);
      }
    }

    public T[] ExtractAll()
    {
      lock(myLocker)
      {
        var set = GetOrCloneSetNoLock();
        var elements = CopyToArray(set);
        set.Clear();
        return elements;
      }
    
      static T[] CopyToArray(HashSet<T> set)
      {
        var clone = new T[set.Count];
        var i = 0;
        foreach (var val in set)
          clone[i++] = val;
        return clone;
      }
    }
    
    public T? ExtractOneOrDefault()
    {
      lock(myLocker)
      {
        var set = GetOrCloneSetNoLock();
        if (set.Count == 0) return default;

        var item = set.First();
        set.Remove(item);
        return item;
      }
    }
    
    private HashSet<T> GetOrCloneSetNoLock()
    {
      var map = mySet;
      if (myIsUnderReadingCount > 0)
      {
        map = new HashSet<T>(map);
        myIsUnderReadingCount = 0;
        mySet = map;
        return map;
      }

      return map;
    }
  }
}