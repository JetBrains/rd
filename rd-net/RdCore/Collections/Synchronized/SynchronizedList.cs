using System.Collections;
using System.Collections.Generic;
using JetBrains.Annotations;

namespace JetBrains.Collections.Synchronized
{
  /// <summary>
  /// This is a thread-safe list with all methods synchronized.
  /// <see cref="GetEnumerator()"/> copies whole content so
  /// you can change collection as you want during enumeration. 
  /// </summary>
  /// <typeparam name="T"></typeparam>
  [PublicAPI] public class SynchronizedList<T> : IList<T>
  {
    private readonly List<T> myList;

    public SynchronizedList(IEnumerable<T> values = null, int capacity = 0)
    {
      myList = new List<T>(capacity > 0 ? capacity : 10);
      if (values != null) 
        myList.AddRange(values);
    }

    public IEnumerator<T> GetEnumerator()
    {
      IList<T> clone;
      lock (myList)
        clone = myList.ToArray();

      return clone.GetEnumerator();
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    public void Add(T item)
    {
      lock (myList)
        myList.Add(item);
    }

    public void Clear()
    {
      lock (myList)
        myList.Clear();
    }

    public bool Contains(T item)
    {
      lock (myList)
        return myList.Contains(item);
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      lock (myList)
        myList.CopyTo(array, arrayIndex);
    }

    public bool Remove(T item)
    {
      lock (myList)
        return myList.Remove(item);
    }

    public int Count
    {
      get
      {
        lock (myList)
          return myList.Count;
      }
    }

    public bool IsReadOnly => false;

    public int IndexOf(T item)
    {
      lock (myList)
        return myList.IndexOf(item);
    }

    public void Insert(int index, T item)
    {
      lock (myList)
        myList.Insert(index, item);
    }

    public void RemoveAt(int index)
    {
      lock (myList)
        myList.RemoveAt(index);
    }

    public T this[int index]
    {
      get
      {
        lock (myList)
          return myList[index];
      }
      set
      {
        lock (myList)
          myList[index] = value;
      }
    }
  }
}