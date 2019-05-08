using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  public class ViewableSet<T> : IViewableSet<T>
  {
    private readonly Signal<SetEvent<T>> myChange = new Signal<SetEvent<T>>();

    public ISource<SetEvent<T>> Change
    {
      get { return myChange; }
    }

    private readonly HashSet<T> mySet = new HashSet<T>();

    public IEnumerator<T> GetEnumerator()
    {
      return mySet.GetEnumerator();
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }


    public void Add(T item)
    {
      if (!mySet.Add(item)) return;
      myChange.Fire(SetEvent<T>.Add(item));
    }

    public void Clear()
    {
      var changes = mySet.ToArray();

      mySet.Clear();

      foreach (var change in changes) myChange.Fire(SetEvent<T>.Remove(change));
    }

    public bool Contains(T item)
    {
      return mySet.Contains(item);
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      mySet.CopyTo(array, arrayIndex);
    }

    public bool Remove(T item)
    {
      if (!mySet.Remove(item)) return false;

      myChange.Fire(SetEvent<T>.Remove(item));
      return true;
    }

    public int Count
    {
      get { return mySet.Count; }
    }

    public bool IsReadOnly
    {
      get { return false; }
    }

    public void Advise(Lifetime lifetime, Action<SetEvent<T>> handler)
    {
      foreach (var elt in mySet) handler(SetEvent<T>.Add(elt));
      myChange.Advise(lifetime, handler);
    }
  }
}