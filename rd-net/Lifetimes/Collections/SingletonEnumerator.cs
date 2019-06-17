using System.Collections;
using System.Collections.Generic;

namespace JetBrains.Collections
{
  /// <summary>
  /// Enumerator for imaginary collection from single value
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public struct SingletonEnumerator<T> : IEnumerator<T>
  {
    private bool myHasNext;
    public SingletonEnumerator(T next) : this()
    {
      Current = next;
      myHasNext = true;
    }

    public void Dispose() {}

    public bool MoveNext()
    {
      if (!myHasNext)
        return false;
      else
      {
        myHasNext = false;
        return true;
      }
    }

    public void Reset() { myHasNext = true; }

    public T Current { get; }

    object IEnumerator.Current => Current;
  }
}