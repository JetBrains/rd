using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  public class ViewableMap<K, V> : IViewableMap<K, V>
  {
    private readonly IDictionary<K, V> myMap = new Dictionary<K, V>();
    private readonly Signal<MapEvent<K, V>> myChange = new Signal<MapEvent<K, V>>();

    public ISource<MapEvent<K, V>> Change
    {
      get { return myChange; }
    }

    public ViewableMap(bool isReadonly = false)
    {
      IsReadOnly = isReadonly;
    }

    public IEnumerator<KeyValuePair<K, V>> GetEnumerator()
    {
      return myMap.GetEnumerator();
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    public void Add(KeyValuePair<K, V> item)
    {
      Add(item.Key, item.Value);
    }

    public void Clear()
    {
      var changes = new List<MapEvent<K, V>>(Count);
      changes.AddRange(myMap.Select(kv => MapEvent<K, V>.Remove(kv.Key, kv.Value)));

      myMap.Clear();

      foreach (var change in changes)
      {
        myChange.Fire(change);
      }
    }

    public bool Contains(KeyValuePair<K, V> item)
    {
      return myMap.Contains(item);
    }

    public void CopyTo(KeyValuePair<K, V>[] array, int arrayIndex)
    {
      myMap.CopyTo(array, arrayIndex);
    }

    public bool Remove(KeyValuePair<K, V> item)
    {
      if (!Contains(item)) return false;
      return Remove(item.Key);
    }

    public int Count
    {
      get { return myMap.Count; }
    }

    public bool IsReadOnly { get; private set; }

    public bool ContainsKey(K key)
    {
      return myMap.ContainsKey(key);
    }

    public void Add(K key, V value)
    {
      myMap.Add(key, value);
      myChange.Fire(MapEvent<K, V>.Add(key, value));
    }

    public bool Remove(K key)
    {
      V value;
      if (!myMap.TryGetValue(key, out value)) return false;
      myMap.Remove(key);

      myChange.Fire(MapEvent<K, V>.Remove(key, value));

      return true;
    }

    public bool TryGetValue(K key, out V value)
    {
      return myMap.TryGetValue(key, out value);
    }

    public V this[K key]
    {
      get { return myMap[key]; }
      set
      {
        V oldval;
        bool isUpdate = myMap.TryGetValue(key, out oldval);
        if (isUpdate && Equals(oldval, value)) return;

        myMap[key] = value;

        // ReSharper disable once ConvertIfStatementToConditionalTernaryExpression
        if (isUpdate) myChange.Fire(MapEvent<K, V>.Update(key, oldval, value));
        else myChange.Fire(MapEvent<K, V>.Add(key, value));
      }
    }

    public ICollection<K> Keys
    {
      get { return myMap.Keys; }
    }

    public ICollection<V> Values
    {
      get { return myMap.Values; }
    }

    public void Advise(Lifetime lifetime, Action<MapEvent<K, V>> handler)
    {
      foreach (var kv in this)
      {
        try
        {
          handler(MapEvent<K, V>.Add(kv.Key, kv.Value));
        }
        catch (Exception e)
        {
          Log.Root.Error(e);
        }
      }
      myChange.Advise(lifetime, handler);
    }
  }
}