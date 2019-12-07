using System;
using System.Collections;
using System.Collections.Generic;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  public class ModificationCookieViewableSet<T, TCookie> : IViewableSet<T> where TCookie: struct, IDisposable
  {
    private readonly Func<TCookie> myCookieFactory;
    private readonly IViewableSet<T> myBackingSet;

    public ModificationCookieViewableSet(Func<TCookie> cookieFactory, IViewableSet<T> backingSet)
    {
      myCookieFactory = cookieFactory;
      myBackingSet = backingSet;
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    public ISource<SetEvent<T>> Change => myBackingSet.Change;

    public void Advise(Lifetime lifetime, Action<SetEvent<T>> handler)
    {
      myBackingSet.Advise(lifetime, handler);
    }

    public IEnumerator<T> GetEnumerator()
    {
      return myBackingSet.GetEnumerator();
    }

    public bool Contains(T item)
    {
      return myBackingSet.Contains(item);
    }

    public int Count => myBackingSet.Count;

    public bool IsReadOnly => myBackingSet.IsReadOnly;

    public void CopyTo(T[] array, int arrayIndex)
    {
      myBackingSet.CopyTo(array, arrayIndex);
    }

    #if !NET35
    public bool IsProperSubsetOf(IEnumerable<T> other)
    {
      return myBackingSet.IsProperSubsetOf(other);
    }

    public bool IsProperSupersetOf(IEnumerable<T> other)
    {
      return myBackingSet.IsProperSupersetOf(other);
    }

    public bool IsSubsetOf(IEnumerable<T> other)
    {
      return myBackingSet.IsSubsetOf(other);
    }

    public bool IsSupersetOf(IEnumerable<T> other)
    {
      return myBackingSet.IsSupersetOf(other);
    }

    public bool Overlaps(IEnumerable<T> other)
    {
      return myBackingSet.Overlaps(other);
    }

    public bool SetEquals(IEnumerable<T> other)
    {
      return myBackingSet.SetEquals(other);
    }
    
    public bool Add(T item)
    {
      using (myCookieFactory())
        return myBackingSet.Add(item);
    }
    #endif

    public bool Remove(T item)
    {
      using (myCookieFactory())
        return myBackingSet.Remove(item);
    }

    public void Clear()
    {
      using (myCookieFactory())
        myBackingSet.Clear();
    }

    #if !NET35
    public void ExceptWith(IEnumerable<T> other)
    {
      using (myCookieFactory())
        myBackingSet.ExceptWith(other);
    }

    public void IntersectWith(IEnumerable<T> other)
    {
      using (myCookieFactory())
        myBackingSet.IntersectWith(other);
    }

    public void UnionWith(IEnumerable<T> other)
    {
      using (myCookieFactory())
        myBackingSet.UnionWith(other);
    }

    public void SymmetricExceptWith(IEnumerable<T> other)
    {
      using (myCookieFactory())
        myBackingSet.SymmetricExceptWith(other);
    }
    #endif

    void ICollection<T>.Add(T item)
    {
      using (myCookieFactory())
        myBackingSet.Add(item);
    }
  }
}