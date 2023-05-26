using System;
using System.Collections;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Diagnostics;

namespace JetBrains.Collections.Synchronized
{
  /// <summary>
  /// This is a thread-safe list with all methods synchronized.
  /// You can change collection as you want during enumeration. 
  /// </summary>
  /// <typeparam name="T"></typeparam>
  [PublicAPI] public class SynchronizedList<T> : IList<T>
  {
    private List<T> myList;
    private readonly object myLocker = new();
    private int myIsUnderReadingCount;

    public SynchronizedList(IEnumerable<T>? values = null, int capacity = 0)
    {
      myList = new List<T>(capacity > 0 ? capacity : 10);
      if (values != null) 
        myList.AddRange(values);
    }

    /// <summary>
    /// Returns a snapshot of the collection. Does not introduce overhead, such as copying the collection if there are no concurrent modifications.
    /// </summary>
    /// <returns></returns>
    public IEnumerator<T> GetEnumerator()
    {
      List<T> list;
      lock (myLocker)
      {
        list = myList;
        myIsUnderReadingCount++;
      }

      try
      {
        foreach (var item in list)
          yield return item;
      }
      finally
      {
        lock (myLocker)
        {
          if (myList == list) 
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

    public void Add(T item)
    {
      lock(myLocker)
        GetOrCloneListNoLock().Add(item);
    }

    public void Clear()
    {
      lock(myLocker)
        GetOrCloneListNoLock().Clear();
    }

    public bool Contains(T item)
    {
      lock(myLocker)
        return myList.Contains(item);
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      lock (myLocker)
      {
        // Linq calls on SynchronizedList are not thread-safe
        // E.g., Enumerable.ToList calls List`1.ctor which contains the following race:
        //   int count = collection.Count;
        //   _items = new T[count];
        //   collection.CopyTo(_items, 0);
        // In order to prevent IndexOutOfRangeException for this code,
        // we shouldn't copy more elements that we have in the array.
        var list = myList;
        list.CopyTo(0, array, arrayIndex, Math.Min(list.Count, array.Length - arrayIndex));
      }
    }

    public bool Remove(T item)
    {
      lock(myLocker)
        return GetOrCloneListNoLock().Remove(item);
    }

    public int Count
    {
      get
      {
        lock(myLocker)
          return myList.Count;
      }
    }

    public bool IsReadOnly => false;

    public int IndexOf(T item)
    {
      lock(myLocker)
        return myList.IndexOf(item);
    }

    public void Insert(int index, T item)
    {
      lock(myLocker)
        GetOrCloneListNoLock().Insert(index, item);
    }

    public void RemoveAt(int index)
    {
      lock(myLocker)
        GetOrCloneListNoLock().RemoveAt(index);
    }

    public T this[int index]
    {
      get
      {
        lock(myLocker)
          return myList[index];
      }
      set
      {
        lock(myLocker)
          GetOrCloneListNoLock()[index] = value;
      }
    }
    
    private List<T> GetOrCloneListNoLock()
    {
      var map = myList;
      if (myIsUnderReadingCount > 0)
      {
        map = new List<T>(map);
        myIsUnderReadingCount = 0;
        myList = map;
        return map;
      }

      return map;
    }
  }
}