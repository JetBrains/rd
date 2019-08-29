using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  public class ViewableMap<TK, TV> : IViewableMap<TK, TV>
  {
    [NotNull] private readonly IDictionary<TK, TV> myStorage;
    [NotNull] private readonly EqualityComparer<TV> myValueComparer;
    [NotNull] private readonly Signal<MapEvent<TK, TV>> myChange = new Signal<MapEvent<TK, TV>>();

    [NotNull] public ISource<MapEvent<TK, TV>> Change => myChange;
    
    [PublicAPI] public ViewableMap() : this(new Dictionary<TK, TV>()) {}

    [PublicAPI] public ViewableMap(EqualityComparer<TV> valueComparer) : this(new Dictionary<TK, TV>()) {}

    [PublicAPI] public ViewableMap([NotNull] IDictionary<TK, TV> storage, [CanBeNull] EqualityComparer<TV> equalityComparer = null)
    {
      myStorage = storage ?? throw new ArgumentNullException(nameof(storage));
      myValueComparer = equalityComparer ?? EqualityComparer<TV>.Default;
    }
    
    
    

    public IEnumerator<KeyValuePair<TK, TV>> GetEnumerator()
    {
      return myStorage.GetEnumerator();
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    public void Add(KeyValuePair<TK, TV> item)
    {
      Add(item.Key, item.Value);
    }

    public void Clear()
    {
      var changes = new List<MapEvent<TK, TV>>(Count);
      changes.AddRange(myStorage.Select(kv => MapEvent<TK, TV>.Remove(kv.Key, kv.Value)));

      myStorage.Clear();

      foreach (var change in changes)
      {
        myChange.Fire(change);
      }
    }

    public bool Contains(KeyValuePair<TK, TV> item)
    {
      return myStorage.Contains(item);
    }

    public void CopyTo(KeyValuePair<TK, TV>[] array, int arrayIndex)
    {
      myStorage.CopyTo(array, arrayIndex);
    }

    public bool Remove(KeyValuePair<TK, TV> item)
    {
      if (!Contains(item)) return false;
      return Remove(item.Key);
    }

    public int Count => myStorage.Count;

    public bool IsReadOnly => myStorage.IsReadOnly;

    public bool ContainsKey(TK key)
    {
      return myStorage.ContainsKey(key);
    }

    public void Add(TK key, TV value)
    {
      myStorage.Add(key, value);
      myChange.Fire(MapEvent<TK, TV>.Add(key, value));
    }

    public bool Remove(TK key)
    {
      if (!myStorage.TryGetValue(key, out var value)) return false;
      myStorage.Remove(key);

      myChange.Fire(MapEvent<TK, TV>.Remove(key, value));

      return true;
    }

    public bool TryGetValue(TK key, out TV value)
    {
      return myStorage.TryGetValue(key, out value);
    }

    public TV this[TK key]
    {
      get => myStorage[key];
      set
      {
        var isUpdate = myStorage.TryGetValue(key, out var oldval);
        if (isUpdate && myValueComparer.Equals(oldval, value)) return;

        myStorage[key] = value;

        // ReSharper disable once ConvertIfStatementToConditionalTernaryExpression
        if (isUpdate) 
          myChange.Fire(MapEvent<TK, TV>.Update(key, oldval, value));
        else 
          myChange.Fire(MapEvent<TK, TV>.Add(key, value));
      }
    }

    public ICollection<TK> Keys => myStorage.Keys;

    public ICollection<TV> Values => myStorage.Values;

    public void Advise(Lifetime lifetime, Action<MapEvent<TK, TV>> handler)
    {
      foreach (var (key, value) in this)
      {
        try
        {
          handler(MapEvent<TK, TV>.Add(key, value));
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