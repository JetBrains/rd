using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace JetBrains.Rd.Util
{
  internal class ConcurrentSet<T> : 
    ISet<T>
  {
    private readonly ConcurrentDictionary<T, bool> myDictionary = new ConcurrentDictionary<T, bool>();

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    public IEnumerator<T> GetEnumerator()
    {
      return myDictionary.Keys.GetEnumerator();
    }

    void ICollection<T>.Add(T item)
    {
      myDictionary[item] = false;
    }

    public void Clear()
    {
      myDictionary.Clear();
    }

    public bool Contains(T item)
    {
      return myDictionary.ContainsKey(item);
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      myDictionary.Keys.CopyTo(array, arrayIndex);
    }

    public bool Remove(T item)
    {
      return myDictionary.TryRemove(item, out _);
    }

    public int Count => myDictionary.Count;
    public bool IsReadOnly => false;
    
    bool ISet<T>.Add(T item)
    {
      return myDictionary.TryAdd(item, false);
    }

    public void ExceptWith(IEnumerable<T> other)
    {
      throw new System.NotImplementedException();
    }

    public void IntersectWith(IEnumerable<T> other)
    {
      throw new System.NotImplementedException();
    }

    public bool IsProperSubsetOf(IEnumerable<T> other)
    {
      throw new System.NotImplementedException();
    }

    public bool IsProperSupersetOf(IEnumerable<T> other)
    {
      throw new System.NotImplementedException();
    }

    public bool IsSubsetOf(IEnumerable<T> other)
    {
      throw new System.NotImplementedException();
    }

    public bool IsSupersetOf(IEnumerable<T> other)
    {
      throw new System.NotImplementedException();
    }

    public bool Overlaps(IEnumerable<T> other)
    {
      throw new System.NotImplementedException();
    }

    public bool SetEquals(IEnumerable<T> other)
    {
      throw new System.NotImplementedException();
    }

    public void SymmetricExceptWith(IEnumerable<T> other)
    {
      throw new System.NotImplementedException();
    }

    public void UnionWith(IEnumerable<T> other)
    {
      throw new System.NotImplementedException();
    }
  }
}