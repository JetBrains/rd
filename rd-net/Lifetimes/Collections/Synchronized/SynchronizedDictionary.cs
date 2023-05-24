using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Util.Internal;

namespace JetBrains.Collections.Synchronized
{
  /// <summary>
  /// This dictionary is used for Net3.5 as a poor replacement of ConcurrentDictionary.
  /// All methods are synchronized.
  /// You can change collection as you want during enumeration. 
  /// </summary>
  /// <typeparam name="TK"></typeparam>
  /// <typeparam name="TV"></typeparam>
  [PublicAPI] public class SynchronizedDictionary<TK, TV> : IDictionary<TK, TV>, ICollection<TK>
  {
    private Dictionary<TK, TV> myImpl;
    private SynchronizedValues? myValues;
    private readonly object myLocker = new();
    private int myIsUnderReadingCount;

    public SynchronizedDictionary(IEqualityComparer<TK>? comparer = null)
    {
      myImpl = new Dictionary<TK, TV>(comparer);
    }
    
    public SynchronizedDictionary(int capacity, IEqualityComparer<TK>? comparer = null)
    {
      myImpl = new Dictionary<TK, TV>(capacity, comparer);
    }
    
    /// <summary>
    /// Returns a snapshot of the collection. Does not introduce overhead, such as copying the collection if there are no concurrent modifications.
    /// </summary>
    /// <returns></returns>
    public IEnumerator<KeyValuePair<TK, TV>> GetEnumerator()
    {
      Dictionary<TK, TV> map;
      lock (myLocker)
      {
        map = myImpl;
        myIsUnderReadingCount++;
      }

      try
      {
        foreach (var pair in map)
          yield return pair;
      }
      finally
      {
        lock (myLocker)
        {
          if (myImpl == map) 
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


    void ICollection<KeyValuePair<TK, TV>>.Add(KeyValuePair<TK, TV> item)
    {
      lock (myLocker)
      {
        var map = (IDictionary<TK, TV>)GetOrCloneMapNoLock();
        map.Add(item);
      }
    }

    
    public void Clear()
    {
      lock(myLocker)
        GetOrCloneMapNoLock().Clear();
    }


    bool ICollection<KeyValuePair<TK, TV>>.Contains(KeyValuePair<TK, TV> item)
    {
      lock(myLocker)
      {
        var map = (IDictionary<TK, TV>)myImpl;
        return map.Contains(item);
      }
    }


    void ICollection<KeyValuePair<TK, TV>>.CopyTo(KeyValuePair<TK, TV>[] array, int arrayIndex)
    {
      lock (myLocker)
      {
        var map = (IDictionary<TK, TV>)myImpl;
        map.CopyTo(array, arrayIndex);
      }
    }


    bool ICollection<KeyValuePair<TK, TV>>.Remove(KeyValuePair<TK, TV> item)
    {
      lock (myLocker)
      {
        var map = (IDictionary<TK, TV>)GetOrCloneMapNoLock();
        return map.Remove(item);
      }
    }

    public int Count
    {
      get { lock(myLocker) return myImpl.Count; }
    }

    bool ICollection<KeyValuePair<TK, TV>>.IsReadOnly => false;


    public bool ContainsKey(TK key)
    {
      lock(myLocker)
        return myImpl.ContainsKey(key);
    }

    
    public void Add(TK key, TV value)
    {
      lock(myLocker)
        GetOrCloneMapNoLock().Add(key, value);
    }

    
    public bool Remove(TK key)
    {
      lock(myLocker)
        return GetOrCloneMapNoLock().Remove(key);
    }

    
    public bool TryGetValue(TK key, out TV value)
    {
      lock(myLocker) return myImpl.TryGetValue(key, out value);
    }

    public TV this[TK key]
    {
      
      get { lock(myLocker) return myImpl[key]; }
      set { lock(myLocker) GetOrCloneMapNoLock()[key] = value; }
    }

    public ICollection<TK> Keys => this;
    public ICollection<TV> Values
    {
      get
      {
        var values = myValues;
        if (values != null)
          return values;

        lock (myLocker)
        {
          values = myValues;
          if (values != null)
            return values;
          
          values = new SynchronizedValues(this);
          Memory.VolatileWrite(ref myValues, values);
        }

        return values;
      }
    }
    

    private Dictionary<TK, TV> GetOrCloneMapNoLock()
    {
      var map = myImpl;
      if (myIsUnderReadingCount > 0)
      {
        map = new Dictionary<TK, TV>(map);
        myIsUnderReadingCount = 0;
        myImpl = map;
        return map;
      }

      return map;
    }

    void ICollection<TK>.Add(TK item) => throw new NotSupportedException();
    
    bool ICollection<TK>.Contains(TK item) => ContainsKey(item);
    void ICollection<TK>.CopyTo(TK[] array, int arrayIndex)
    {
      lock (myLocker) 
        myImpl.Keys.CopyTo(array, arrayIndex);
    }

    bool ICollection<TK>.IsReadOnly => true;

    IEnumerator<TK> IEnumerable<TK>.GetEnumerator()
    {
      foreach (var pair in this)
        yield return pair.Key;
    }
    
    // Compiler Error CS0695 'generic type' cannot implement both 'generic interface' and 'generic interface' because they may unify for some type parameter substitutions
    // so we need to write SynchronizedValues 
    private class SynchronizedValues : ICollection<TV>
    {
      private readonly SynchronizedDictionary<TK, TV> myMap;

      public SynchronizedValues(SynchronizedDictionary<TK, TV> map)
      {
        myMap = map;
      }
    
      void ICollection<TV>.Add(TV item) => throw new NotSupportedException();
      public bool Remove(TV item) => throw new NotSupportedException();
      public void Clear() => throw new NotSupportedException();

      bool ICollection<TV>.Contains(TV item)
      {
        lock (myMap.myLocker) 
          return myMap.myImpl.Values.Contains(item);
      }

      void ICollection<TV>.CopyTo(TV[] array, int arrayIndex)
      {
        lock (myMap.myLocker) 
          myMap.myImpl.Values.CopyTo(array, arrayIndex);
      }

      public int Count => myMap.Count;

      bool ICollection<TV>.IsReadOnly => true;

      public IEnumerator<TV> GetEnumerator()
      {
        foreach (var pair in myMap)
          yield return pair.Value;
      }

      IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
    }
  }
}