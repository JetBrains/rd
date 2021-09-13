using System;
using System.Collections;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using JetBrains.Annotations;
using JetBrains.Collections.Synchronized;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Default implementation if <see cref="IViewableList{T}"/>. 
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public class ViewableList<T> : IViewableList<T> where T : notnull
  {
    private readonly IList<T> myStorage;
    private readonly Signal<ListEvent<T>> myChange = new Signal<ListEvent<T>>();

    [PublicAPI]
    public ISource<ListEvent<T>> Change => myChange;


    public ViewableList() : this(new List<T>()) {}

    /// <summary>
    /// Special delegating constructor that accepts storage backend (e.g. <see cref="SynchronizedList{T}"/>) 
    /// </summary>
    /// <param name="list"></param>
    public ViewableList(IList<T> list)
    {
      myStorage = list;
    }
    

    public IEnumerator<T> GetEnumerator()
    {
      return myStorage.GetEnumerator();
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    void ICollection<T>.Add(T item)
    {
      Add(item!);
    }

    public void Add([DisallowNull] T item)
    {
      if (item == null) throw new ArgumentNullException(nameof(item));
      myStorage.Add(item);
      myChange.Fire(ListEvent<T>.Add(myStorage.Count-1, item));
    }

    public void Clear()
    {
      for (int index = myStorage.Count - 1; index >= 0; index--) 
        RemoveAt(index);
    }

    public bool Contains(T item)
    {
      return myStorage.Contains(item);
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      myStorage.CopyTo(array, arrayIndex);
    }

    public bool Remove(T item)
    {
      var index = myStorage.IndexOf(item);
      if (index < 0) return false;

      RemoveAt(index);
      return true;
    }

    public int Count => myStorage.Count;

    public bool IsReadOnly => myStorage.IsReadOnly;

    public void RemoveAt(int index)
    {
      var old = myStorage[index];
      myStorage.RemoveAt(index);
      myChange.Fire(ListEvent<T>.Remove(index, old));
    }
    

    public T this[int index]
    {
      get => myStorage[index];
      set
      {
        Assertion.Require(value != null, "value != null");
        
        var oldval = myStorage[index];
        if (Equals(oldval, value)) return;

        myStorage[index] = value;
        myChange.Fire(ListEvent<T>.Update(index, oldval, value));        
      }
    }   

    public void Advise(Lifetime lifetime, Action<ListEvent<T>> handler)
    {
      for (int index=0; index < myStorage.Count; index++)
      {
        try
        {
          handler(ListEvent<T>.Add(index, myStorage[index]));
        }
        catch (Exception e)
        {
          Log.Root.Error(e);
        }
      }
      myChange.Advise(lifetime, handler);
    }

    public int IndexOf(T item)
    {
      return myStorage.IndexOf(item);
    }

    public void Insert(int index, T item)
    {
      if (item == null) 
        throw new ArgumentNullException(nameof(item));
      
      myStorage.Insert(index, item);
      myChange.Fire(ListEvent<T>.Add(index, item));
    }
  }
}