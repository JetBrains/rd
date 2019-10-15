using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;

namespace JetBrains.Rd.Impl
{
  public class SwitchingViewableSet<T> : IViewableSet<T>
  {
    private IViewableSet<T> myBackingSet;
    private LifetimedList<Action<SetEvent<T>>> myListeners = new LifetimedList<Action<SetEvent<T>>>();
    private readonly SequentialLifetimes myAdviseLifetimes;

    public SwitchingViewableSet(Lifetime lifetime, IViewableSet<T> backingSet)
    {
      myBackingSet = backingSet;
      myAdviseLifetimes = new SequentialLifetimes(lifetime);
      
      AdviseForBackingSet();
    }

    public void ChangeBackingSet(IViewableSet<T> newBackingSet, bool isNewSetMaster = true)
    {
      if (isNewSetMaster)
      {
        var missingValues = myBackingSet.Except(newBackingSet);
        var newValues = newBackingSet.Except(myBackingSet);
        
        foreach (var missingValue in missingValues) 
          FireListeners(SetEvent<T>.Remove(missingValue));
        foreach (var newValue in newValues) 
          FireListeners(SetEvent<T>.Add(newValue));
      }

      var oldBackingSet = myBackingSet;
      myBackingSet = newBackingSet;
      if (!isNewSetMaster)
      {
        var valuesToRemove = newBackingSet.Where(it => !oldBackingSet.Contains(it)).ToList();
        foreach (var newValue in valuesToRemove)
          newBackingSet.Remove(newValue);

        foreach (var oldValue in oldBackingSet) 
          newBackingSet.Add(oldValue);
      }
      AdviseForBackingSet();
    }

    private void AdviseForBackingSet()
    {
      var lt = myAdviseLifetimes.Next();
      var initial = true;
      myBackingSet.Advise(lt, setEvent =>
      {
        // ReSharper disable once AccessToModifiedClosure
        if (initial) return;
        FireListeners(setEvent);
      });
      initial = false;
    }

    private void FireListeners(SetEvent<T> setEvent)
    {
      foreach (var (lifetime, value) in myListeners)
      {
        if (!lifetime.IsAlive) continue;
        value?.Invoke(setEvent);
      }
    }

    public void Advise(Lifetime lifetime, Action<SetEvent<T>> handler)
    {
      foreach (var existingValue in myBackingSet) 
        handler(SetEvent<T>.Add(existingValue));

      myListeners.Add(lifetime, handler);
    }

    public ISource<SetEvent<T>> Change => this;

    #region IViewableSet delegation
    
    IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
    public IEnumerator<T> GetEnumerator() => myBackingSet.GetEnumerator();
    public void Add(T item) => myBackingSet.Add(item);
    public void Clear() => myBackingSet.Clear();
    public bool Contains(T item) => myBackingSet.Contains(item);
    public void CopyTo(T[] array, int arrayIndex) => myBackingSet.CopyTo(array, arrayIndex);
    public bool Remove(T item) => myBackingSet.Remove(item);
    public int Count => myBackingSet.Count;
    public bool IsReadOnly => myBackingSet.IsReadOnly;
    #if !NET35
    bool ISet<T>.Add(T item) => myBackingSet.Add(item);
    public void ExceptWith(IEnumerable<T> other) => myBackingSet.ExceptWith(other);
    public void IntersectWith(IEnumerable<T> other) => myBackingSet.IntersectWith(other);
    public bool IsProperSubsetOf(IEnumerable<T> other) => myBackingSet.IsProperSubsetOf(other);
    public bool IsProperSupersetOf(IEnumerable<T> other) => myBackingSet.IsProperSupersetOf(other);
    public bool IsSubsetOf(IEnumerable<T> other) => myBackingSet.IsSubsetOf(other);
    public bool IsSupersetOf(IEnumerable<T> other) => myBackingSet.IsSupersetOf(other);
    public bool Overlaps(IEnumerable<T> other) => myBackingSet.Overlaps(other);
    public bool SetEquals(IEnumerable<T> other) => myBackingSet.SetEquals(other);
    public void SymmetricExceptWith(IEnumerable<T> other) => myBackingSet.SymmetricExceptWith(other);
    public void UnionWith(IEnumerable<T> other) => myBackingSet.UnionWith(other);
    #endif
    #endregion
  }
}