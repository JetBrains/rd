using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics;

namespace JetBrains.Util.Util
{

  
  public class StaticsForType<T> where T:class
  {     
    private readonly List<T> myList = new List<T>();
    private event Action Changed;

    private void FireChanged()
    {
      Changed?.Invoke();
    }

    public void ForEachValue(Action action)
    {
      lock (myList)
      {
        Changed += action;        
      }
      action();
    }
    
    internal StaticsForType() {}
    
    
    
    public void AddLast([NotNull]T value)
    {
      lock (myList)
      {
        myList.Add(value);
      }
      FireChanged();
    }

    
    public void AddFirst([NotNull]T value)
    {
      lock (myList)
      {
        myList.Insert(0, value);
      }
      FireChanged();
    }

    [CanBeNull]
    public T PeekFirst()
    {
      lock (myList)
      {
        return myList.Count > 0 ? myList[0] : null;
      }
    }
    
    [CanBeNull]
    public T PeekLast()
    {
      lock (myList)
      {
        return myList.Count > 0 ? myList[myList.Count - 1] : null;
      }
    }

    public void ReplaceFirst([NotNull]T value)
    {
      lock (myList)
      {
        myList.RemoveAt(0);
        myList.Insert(0, value);
      }
      FireChanged();
    }

    public bool RemoveLastReferenceEqual([NotNull]T value, bool failIfNotLast = false)
    {
      var result = false;
      
      lock (myList)
      {
        int idx = myList.Count - 1;
        if (failIfNotLast && (idx < 0 || myList[idx] != value))
          Assertion.Fail("Precondition failed for Statics<{0}>. LastElement is not {1}, myStack.Count={2}", typeof(T).FullName, value, idx + 1);
        
        while (idx >= 0)
        {
          if (myList[idx] == value)
          {
            myList.RemoveAt(idx);
            result =  true;
            break;
          }
          idx--;
        }
      }
      
      if (result) FireChanged();
      return result;
    }
  }

  
  /// <summary>
  /// Represents global statics in a stack-like way 
  /// </summary>
  public static class Statics
  {
    private static readonly Dictionary<Type, object> ourPerTypeStatics = new Dictionary<Type, object>();
    
    /// <summary>
    /// Gets statics holder for type <see cref="T"/>
    /// </summary>
    /// <typeparam name="T"></typeparam>
    /// <returns>Stack-like holder</returns>
    public static StaticsForType<T> For<T>() where T:class
    {
      lock (ourPerTypeStatics)
      {
        StaticsForType<T> result;
        object o;
        if (!ourPerTypeStatics.TryGetValue(typeof(T), out o))
        {
          result = new StaticsForType<T>();
          ourPerTypeStatics[typeof(T)] = result;
        }
        else
        {
          result = (StaticsForType<T>) o;
        }
        return result;
      }
    }
  }
}