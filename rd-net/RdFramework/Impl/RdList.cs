using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.ComponentModel;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

// ReSharper disable InconsistentNaming

namespace JetBrains.Rd.Impl
{
  public class RdList<V> : RdReactiveBase, IViewableList<V>
#if !NET35
    , INotifyCollectionChanged
#endif
  
  {
    private readonly ViewableList<V> myList = new ViewableList<V>();
       
    
    public RdList() : this(Polymorphic<V>.Read, Polymorphic<V>.Write) {}

    public RdList(CtxReadDelegate<V> readValue, CtxWriteDelegate<V> writeValue, long nextVersion = 1L)
    {
      myNextVersion = nextVersion;
      ValueCanBeNull = false;

      ReadValueDelegate = readValue;
      WriteValueDelegate = writeValue;
      
      //WPF integration
      this.AdviseAddRemove(Lifetime.Eternal, (kind, idx, v) =>
      {
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("Item[]"));
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("Count"));
#if !NET35
        CollectionChanged?.Invoke(this, new NotifyCollectionChangedEventArgs(kind == AddRemove.Add ? NotifyCollectionChangedAction.Add : NotifyCollectionChangedAction.Remove, v, idx));
  #endif
      });
    }
    
    //WPF integration
#if !NET35    
    public event NotifyCollectionChangedEventHandler CollectionChanged;
#endif
    public override event PropertyChangedEventHandler PropertyChanged;



    #region Serializers


    [PublicAPI]
    public CtxReadDelegate<V> ReadValueDelegate { get; private set; }
    [PublicAPI]
    public CtxWriteDelegate<V> WriteValueDelegate { get; private set; }

    [PublicAPI]
    public static RdList<V> Read(SerializationCtx ctx, UnsafeReader reader)
    {
      return Read(ctx, reader, Polymorphic<V>.Read, Polymorphic<V>.Write);
    }
    [PublicAPI]
    public static RdList<V> Read(SerializationCtx ctx, UnsafeReader reader,CtxReadDelegate<V> readValue, CtxWriteDelegate<V> writeValue)
    {
      var nextVersion = reader.ReadLong();
      var id = reader.ReadRdId();
      return new RdList<V>(readValue, writeValue, nextVersion).WithId(id);
    }

    [PublicAPI]
    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdList<V> value)
    {
      Assertion.Assert(!value.RdId.IsNil, "!value.Id.IsNil");
      writer.Write(value.myNextVersion);
      writer.Write(value.RdId);
    }
    #endregion


    #region Versions

    private const int versionedFlagShift = 2; //change when you change AddUpdateRemove
    
    private long myNextVersion;
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

          if (!OptimizeNested && it.Kind != AddUpdateRemove.Remove)
          {
            it.NewValue.IdentifyPolymorphic(Proto.Identities, Proto.Identities.Next(RdId));
          }

          Wire.Send(RdId, SendContext.Of(serializationContext, it, this), (sendContext, stream) =>
          {
            var sContext = sendContext.SzrCtx;
            var evt = sendContext.Event;
            var me = sendContext.This;

            stream.Write((me.myNextVersion++ << versionedFlagShift) | (long)evt.Kind);
            
            stream.Write(evt.Index);
            if (evt.Kind != AddUpdateRemove.Remove) 
              me.WriteValueDelegate(sContext, stream, evt.NewValue);
            
            SendTrace?.Log($"list `{me.Location}` ({me.RdId}) :: {evt.Kind} :: index={evt.Index} :: " +
                            $"version = {me.myNextVersion - 1}" +
                            $"{(evt.Kind != AddUpdateRemove.Remove ? " :: value = " + evt.NewValue.PrintToString() : "")}");
          });

        });
      }

      Wire.Advise(lifetime, this);


      if (!OptimizeNested) //means values must be bindable
      {
        this.View(lifetime, (lf, index, value) =>
        {
          value.BindPolymorphic(lf, this, "["+index+"]"); //todo name will be not unique when you add elements in the middle of the list 
        });
      }
      
      
    }

    protected override string ShortName => "list";


    public override void OnWireReceived(UnsafeReader stream)
    {
      var header = stream.ReadLong();
      var opType = header & ((1 << versionedFlagShift) - 1);
      var version = header >> versionedFlagShift;

      var index = stream.ReadInt();

        
      var kind = (AddUpdateRemove) opType;
      V value = default(V);
      var isPut = kind == AddUpdateRemove.Add || kind == AddUpdateRemove.Update;
      if (isPut)
        value = ReadValueDelegate(SerializationContext, stream);
        
      ReceiveTrace?.Log(
        $"list `{Location}` ({RdId}) :: {kind} :: index={index} :: version = {version}{(isPut ? " :: value = " + value.PrintToString() : "")}");

      if (version != myNextVersion)
      {
        Assertion.Fail("Version conflict for {0} Expected version {1} received {2}. Are you modifying a list from two sides?", 
          Location,
          myNextVersion,
          version);
      }

        
      myNextVersion++;
        
      using (UsingDebugInfo())
      {
        switch (kind)
        {
          case AddUpdateRemove.Add:
            if (index < 0)
              myList.Add(value);
            else
              myList.Insert(index, value);
            break;
            
          case AddUpdateRemove.Update:
            // ReSharper disable once AssignNullToNotNullAttribute
            myList[index] = value;
            break;

          case AddUpdateRemove.Remove:
            myList.RemoveAt(index);
            break;

          default:
            throw new ArgumentOutOfRangeException(kind.ToString());
        }
      }    
    }

    #endregion


    #region Read delegation

    IEnumerator IEnumerable.GetEnumerator()
    {
      return myList.GetEnumerator();
    }

    public IEnumerator<V> GetEnumerator()
    {
      return myList.GetEnumerator();
    }

    public bool Contains(V item)
    {
      return myList.Contains(item);
    }

    public void CopyTo(V[] array, int arrayIndex)
    {
      myList.CopyTo(array, arrayIndex);
    }


    public int Count => myList.Count;

    public bool IsReadOnly => myList.IsReadOnly;


    public int IndexOf(V item)
    {
      return myList.IndexOf(item);
    }

    public ISource<ListEvent<V>> Change => myList.Change;

    #endregion


    #region Write delegation

    public void Insert(int index, V value)
    {
      using (UsingLocalChange())
        myList.Insert(index, value);
    }


    public void RemoveAt(int index)
    {
      using (UsingLocalChange())
        myList.RemoveAt(index);
    }


    public V this[int index]
    {
      get => myList[index];
      set
      {
        using (UsingLocalChange())
          myList[index] = value;
      }
    }


    public bool Remove(V item)
    {
      using (UsingLocalChange())
      {
        return myList.Remove(item);
      }
    }


    public void Add(V item)
    {
      using (UsingLocalChange())
      {
        myList.Add(item);
      }
    }


    public void Clear()
    {
      using (UsingLocalChange())
      {
        myList.Clear();
      }
    }

    #endregion


    public void Advise(Lifetime lifetime, Action<ListEvent<V>> handler)
    {
      if (IsBound) AssertThreading();

      using (UsingDebugInfo())
        myList.Advise(lifetime, handler);
    }


    public override void Print(PrettyPrinter printer)
    {
      base.Print(printer);
      if (!printer.PrintContent) return;

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