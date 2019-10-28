using System.Collections;
using System.Collections.Concurrent;
using System.Collections.Generic;

namespace JetBrains.Collections
{
  public class SetOnDictionaryKeys<T, TValue> : 
    #if NET35
    ICollection<T>
    #else
    ISet<T>
    #endif
  {
    private readonly IDictionary<T, TValue> myDictionary;
    private readonly TValue myStubValue;

    public SetOnDictionaryKeys(IDictionary<T, TValue> dictionary, TValue stubValue)
    {
      myDictionary = dictionary;
      myStubValue = stubValue;
    }

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
      myDictionary[item] = myStubValue;
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
      return myDictionary.Remove(item);
    }

    public int Count => myDictionary.Count;
    public bool IsReadOnly => myDictionary.IsReadOnly;
    
    #if !NET35
    bool ISet<T>.Add(T item)
    {
      if (myDictionary is ConcurrentDictionary<T, TValue> concurrentDictionary)
        return concurrentDictionary.TryAdd(item, myStubValue);
      
      if (myDictionary.ContainsKey(item)) return false;
      myDictionary[item] = myStubValue;
      return true;
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
    #endif
  }
}