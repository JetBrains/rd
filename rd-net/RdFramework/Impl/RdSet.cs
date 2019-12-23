using System;
using System.Collections;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
  public class RdSet<T> : RdReactiveBase, IViewableSet<T>
  {
    private readonly IViewableSet<T> mySet;

    public RdSet() : this(Polymorphic<T>.Read, Polymorphic<T>.Write) {}

    public RdSet(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue, IViewableSet<T> backingSet)
    {
      ValueCanBeNull = false;
      ReadValueDelegate = readValue;
      WriteValueDelegate = writeValue;

      mySet = backingSet ?? new ViewableSet<T>();
    }

    public RdSet(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue) : this(readValue, writeValue, null) { }


    #region Serializers

    public CtxReadDelegate<T> ReadValueDelegate { get; private set; }
    public CtxWriteDelegate<T> WriteValueDelegate { get; private set; }

    public static RdSet<T> Read(SerializationCtx ctx, UnsafeReader reader)
    {
      return Read(ctx, reader, Polymorphic<T>.Read, Polymorphic<T>.Write);
    }

    public static RdSet<T> Read(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
    {
      var id = reader.ReadRdId();
      return new RdSet<T>(readValue, writeValue).WithId(id);
    }

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdSet<T> value)
    {
      Assertion.Assert(!value.RdId.IsNil, "!value.RdId.IsNil");
      writer.Write(value.RdId);
    }
    #endregion


    #region Mastering

    public bool IsMaster = false;

    #endregion


    #region Init
    
    public bool OptimizeNested { [PublicAPI] get; set; }

    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);
      var serializationContext = SerializationContext;

      using (UsingLocalChange())
      {
        Advise(lifetime, it =>
        {
          if (!IsLocalChange) return;
          
          Wire.Send(RdId, (stream) =>
          {
            stream.Write((int)it.Kind);
            WriteValueDelegate(serializationContext, stream, it.Value);

            
            SendTrace?.Log($"{this} :: {it.Kind} :: {it.Value.PrintToString()}");
          });

        });
      }

      Wire.Advise(lifetime, this);

    }

    public override void OnWireReceived(UnsafeReader stream)
    {
      var kind = (AddRemove) stream.ReadInt();
      var value = ReadValueDelegate(SerializationContext, stream);
      ReceiveTrace?.Log($"{this} :: {kind} :: {value.PrintToString()}");
        
      using (UsingDebugInfo())
      {
        switch (kind)
        {
          case AddRemove.Add:
            mySet.Add(value);
            break;

          case AddRemove.Remove:
            mySet.Remove(value);
            break;

          default:
            throw new ArgumentOutOfRangeException();
        }
          
      }
    }

    #endregion


    #region Read delegation

    IEnumerator IEnumerable.GetEnumerator()
    {
      return mySet.GetEnumerator();
    }

    public IEnumerator<T> GetEnumerator()
    {
      return mySet.GetEnumerator();
    }

    public bool Contains(T item)
    {
      return mySet.Contains(item);
    }

    public void CopyTo(T[] array, int arrayIndex)
    {
      mySet.CopyTo(array, arrayIndex);
    }


    public int Count => mySet.Count;

    public bool IsReadOnly => mySet.IsReadOnly;


    public ISource<SetEvent<T>> Change => mySet.Change;

    #endregion



    #region Write delegation

    public bool Remove(T item)
    {
      using (UsingLocalChange())
        return mySet.Remove(item);
    }
    
    // ReSharper disable once AssignNullToNotNullAttribute
    #if NET35
    public
    #endif
    void
      #if !NET35
      ICollection<T>.
      #endif
      Add(T item)
    {
      AssertNullability(item);
      using (UsingLocalChange())
        mySet.Add(item);
    }

    #if !NET35
    public bool Add(T item)
    {
      AssertNullability(item);
      using (UsingLocalChange())
        return mySet.Add(item);
    }

    public void ExceptWith(IEnumerable<T> other)
    {
      using (UsingLocalChange())
        mySet.ExceptWith(other);
    }

    public void IntersectWith(IEnumerable<T> other)
    {
      using (UsingLocalChange())
        mySet.IntersectWith(other);
    }
    
    public void SymmetricExceptWith(IEnumerable<T> other)
    {
      using (UsingLocalChange())
        mySet.SymmetricExceptWith(other);
    }

    public void UnionWith(IEnumerable<T> other)
    {
      using (UsingLocalChange())        
        mySet.UnionWith(other);
    }
    #endif

    public void Clear()
    {
      using (UsingLocalChange()) 
        mySet.Clear();
    }
    
    #endregion



    #region ISet Read delegation

    #if !NET35
    public bool IsProperSubsetOf(IEnumerable<T> other) => mySet.IsProperSubsetOf(other);

    public bool IsProperSupersetOf(IEnumerable<T> other) => mySet.IsProperSupersetOf(other);

    public bool IsSubsetOf(IEnumerable<T> other) => mySet.IsSubsetOf(other);

    public bool IsSupersetOf(IEnumerable<T> other) => mySet.IsSupersetOf(other);

    public bool Overlaps(IEnumerable<T> other) => mySet.Overlaps(other);

    public bool SetEquals(IEnumerable<T> other) => mySet.SetEquals(other);
    #endif

    #endregion

    

    public void Advise(Lifetime lifetime, Action<SetEvent<T>> handler)
    {
      if (IsBound) AssertThreading();
      using (UsingDebugInfo())
        mySet.Advise(lifetime, handler);
    }


    protected override string ShortName => "set";
    public override void Print(PrettyPrinter printer)
    {
      base.Print(printer);

      printer.Print(" [");
      if (Count > 0) printer.Println();

      using (printer.IndentCookie())
      {
        foreach (var v in this)
        {
          v.PrintEx(printer);
          printer.Println();
        }
      }
      printer.Println("]");
    }
  }

}