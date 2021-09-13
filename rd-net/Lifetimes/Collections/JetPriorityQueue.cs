using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Util;

namespace JetBrains.Collections
{
  /// <summary>
  /// JetBrains interface of priority queue data structure.
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public interface IPriorityQueue<T> : ICollection<T>
    #if !NET35
    , IReadOnlyCollection<T>
    #endif
  {
    new int Count { get; }

    bool TryExtract(out T res);
    bool TryPeek(out T res);
  }

  /// <summary>
  /// JetBrains implementation of priority queue data structure.
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public class JetPriorityQueue<T> : IPriorityQueue<T>
  {
    public const int DefaultCapacity = 10;
    private readonly List<T> myStorage;
    private readonly List<long> myVersions;
    private readonly IComparer<T> myComparer;
    private long myVersionAcc;
    

    public JetPriorityQueue(int initialCapacity = DefaultCapacity, IComparer<T> comparer = null)
    {
      if (initialCapacity <= 0) initialCapacity = DefaultCapacity;
      myStorage = new List<T>(initialCapacity + 1) { default(T) }; //first elem is always false to simplify `left` and `right`
      myVersions = new List<long>(initialCapacity + 1) {0};
      myComparer = comparer ?? Comparer<T>.Default;
    }

    #region ICollection implementation
    public IEnumerator<T> GetEnumerator()
    {
      var enumerator = myStorage.GetEnumerator();
      enumerator.MoveNext();
      return enumerator;
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    public void Add(T item)
    {
      var idx = myStorage.Count;
      myStorage.Add(item);
      myVersions.Add(++myVersionAcc);
      HeapUp(idx);
    }

    public void Clear()
    {
      myStorage.Clear();
      myVersions.Clear();
      myStorage.Add(default(T));
      myVersions.Add(0);
    }

    public bool Contains(T item)
    {
      return myStorage.IndexOf(item) > 0;
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      myStorage.CopyTo(1, array, arrayIndex, Count);
    }

    public bool Remove(T item)
    {
      throw new InvalidOperationException();
    }

    public int Count => myStorage.Count - 1;
    public bool IsReadOnly => false;

    #endregion


    #region Priority related methods


    public bool TryExtract(out T res)
    {
      if (!TryPeek(out res)) return false;

      var last = myStorage.Count - 1;
      myStorage[1] = myStorage[last];
      myVersions[1] = myVersions[last];
      
      //todo default list implementation calls Array.Copy with zero size that is not optimal behaviour (e.g. use LocalList here)
      myStorage.RemoveAt(last); 
      myVersions.RemoveAt(last);
      
      if (last > 1) HeapDown(1);
      return true;
    }

    public bool TryPeek(out T res)
    {
      if (myStorage.Count <= 1)
      {
        res = default (T);
        return false;
      }

      res = myStorage[1];      
      return true;
    }

    #endregion

    #region private Helpers
    private void Swap(ref int i, int j)
    {
      var s = myStorage[i];
      myStorage[i] = myStorage[j];
      myStorage[j] = s;

      var v = myVersions[i];
      myVersions[i] = myVersions[j];
      myVersions[j] = v;

      i = j;
    }


    private int Compare(int left, int right)
    {
      if (left == right) return 0;
      
      var cmp1 = myComparer.Compare(myStorage[left], myStorage[right]);
      if (cmp1 != 0) return cmp1;

      var cmp2 = myVersions[left] - myVersions[right];
      Assertion.Assert(cmp2 != 0, "Equal versions for indices {0}, {1}, version = {2}", left, right, myVersions[left]);
      return cmp2 > 0 ? 1 : -1;
    }
    
    private void HeapDown(int idx)
    {
      Assertion.Assert(idx >= 1 && idx < myStorage.Count, "Index {0} is not in range [1, {1})", idx, myStorage.Count);

      int n = myStorage.Count;
      int left = (idx << 1) | 0;
      while (left < n)
      {
        int nxt;
        if (left == n-1) nxt = left;
        else
        {
          int right = (idx << 1) | 1;
          nxt = Compare(left, right) < 0 ? left : right;
        }

        if (Compare(idx, nxt) <= 0) break;
        Swap(ref idx, nxt);
        left = (idx << 1) | 0;        
      }    
    }

    private void HeapUp(int idx)
    {      
      Assertion.Assert(idx >= 1 && idx < myStorage.Count, "Index {0} is not in range [1, {1})", idx, myStorage.Count);
      
      while (idx > 1 && Compare(idx, idx >> 1) < 0)
      {
        Swap(ref idx, idx >> 1);
      }
    }

    #endregion
  }


  /// <summary>
  /// Thread-safe implementation of priority queue data structure.
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public class BlockingPriorityQueue<T> : IPriorityQueue<T>
  {
    private readonly Lifetime myLifetime;
    private readonly JetPriorityQueue<T> myQueue;
    private readonly object mySentry = new object();

    public BlockingPriorityQueue(Lifetime lifetime, int initialCapacity = JetPriorityQueue<T>.DefaultCapacity, IComparer<T> comparer = null)
    {
      myLifetime = lifetime;
      myQueue = new JetPriorityQueue<T>(initialCapacity, comparer);
      lifetime.OnTermination(() =>
      {
        lock (mySentry)
        {
          Clear();
          Monitor.PulseAll(mySentry); //to wake up all waiters
        }
      });
    }

    public IEnumerator<T> GetEnumerator()
    {
      return ((IEnumerable<T>)ToArray(/* required for thread safety */)).GetEnumerator();
    }

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }

    public void Add(T item)
    {
      Enqueue(item);
    }

    public void Clear()
    {
      lock (mySentry) myQueue.Clear();
    }

    public bool Contains(T item)
    {
      lock (mySentry) return myQueue.Contains(item);
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      lock (mySentry) myQueue.CopyTo(array, arrayIndex);
    }

    public bool Remove(T item)
    {
      lock (mySentry) return myQueue.Remove(item);
    }

    public int Count { get { lock (mySentry) return myQueue.Count; }  }
    public bool IsReadOnly { get { lock (mySentry) return myQueue.IsReadOnly; } }

    public bool TryExtract(out T res)
    {
      lock (mySentry) return myQueue.TryExtract(out res);
    }

    public bool TryPeek(out T res)
    {
      lock (mySentry) return myQueue.TryPeek(out res);
    }

    [PublicAPI] public bool TryExtract(out T res, int intervalMs)
    {
      lock (mySentry)
      {
        var localIntervalMs = intervalMs;
        var stopwatch = LocalStopwatch.StartNew();
        
        do
        {
          if (myQueue.TryExtract(out res)) return true;

          if (myLifetime.Status >= LifetimeStatus.Terminating) return false;

          if (!Monitor.Wait(mySentry, localIntervalMs))
            break;

          var elapsed = stopwatch.ElapsedMilliseconds;
          if (elapsed >= intervalMs)
            break;

          localIntervalMs = intervalMs - (int)elapsed;
        } while (true);

        return myQueue.TryExtract(out res);
      }
    }

    [PublicAPI] public bool TryPeek(out T res, int intervalMs)
    {      
      lock (mySentry)
      {
        var localIntervalMs = intervalMs;
        var stopwatch = LocalStopwatch.StartNew();
        
        do
        {
          if (myQueue.TryPeek(out res)) return true;

          if (myLifetime.Status >= LifetimeStatus.Terminating) return false;
          
          if (!Monitor.Wait(mySentry, localIntervalMs))
            break;

          var elapsed = stopwatch.ElapsedMilliseconds;
          if (elapsed >= intervalMs)
            break;

          localIntervalMs = intervalMs - (int)elapsed;
        } while (true);

        return myQueue.TryPeek(out res);
      }
    }


    /// <summary>
    /// Returns first element from queue or waits until it appears. In case of lifetime termination throws PCE.
    /// </summary>
    /// <returns>First element in queue</returns>
    [PublicAPI] public T ExtractOrBlock()
    {
      lock (mySentry)
      {
        while (true)
        {
          if (myLifetime.Status >= LifetimeStatus.Terminating) throw new OperationCanceledException();

          if (myQueue.TryExtract(out var res)) return res;

          //no luck, wait for value
          Monitor.Wait(mySentry);
        }
      }
    }

    /// <summary>
    /// Enqueues an item and returns the total number of items in the queue right after enqueueing, in a thread-safe-consistent manner.
    /// </summary>
    [PublicAPI] public int Enqueue(T item)
    {
      lock (mySentry)
      {
        if (myLifetime.Status >= LifetimeStatus.Terminating) return 0;

        myQueue.Add(item);
        int count = myQueue.Count;
        Monitor.Pulse(mySentry);
        return count;
      }      
    }

    /// <summary>
    /// Copies data to an array, thread-safely.
    /// </summary>
    [PublicAPI] public T[] ToArray()
    {
      lock(mySentry)
        return myQueue.ToArray();
    }
  }


  public static class PriorityQueueEx
  {
    /// <summary>
    /// Same as <see cref="IPriorityQueue{T}.Add"/>
    /// </summary>
    /// <param name="queue"></param>
    /// <param name="val"></param>
    /// <typeparam name="T"></typeparam>
    [PublicAPI] public static void Enqueue<T>(this IPriorityQueue<T> queue, T val)
    {
      queue.Add(val);
    }
  

    [PublicAPI] public static T ExtractOrDefault<T>(this IPriorityQueue<T> queue)
    {
      return !queue.TryExtract(out var res) ? default(T) : res;
    }

    [PublicAPI] public static T Extract<T>(this IPriorityQueue<T> queue)
    {
      if (!queue.TryExtract(out var res))
      {
        throw new InvalidOperationException("Can't extract min, n");
      }
      return res;
    }

    [PublicAPI] public static T Peek<T>(this IPriorityQueue<T> queue)
    {
      if (!queue.TryPeek(out var res))
      {
        throw new InvalidOperationException("Can't extract min, n");
      }
      return res;
    }
  }
}
