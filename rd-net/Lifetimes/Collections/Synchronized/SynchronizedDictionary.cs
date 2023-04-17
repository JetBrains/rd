using System.Collections;
using System.Collections.Generic;
using JetBrains.Annotations;

namespace JetBrains.Collections.Synchronized
{
  /// <summary>
  /// This dictionary is used for Net3.5 as a poor replacement of ConcurrentDictionary.
  /// All methods are synchronized. <see cref="GetEnumerator()"/> copies whole content so
  /// you can change collection as you want during enumeration. 
  /// </summary>
  /// <typeparam name="TK"></typeparam>
  /// <typeparam name="TV"></typeparam>
  [PublicAPI] public class SynchronizedDictionary<TK, TV> : IDictionary<TK, TV>
  {
    private readonly IDictionary<TK, TV> myImpl;

    public SynchronizedDictionary(IEqualityComparer<TK>? comparer = null)
    {
      myImpl = new Dictionary<TK, TV>(comparer);
    }
    
    public SynchronizedDictionary(int capacity, IEqualityComparer<TK>? comparer = null)
    {
      myImpl = new Dictionary<TK, TV>(capacity, comparer);
    }
    
    /// <summary>
    /// Copies content of collection: O(n) CPU and memory complexity.
    /// </summary>
    /// <returns></returns>
    public IEnumerator<KeyValuePair<TK, TV>> GetEnumerator()
    {
      var copy = new List<KeyValuePair<TK, TV>>();
      lock (myImpl)
      {
        copy.AddRange(myImpl);
      }
      return copy.GetEnumerator();
    }
    
    
    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    
    public void Add(KeyValuePair<TK, TV> item)
    {
      lock(myImpl)
        myImpl.Add(item);
    }

    
    public void Clear()
    {
      lock(myImpl)
        myImpl.Clear();
    }

    
    public bool Contains(KeyValuePair<TK, TV> item)
    {
      lock(myImpl)
        return myImpl.Contains(item);
    }

    
    public void CopyTo(KeyValuePair<TK, TV>[] array, int arrayIndex)
    {
      lock(myImpl)
        myImpl.CopyTo(array, arrayIndex);
    }

    
    public bool Remove(KeyValuePair<TK, TV> item)
    {
      lock(myImpl)
        return myImpl.Remove(item);
    }

    
    public int Count
    {
      get { lock(myImpl) return myImpl.Count; }
    }

    public bool IsReadOnly
    {
      get { lock(myImpl) return myImpl.IsReadOnly; }
    }

    
    public bool ContainsKey(TK key)
    {
      lock(myImpl)
        return myImpl.ContainsKey(key);
    }

    
    public void Add(TK key, TV value)
    {
      lock(myImpl)
        myImpl.Add(key, value);
    }

    
    public bool Remove(TK key)
    {
      lock(myImpl)
        return myImpl.Remove(key);
    }

    
    public bool TryGetValue(TK key, out TV value)
    {
      lock(myImpl) return myImpl.TryGetValue(key, out value);
    }

    public TV this[TK key]
    {
      
      get { lock(myImpl) return myImpl[key]; }
      set { lock(myImpl) myImpl[key] = value; }
    }

    public ICollection<TK> Keys
    {
      get { lock(myImpl) return myImpl.Keys; }
    }

    public ICollection<TV> Values
    {
      get { lock(myImpl) return myImpl.Values; }
    }
  }
}