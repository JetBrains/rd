using System;
using System.Collections;
using System.Collections.Generic;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Util.Internal;

namespace JetBrains.Collections.Viewable;

public class ViewableConcurrentSet<T> : IViewableConcurrentSet<T>, IEnumerable<T>
{
  private readonly Signal<VersionedData> mySignal = new();
  private Dictionary<T, LifetimeDefinition> myMap;
  private readonly object myLocker = new();
  private int myCount;
  private int myAddVersion;
  private int myIsUnderReadingCount;

  public int Count => Memory.VolatileRead(ref myCount);

  public ViewableConcurrentSet(IEqualityComparer<T>? comparer = null)
  {
    myMap = new Dictionary<T, LifetimeDefinition>(comparer);
  }

  public bool Add(T value)
  {
    LifetimeDefinition definition;
    int version;
    lock (myLocker)
    {
      var map = GetOrCloneMapNoLock();
      if (map.TryGetValue(value, out definition) && definition.Lifetime.IsAlive)
        return false;

      definition = new LifetimeDefinition();
      map[value] = definition;
      version = ++myAddVersion;
      myCount++;
    }

    mySignal.Fire(new VersionedData(definition.Lifetime, value, version));
    return true;
  }

  public bool Remove(T value)
  {
    LifetimeDefinition definitionToRemove;
    lock (myLocker)
    {
      var map = GetOrCloneMapNoLock();
      if (!map.TryGetValue(value, out definitionToRemove))
        return false;
    }

    definitionToRemove.Terminate();

    lock (myLocker)
    {
      var map = GetOrCloneMapNoLock();
      if (!map.TryGetValue(value, out var definition) || definition != definitionToRemove)
        return false;
        
      map.Remove(value);
      myCount--;
    }
      
    return true;
  }

  public bool Contains(T value)
  {
    return TryGetLifetime(value, out var lifetime) && lifetime.IsAlive;
  }

  public bool TryGetLifetime(T value, out Lifetime lifetime)
  {
    lock (myLocker)
    {
      if (myMap.TryGetValue(value, out var definition))
      {
        lifetime = definition.Lifetime;
        return true;
      }

      lifetime = Lifetime.Terminated;
      return false;
    } 
  }

  private Dictionary<T, LifetimeDefinition> GetOrCloneMapNoLock()
  {
    var map = myMap;
    if (myIsUnderReadingCount > 0)
    {
      map = new(map);
      myIsUnderReadingCount = 0;
      myMap = map;
      return map;
    }

    return map;
  }

  public void View(Lifetime lifetime, Action<Lifetime, T> action)
  {
    Dictionary<T, LifetimeDefinition> map;
    lock (myLocker)
    {
      map = myMap;
      var version = myAddVersion;

      mySignal.Advise(lifetime, versionedData =>
      {
        if (versionedData.Version <= version)
          return;

        var value = versionedData.Value;

        var newLifetime = versionedData.Lifetime.Intersect(lifetime);
        if (newLifetime.IsNotAlive)
          return;

        action(newLifetime, value);
      });
      
      if (map.Count == 0) 
        return;
      
      myIsUnderReadingCount++;
    }

    foreach (var (value, definition) in map)
    {
      var newLifetime = definition.Lifetime.Intersect(lifetime);
      if (newLifetime.IsNotAlive)
        return;
      
      try
      {
        action(newLifetime, value);
      }
      catch (Exception e)
      {
        Log.Root.Error(e);
      }
    }

    lock (myLocker)
    {
      if (myMap == map)
      {
        var count = myIsUnderReadingCount--;
        Assertion.Assert(count >= 0);
      }
    }
  }

  private readonly record struct VersionedData(Lifetime Lifetime, T Value, int Version)
  {
    public readonly Lifetime Lifetime = Lifetime;
    public readonly T Value = Value;
    public readonly int Version = Version;
  }
  
  private readonly struct ReadCookie : IDisposable
  {
    private readonly ViewableConcurrentSet<T> mySet;
    public Dictionary<T, LifetimeDefinition> Map { get; }

    public ReadCookie(ViewableConcurrentSet<T> set)
    {
      mySet = set;
      lock (set.myLocker)
      {
        Map = mySet.myMap;
        mySet.myIsUnderReadingCount++;
      }
    }

    public void Dispose()
    {
      lock (mySet.myLocker)
      {
        if (Map == mySet.myMap)
        {
          var count = mySet.myIsUnderReadingCount--;
          Assertion.Assert(count >= 0);
        }
      }
    }
  }

  public IEnumerator<T> GetEnumerator()
  {
    using var cookie = new ReadCookie(this);
    foreach (var (key, definition) in cookie.Map)
    {
      if (definition.Lifetime.IsAlive)
        yield return key;
    }
  }

  IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
}