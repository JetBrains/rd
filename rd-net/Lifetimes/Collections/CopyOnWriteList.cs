using System;
using System.Collections;
using System.Collections.Generic;
using System.Threading;

namespace JetBrains.Collections
{
  public class CopyOnWriteList<T> : IList<T>
  {
    private T[] myStorage;

    public CopyOnWriteList()
    {
      myStorage = new T[0];
    }

    public IEnumerator<T> GetEnumerator()
    {
      return ((IEnumerable<T>) myStorage).GetEnumerator();
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    private void Modify<A>(A param, Func<T[], A, T[]> action)
    {
      while (true)
      {
        var currentStorage = myStorage;
        var newArray = action(currentStorage, param);
        if (Interlocked.CompareExchange(ref myStorage, newArray, currentStorage) == currentStorage)
          break;
      }
    }

    public void Add(T item)
    {
      Modify(item, (arg1, arg2) =>
      {
        var newArray = new T[arg1.Length + 1];
        Array.Copy(arg1, newArray, arg1.Length);
        newArray[arg1.Length] = arg2;
        return newArray;
      });
    }

    public void Clear()
    {
      Modify(0, ((arg1, i) => new T[0]));
    }

    public bool Contains(T item)
    {
      return Array.IndexOf(myStorage, item) != -1;
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      Array.Copy(myStorage, 0, array, arrayIndex, myStorage.Length);
    }

    public bool Remove(T item)
    {
      var result = false;
      Modify(item, (arg1, arg2) =>
      {
        var indexOfEntry = Array.IndexOf(arg1, arg2);
        if (indexOfEntry == -1)
        {
          result = false;
          return arg1;
        }

        var newArray = new T[arg1.Length - 1];
        Array.Copy(arg1, newArray, indexOfEntry);
        Array.Copy(arg1, indexOfEntry + 1, newArray, indexOfEntry, arg1.Length - 1 - indexOfEntry);
        result = true;
        return newArray;
      });
      return result;
    }

    public int Count => myStorage.Length;
    public bool IsReadOnly => false;
    public int IndexOf(T item)
    {
      return Array.IndexOf(myStorage, item);
    }

    private struct ItemIndexPair
    {
      public readonly int Index;
      public readonly T Item;

      public ItemIndexPair(int index, T item)
      {
        Index = index;
        Item = item;
      }
    }

    public void Insert(int index, T item)
    {
      Modify(new ItemIndexPair(index, item), (arg1, arg2) =>
      {
        var index2 = arg2.Index;
        var item2 = arg2.Item;
        var newArray = new T[arg1.Length + 1];
        Array.Copy(arg1, newArray, index2);
        Array.Copy(arg1, index2, newArray, index2 + 1, arg1.Length - index2);
        newArray[index2] = item2;
        return newArray;
      });
    }

    public void RemoveAt(int index)
    {
      Modify(index, (arg1, arg2) =>
      {
        var indexOfEntry = arg2;
        var newArray = new T[arg1.Length - 1];
        Array.Copy(arg1, newArray, indexOfEntry);
        Array.Copy(arg1, indexOfEntry + 1, newArray, indexOfEntry, arg1.Length - 1 - indexOfEntry);
        return newArray;
      });
    }

    public T this[int index]
    {
      get => myStorage[index];
      set => myStorage[index] = value;
    }
  }
}