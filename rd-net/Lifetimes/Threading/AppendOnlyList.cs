using System;using System.Collections;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using System.Threading;
using JetBrains.Diagnostics;
using JetBrains.Util.Internal;

namespace JetBrains.Threading;

public class AppendOnlyList<T> : IList<T>
#if !NET35
  ,IReadOnlyList<T>
#endif
{
  private readonly int myMaxCount;
  private readonly object myLocker = new();

  private volatile Segment myHead;
  private readonly Segment myTail;

  private Segment? myLastReadSegment;

  public int Count => myHead.TotalCount;

  public bool IsFull => Count == myMaxCount;
  public bool IsFrozen { get; private set; }

  public bool IsEmpty => Count == 0;

  public AppendOnlyList(int defaultCapacity = 32, int maxCount = int.MaxValue)
  {
    Assertion.Assert(maxCount >= 0 && defaultCapacity >= 0);
    
    myMaxCount = maxCount;
    myHead = new Segment(0, Math.Min(defaultCapacity, maxCount));
    myTail = myHead;
  }

  public T this[int index]
  {
    get
    {
      var lastReadAccessSegment = myLastReadSegment;
      var segment = lastReadAccessSegment?.GlobalStartIndex <= index ? lastReadAccessSegment : myTail;
      
      T? value;
      while (!segment.TryGetValueByGlobal(index, out value))
      {
        segment = segment.Next;
        if (segment == null)
          throw new IndexOutOfRangeException();
      }

      myLastReadSegment = segment;
      return value;
    }
  }

  public bool TryAppend(T item)
  {
    while (true)
    {
      var segment = myHead;
      var prev = segment;
      while (segment != null)
      {
        if (segment.TryAdd(item))
          return true;

        prev = segment;
        segment = prev.Next;
      }
      
      if (IsFrozen)
        return false;

      lock (myLocker)
      {
        if (IsFrozen)
          return false;
        
        if (prev.Next != null)
          continue;
        
        var newCapacity = prev.Capacity * 2L;
        var currentGlobalCapacity = prev.TotalCapacity;
        var newTotalCapacity = currentGlobalCapacity + newCapacity;
        if (newTotalCapacity > myMaxCount)
        {
          newCapacity = myMaxCount - currentGlobalCapacity;
          if (newCapacity <= 0)
            return false;
        }

        var newSegment = new Segment(currentGlobalCapacity, (int)newCapacity);
        
        myHead = newSegment;
        prev.Next = newSegment;
      }
    }
  }

  public int Freeze()
  {
    lock (myLocker)
    {
      IsFrozen = true;
      return myHead.Freeze();
    }
  }

  public void CopyTo(T[] array, int arrayIndex)
  {
    foreach (var item in this)
    {
      var index = arrayIndex++;
      if (index >= array.Length)
        return;
      
      array[index] = item;
    }
  }

  public Enumerator GetEnumerator() => new(this);

  public bool Contains(T item) => IndexOf(item) != -1;
  public bool Contains(T item, IEqualityComparer<T> comparer) => IndexOf(item, comparer) != -1;

  public int IndexOf(T item) => IndexOf(item, EqualityComparer<T>.Default);
  public int IndexOf(T item, IEqualityComparer<T> comparer)
  {
    var index = 0;
    foreach (var value in this)
    {
      if (comparer.Equals(value, item))
        return index;

      index++;
    }

    return -1;
  }
  
  public struct Enumerator : IEnumerator<T>
  {
    private Segment? myCurrentSegment;
    private int myCurrentIndex;

    public T Current => myCurrentSegment.NotNull().TryGetValueByLocal(myCurrentIndex, out var value)
      ? value
      : throw new InvalidOperationException("");
    
    private int myMaxCount;

    internal Enumerator(AppendOnlyList<T> list)
    {
      myCurrentSegment = list.myTail;
      myCurrentIndex = -1;
      myMaxCount = list.myHead.TotalCount;
    }

    public bool MoveNext()
    {
      var segment = myCurrentSegment;
      if (segment == null)
        return false;

      var index = ++myCurrentIndex;
      if (index >= myMaxCount)
      {
        myCurrentSegment = null;
        return false;
      }

      if (segment.Count <= index)
      {
        myCurrentSegment = segment = segment.Next;
        myMaxCount -= index;
        myCurrentIndex = 0;
      }
      
      Assertion.Assert(segment is { Count: > 0 });
      return true;
    }

    object? IEnumerator.Current => Current;
    public void Reset() => throw new NotImplementedException();
    public void Dispose() { }
  }

  private class Segment
  {
    public readonly int GlobalStartIndex;
    
    private readonly Slot[] mySlots;
    private State myState = new() { Index = -1 };

    public Segment? Next;

    public int Capacity => mySlots.Length;
    public int Count => Memory.VolatileRead(ref myState.Index) + 1;

    public int TotalCount => GlobalStartIndex + Count;
    public int TotalCapacity => GlobalStartIndex + Capacity;

    public Segment(int globalStartIndex, int capacity)
    {
      GlobalStartIndex = globalStartIndex;
      mySlots = new Slot[capacity];
    }

    public bool TryAdd(T item)
    {
      var state = myState;
      var spinner = new SpinWaitEx();
      
      while (true)
      {
        if (state.IsFrozen || state.Index == int.MaxValue)
          return false;

        var newState = new State { Index = state.Index + 1 };
        if (newState.Index >= mySlots.Length)
          return false;

        var oldValue = Interlocked.CompareExchange(ref myState.Value, newState.Value, state.Value);
        if (oldValue == state.Value)
        {
          // ReSharper disable once UseObjectOrCollectionInitializer
          mySlots[newState.Index].Value = item;
          mySlots[newState.Index].Set = true; // volatile write
          return true;
        }

        state = new State { Value = oldValue };
        spinner.SpinOnce(false);
      }
    }

    public bool TryGetValueByLocal(int localIndex, out T value)
    {
      if (localIndex < 0 || localIndex > Memory.VolatileRead(ref myState.Index))
      {
        value = default!;
        return false;
      }
      
      ref var slot = ref mySlots[localIndex];
      if (!slot.Set)
      {
        var spinner = new SpinWaitEx();
        while (!slot.Set) 
          spinner.SpinOnce(false);
      }

      value = slot.Value;
      return true;
    }
    
    public bool TryGetValueByGlobal(int globalIndex, out T value)
    {
      return TryGetValueByLocal(globalIndex - GlobalStartIndex, out value);
    }

    public int Freeze()
    {
      myState.IsFrozen = true;
      Memory.Barrier();
      return TotalCount;
    }

    private struct Slot
    {
      public T Value;
      public volatile bool Set;
    }
  }
  
  #region explisit

  T IList<T>.this[int index]
  {
    get => this[index];
    set => throw new NotSupportedException();
  }

  void ICollection<T>.Add(T item)
  {
    if (!TryAppend(item))
      throw new InvalidOperationException("The list is full");
  }

  bool ICollection<T>.IsReadOnly => false;
  void ICollection<T>.Clear() => throw new NotSupportedException();
  void IList<T>.RemoveAt(int index) => throw new NotSupportedException();
  bool ICollection<T>.Remove(T item) => throw new NotSupportedException();
  void IList<T>.Insert(int index, T item) => throw new NotSupportedException();
  IEnumerator<T> IEnumerable<T>.GetEnumerator() => GetEnumerator();
  IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
  

  #endregion
}

[StructLayout(LayoutKind.Explicit)]
internal struct State
{
  [FieldOffset(0)]
  public long Value;

  [FieldOffset(0)] public bool IsFrozen;
  [FieldOffset(1)] private bool _1;
  [FieldOffset(2)] private bool _2;
  [FieldOffset(3)] private bool _3;
  [FieldOffset(4)] public int Index;
}


