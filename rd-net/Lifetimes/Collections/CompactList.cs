using System;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using JetBrains.Diagnostics;
using JetBrains.Util;

namespace JetBrains.Collections
{  
  /// <summary>
  /// Saves memory footprint and traffic for lists with single element: doesn't allocate real list
  /// until number of elements is more then 1.
  /// <see cref="GetEnumerator()"/> return struct that should save memory traffic during enumeration.
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public struct CompactList<T> : IEnumerable<T>
  {
    internal static readonly List<T?> SingleMarker = new List<T?>();

    private T? mySingleValue;
    // or or
    private List<T?>? myMultipleValues;
    
    public CompactListEnumerator<T> GetEnumerator()
    {
      return new CompactListEnumerator<T>(mySingleValue, myMultipleValues);
    }

    public int Count {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get
      {
        if (myMultipleValues == SingleMarker) return 1;
        return myMultipleValues?.Count ?? 0;
      } 
    }

    IEnumerator<T> IEnumerable<T>.GetEnumerator()
    {
      return GetEnumerator();
    }
    
    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    public void Add(T item)
    {
      switch (Count)      
      {
        case 0:
          mySingleValue = item;
          myMultipleValues = SingleMarker;
          break;
        case 1: 
          myMultipleValues = new List<T?> { mySingleValue, item };
          mySingleValue = default(T);
          break;
        default:
          myMultipleValues.NotNull().Add(item);
          break;
      }
    }

    public void Clear()
    {
      mySingleValue = default(T);
      myMultipleValues = null;
    }

    public int LastIndexOf(T item, IEqualityComparer<T?> comparer)
    {
      switch (Count)      
      {
        case 0: return -1;
        case 1: return comparer.Equals(mySingleValue, item) ? 0 : -1;          
        default:
          Assertion.AssertNotNull(myMultipleValues);
          for (var i = myMultipleValues.Count - 1; i >= 0; i--)
          {
            if (comparer.Equals(myMultipleValues[i], item)) return i;
          }
          return -1;
      }
    }

    public bool RemoveAt(int index)
    {
      if (Mode.IsAssertion)
        Assertion.Assert(index >= 0, "{0} >= 0", index);
      if (index >= Count) return false;
      
      switch (Count)
      {
        case 1:
          mySingleValue = default(T);
          myMultipleValues = null;
          return true;
          
        case 2:
          Assertion.AssertNotNull(myMultipleValues);
          mySingleValue = myMultipleValues[1-index];
          myMultipleValues = SingleMarker;
          return true;
            
        default:
          Assertion.AssertNotNull(myMultipleValues);
          myMultipleValues.RemoveAt(index);
          return true;
      }      
    }

    public T?[] ToArray()
    {
      switch (Count)      
      {
        case 0:
          return EmptyArray<T>.Instance;
        case 1:
          return new [] {mySingleValue};
        default:
          return myMultipleValues.NotNull().ToArray();
      }
    }
    
    public T? this[int index]
    {
      get
      {
        if (myMultipleValues == SingleMarker)
        {
          if (index == 0)
            return mySingleValue;
          else
            throw new IndexOutOfRangeException($"{index} out of range [0;0]");
        }
        else if (myMultipleValues != null)
          return myMultipleValues[index];
        else 
          throw new IndexOutOfRangeException($"List is empty");        
      }
    }        
  }
  
  public struct CompactListEnumerator<T> : IEnumerator<T?>
  {
    private readonly T? mySingleValue;
    private readonly List<T?>? myMultipleValues;
    private int myIndex;
    private T? myCurrent;
      
    internal CompactListEnumerator(T? singleValue, List<T?>? multipleValues)
    {
      mySingleValue = singleValue;
      myMultipleValues = multipleValues;
      myIndex = -1;
      myCurrent = default;
    }

    object? IEnumerator.Current => myCurrent;
      
    public T? Current => myCurrent;

    public bool MoveNext()
    {
      if (myMultipleValues == null)
        return false;

      myIndex++;
      if (myMultipleValues == CompactList<T>.SingleMarker)
      {
        if (myIndex == 0)
        {
          myCurrent = mySingleValue;
          return true;
        }
      }
      else
      {
        if (myIndex < myMultipleValues.Count)
        {
          myCurrent = myMultipleValues[myIndex];
          return true;
        }
      }

      myCurrent = default(T);
      return false;
    }

    public void Reset()
    {
      myIndex = -1;
      myCurrent = default(T);
    }
      
    public void Dispose()
    {
      // do nothing
    }
  }

  public static class SmartListExtensions
  {
    public static CompactList<T> ToSmartList<T>(this IEnumerable<T> source)
    {
      var result = new CompactList<T>();
      foreach (var item in source)
      {
        result.Add(item);
      }
      return result;
    }
  }
}