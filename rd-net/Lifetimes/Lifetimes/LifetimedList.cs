using System;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Threading;
using JetBrains.Diagnostics;
using JetBrains.Threading;
using JetBrains.Util;
using JetBrains.Util.Internal;
using JetBrains.Util.Util;
using static JetBrains.Lifetimes.LifetimedListEx;

namespace JetBrains.Lifetimes
{
  
  /// <summary>
  /// Thread safe list for <see cref="ValueLifetimed{T}"/> entities.
  ///
  /// There are no <see cref="ICollection{T}.Remove"/> method, it's append only. When item's <see cref="ValueLifetimed{T}.Lifetime"/> becomes
  /// not alive, item's <see cref="ValueLifetimed{T}.Value"/> is cleared (to avoid memory leak) and item is fully removed from internal
  /// <see cref="myItems"/> array after exponential growth phase in <see cref="EnsureCapacityNoLock"/>.
  ///
  ///  
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public struct LifetimedList<T>
  {
    public struct Enumerator : IEnumerator<ValueLifetimed<T>>
    {
      private readonly ValueLifetimed<T>[] myItems;
      private readonly int mySize;
      
      private ValueLifetimed<T> myCurValue;
      private int myPos;

      public Enumerator(ValueLifetimed<T>[] items, int size) : this()
      {
        myItems = items;
        mySize = size;
        myPos = -1;
      }

      public void Dispose() {}

      public bool MoveNext()
      {
        while (++myPos < mySize)
        {
          ref var item = ref myItems[myPos];
          if (item.Lifetime.IsNotAlive) continue;
          
          myCurValue = item;

          // double-check `IsAlive` because myCurValue may be partially cleared if ClearValuesIfNotAlive was called at the same time
          Memory.Barrier(); // to suppress reordering
          if (item.Lifetime.IsAlive)
            return true;
        }

        return false;
      }       

      public void Reset() { throw new NotSupportedException(); }
      public ValueLifetimed<T> Current => myCurValue;
      object IEnumerator.Current => Current;
    }

    //in x64 we have one free 4-bytes slot in this structure so let we use it for something meaningful
    //1. Global lock (GlobalMutexSlice) for thread-safe insertion of a new element
    //2. Local lock (LocalMutexSlice) for fast thread-safe reading/writing of `mySize` and `myItems`
    //3. Marker (MarkerMutexSlice) to delimit items into two parts: with high priority (< Marker) and normal (>= Marker)
    private int myState;
    private ArrayAndSize myArrayAndSize;

    public void Add(Lifetime lifetime, T value) => Add(new ValueLifetimed<T>(lifetime, value));
    public void AddPriorityItem(Lifetime lifetime, T value) => AddPriorityItem(new ValueLifetimed<T>(lifetime, value));
    
    public void Add(ValueLifetimed<T> item)
    {
      bool shouldClear;
      EnterLock();
      try
      {
        shouldClear = EnsureCapacityNoLock(false, out var items, out var marker, out var size);
        items[size] = item;

        // increase mySize only after inserting an element
        if (myArrayAndSize?.Array == items)
          myArrayAndSize.Size = size + 1;
        else
          myArrayAndSize = new ArrayAndSize(items, size + 1);
        // no need to use InterlockedUpdate because we under lock
        myState = MarkerMutexSlice.Updated(myState, marker);
      }
      finally
      {
        ReleaseLock();
      }

      if (shouldClear)
      {
        // if ClearValuesIfNotAlive was called at the same time with EnsureCapacityNoLock, we may not have cleared some elements
        // 1. Thread 1 - create a new array (items)
        // 2. Thread 1 - copy item[0] to the new array
        // 3. Thread 2 - terminate item[0].Lifetime
        // 4. Thread 2 - ClearValuesIfNotAlive
        // 5. Thread 1 - myItems = items (myItems still contains uncleared item)
        // Memory leak
        ClearValuesIfNotAlive();
      }
    }        

    public void AddPriorityItem(ValueLifetimed<T> item)
    {
      EnterLock();
      try
      {
        // AddPriorityItem is not a frequent operation, we can afford to create a new array each time
        EnsureCapacityNoLock(true, out var items, out var marker, out var size);
        items[marker] = item;
        
        // increase mySize only after inserting an element
        if (myArrayAndSize.Array == items)
          myArrayAndSize.Size = size + 1;
        else
          myArrayAndSize = new ArrayAndSize(items, size + 1);
        // no need to use InterlockedUpdate because we under lock
        myState = MarkerMutexSlice.Updated(myState, marker + 1);
      }
      finally
      {
        ReleaseLock();
      }
      
      // AddPriorityItem always creates a new array
      // if ClearValuesIfNotAlive was called at the same time with EnsureCapacityNoLock, we may not have cleared some elements
      // 1. Thread 1 - create a new array (items)
      // 2. Thread 1 - copy item[0] to the new array
      // 3. Thread 2 - terminate item[0].Lifetime
      // 4. Thread 2 - ClearValuesIfNotAlive
      // 5. Thread 1 - myItems = items (myItems still contains uncleared item)
      // Memory leak
      ClearValuesIfNotAlive();
    }

    private bool EnsureCapacityNoLock(bool priority, out ValueLifetimed<T>[] items, out int marker, out int size)
    {
      var arrayAndSize = myArrayAndSize ??= new ArrayAndSize();

      marker = MarkerMutexSlice[myState];
      
      if (arrayAndSize.Size < arrayAndSize.Array.Length && !priority)
      {
        items = arrayAndSize.Array;
        size = arrayAndSize.Size;
        return false;
      }
     
      var newSize = 0;
      for (var i = 0; i < arrayAndSize.Size; i++)
      {
        if (arrayAndSize.Array[i].Lifetime.IsAlive) 
          newSize++;
      }

      if (newSize == 0) newSize = 1;
      else newSize *= 2;

      // we have to make new array ALWAYS at this point, because this method could be called during enumeration and we want enumeration to work in a snapshot fashion      
      var countAfterCleaning = 0;
      var markerDecrement = 0;
      var newItems = new ValueLifetimed<T>[newSize];
      
      for (var i = 0; i < marker; i++)
      {
        ref var item = ref arrayAndSize.Array[i];
        if (item.Lifetime.IsAlive)
          newItems[countAfterCleaning++] = item;
        else
          markerDecrement++;
      }

      var offset = priority ? 1 : 0; // reserve place for new priority item
      for (var i = marker; i < arrayAndSize.Size; i++)
      {
        ref var item = ref arrayAndSize.Array[i];
        if (item.Lifetime.IsAlive)
          newItems[countAfterCleaning++ + offset] = item;
        else if (i < marker)
          markerDecrement++;
      }

      marker -= markerDecrement;
      size = countAfterCleaning;
      items = newItems;
      return true;
    }

    public void ClearValuesIfNotAlive()
    {
      // !!! performance optimization !!!
      // no need to take lock because it doesn't affect the addition of new items.
      var arrayAndSize = Memory.VolatileRead(ref myArrayAndSize);
      if (arrayAndSize == null) return;

      var size = Memory.VolatileRead(ref arrayAndSize.Size);
      for (var j = 0; j < size; j++)
        arrayAndSize.Array[j].ClearValueIfNotAlive();
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private void EnterLock()
    {
      var spinner = new SpinWaitEx();
      do
      {
        var s = Memory.VolatileRead(ref myState);
        if (!MutexSlice[s])
        {
          if (Interlocked.CompareExchange(ref myState, MutexSlice.Updated(s, true), s) == s)
            return;
        }

        spinner.SpinOnce(false);
      } while (true);
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private void ReleaseLock()
    {
      Assertion.Assert(MutexSlice[myState], "Must be under mutex");
      MutexSlice.InterlockedUpdate(ref myState, false);
    }

    public Enumerator GetEnumerator()
    {
      var arrayAndSize = Memory.VolatileRead(ref myArrayAndSize);
      if (arrayAndSize == null) return default;
      
      return new Enumerator(arrayAndSize.Array, Memory.VolatileRead(ref arrayAndSize.Size));
    }
    
    private class ArrayAndSize
    {
      public readonly ValueLifetimed<T>[] Array;
      public int Size;

      public ArrayAndSize() : this(new ValueLifetimed<T>[1], 0){}
      public ArrayAndSize(ValueLifetimed<T>[] array, int size)
      {
        Array = array;
        Size = size;
      }
    }
  }

  public static class LifetimedListEx
  {
    internal static readonly BoolBitSlice MutexSlice = BitSlice.Bool();
    internal static readonly IntBitSlice MarkerMutexSlice = BitSlice.Int(16, MutexSlice);

    public static IEnumerable<TOut> Select<TIn, TOut>(this LifetimedList<TIn> source, Func<ValueLifetimed<TIn>, TOut> selector)
    {
      foreach (var value in source)
        yield return selector(value);
    }
    
    public static IEnumerable<ValueLifetimed<T>> Where<T>(this LifetimedList<T> source, Func<ValueLifetimed<T>, bool> predicate)
    {
      foreach (var value in source)
      {
        if (predicate(value))
          yield return value;
      }
    }
  }
}