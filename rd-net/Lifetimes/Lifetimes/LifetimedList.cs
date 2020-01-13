using System;
using System.Collections;
using System.Collections.Generic;

namespace JetBrains.Lifetimes
{
  
  /// <summary>
  /// List for <see cref="ValueLifetimed{T}"/> entities. Not thread safe but designed to avoid <see cref="InvalidOperationException"/>
  /// from <see cref="IEnumerator.MoveNext"/> during concurrent modification. So no need to defense copy of internal array.
  ///
  /// There are no <see cref="ICollection{T}.Remove"/> method, it's append only. When item's <see cref="ValueLifetimed{T}.Lifetime"/> becomes
  /// not alive, item's <see cref="ValueLifetimed{T}.Value"/> is cleared (to avoid memory leak) and item is fully removed from internal
  /// <see cref="myItems"/> array after exponential growth phase in <see cref="EnsureCapacity"/>.
  ///
  ///  
  /// </summary>
  /// <typeparam name="T"></typeparam>
  public struct LifetimedList<T> : IEnumerable<ValueLifetimed<T>>
  {
    public struct Enumerator : IEnumerator<ValueLifetimed<T>>
    {
      private readonly ValueLifetimed<T>[] myItems;
      private readonly int mySize;
      private int myPos;

      public Enumerator(LifetimedList<T> list) : this()
      {
        myItems = list.myItems;  
        mySize = list.mySize;
        myPos = -1;
      }

      public void Dispose() {}

      public bool MoveNext()
      {
        while (++myPos < mySize)
          if (myItems[myPos].Lifetime.IsAlive)
            return true;
        return false;
      }            

      public void Reset() { myPos = -1; }
      public ValueLifetimed<T> Current => myItems[myPos];
      object IEnumerator.Current => Current;
    }
    
    //in x64 we have one free 4-bytes slot in this structure so let we use it for something meaningful
    //delimits items into two parts: with high priority (< Marker) and normal (>= Marker)
    public int Marker { get; private set; }
    private int mySize;
    private ValueLifetimed<T>[] myItems;

    public void Add(Lifetime lifetime, T value) => Add(new ValueLifetimed<T>(lifetime, value));
    public void AddPriorityItem(Lifetime lifetime, T value) => AddPriorityItem(new ValueLifetimed<T>(lifetime, value));
    
    public void Add(ValueLifetimed<T> item)
    {
      EnsureCapacity();
      myItems[mySize++] = item;
    }        

    public void AddPriorityItem(ValueLifetimed<T> item)
    {
      EnsureCapacity();
      Array.Copy(myItems, Marker, myItems, Marker+1, mySize-Marker);
      myItems[Marker++] = item;
      mySize++;
    }

    private void EnsureCapacity()
    {
      if (myItems == null)
        myItems = new ValueLifetimed<T>[1];
      
      if (mySize < myItems.Length)
        return;
      
      // we have to make new array ALWAYS at this point, because this method could be called during enumeration and we want enumeration to work in a snapshot fashion      
      var countAfterCleaning = 0;
      var markerDecrement = 0;
      var newItems = new ValueLifetimed<T>[mySize * 2]; //Count can't be zero here
      for (var i = 0; i < mySize; i++)
      {
        if (myItems[i].Lifetime.IsAlive)
          newItems[countAfterCleaning++] = myItems[i];
        else if (i < Marker)
            markerDecrement++;        
      }

      mySize = countAfterCleaning;
      Marker -= markerDecrement;
      myItems = newItems;      
    }        

    public void ClearValuesIfNotAlive()
    {
      for (var i = 0; i < mySize; i++)
      {
        myItems[i].ClearValueIfNotAlive();
      }
    }

    public Enumerator GetEnumerator()
    {
      return new Enumerator(this);
    }

    IEnumerator<ValueLifetimed<T>> IEnumerable<ValueLifetimed<T>>.GetEnumerator() => GetEnumerator();

    IEnumerator IEnumerable.GetEnumerator()
    {
      return GetEnumerator();
    }
  }
}