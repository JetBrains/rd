using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Specialized;
using System.ComponentModel;
using System.Linq;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Collections.Synchronized;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

#nullable disable

// ReSharper disable InconsistentNaming

namespace JetBrains.Rd.Impl
{
  public class  RdList<V> : RdReactiveBase, IViewableList<V>
#if !NET35
    , INotifyCollectionChanged
#endif
    where V : notnull
  {
    private readonly ViewableList<V> myList = new(new SynchronizedList<V>()/*to have thread safe print*/);
       
    
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
      Assertion.Assert(!value.RdId.IsNil);
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
    [ItemCanBeNull] private List<LifetimeDefinition> myBindDefinitions;

    protected override void Unbind()
    {
      base.Unbind();
      if (myBindDefinitions is { } definitions)
      {
        myBindDefinitions = null;
        foreach (var definition in definitions) 
          definition?.Terminate();
      }
    }

    protected override void PreInit(Lifetime lifetime, IProtocol parentProto)
    {
      base.PreInit(lifetime, parentProto);

      if (!OptimizeNested)
      {
        Assertion.Assert(myBindDefinitions == null);
        myBindDefinitions ??= new List<LifetimeDefinition>(myList.Count);
        
        for (var index = 0; index < Count; index++)
        {
          var item = this[index];
          if (item != null)
          {
            item.IdentifyPolymorphic(parentProto.Identities, parentProto.Identities.Next(RdId));
            myBindDefinitions.Add(TryPreBindValue(lifetime, item, index, false));
          }
        }
      }
      
      parentProto.Wire.Advise(lifetime, this);
    }

    protected override void Init(Lifetime lifetime, IProtocol proto, SerializationCtx ctx)
    {
      base.Init(lifetime, proto, ctx);
      
      if (!OptimizeNested)
      {
        Change.Advise(lifetime, it =>
        {
          if (AllowBindCookie.IsBindNotAllowed)
            proto.Scheduler.AssertThread(this);

          if (IsLocalChange)
          {
            if (it.Kind != AddUpdateRemove.Add)
              myBindDefinitions[it.Index]?.Terminate();

            if (it.Kind != AddUpdateRemove.Remove && it.NewValue != null)
            {
              it.NewValue.IdentifyPolymorphic(proto.Identities, proto.Identities.Next(RdId));
              myBindDefinitions.Insert(it.Index, TryPreBindValue(lifetime, it.NewValue, it.Index, false));
            }
          }
        });
      }
      
      using (UsingLocalChange())
      {
        Advise(lifetime, it =>
        {     
          if (!IsLocalChange) return;

          proto.Wire.Send(RdId, SendContext.Of(ctx, it, this), static(sendContext, stream) =>
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
          
          it.NewValue.BindPolymorphic();
        });
      }
    }

    protected override string ShortName => "list";


    public override RdWireableContinuation OnWireReceived(Lifetime lifetime, IProtocol proto, SerializationCtx ctx, UnsafeReader stream)
    {
      var header = stream.ReadLong();
      var opType = header & ((1 << versionedFlagShift) - 1);
      var version = header >> versionedFlagShift;

      var index = stream.ReadInt();

      var kind = (AddUpdateRemove) opType;
      var value = default(V);
      var isPut = kind is AddUpdateRemove.Add or AddUpdateRemove.Update;
      if (isPut)
        value = ReadValueDelegate(ctx, stream);

      var definition = value != null ? TryPreBindValue(lifetime, value, index, true) : null;
      
      return new RdWireableContinuation(lifetime, proto.Scheduler, () =>
      {
        ReceiveTrace?.Log($"list `{Location}` ({RdId}) :: {kind} :: index={index} :: version = {version}{(isPut ? " :: value = " + value.PrintToString() : "")}");

        if (version != myNextVersion)
        {
          definition?.Terminate();
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
              {
                if (definition != null)
                  myBindDefinitions.Add(definition);
                myList.Add(value);
              }
              else
              {
                if (definition != null)
                  myBindDefinitions.Insert(index, definition);
                myList.Insert(index, value);
              }

              break;

            case AddUpdateRemove.Update:
              // ReSharper disable once AssignNullToNotNullAttribute
              if (definition != null)
              {
                myBindDefinitions[index]?.Terminate();
                myBindDefinitions[index] = definition;
              }
              
              myList[index] = value;
              break;

            case AddUpdateRemove.Remove:
              if (definition != null)
              {
                myBindDefinitions[index]?.Terminate();
                myBindDefinitions.RemoveAt(index);
              }
              myList.RemoveAt(index);
              break;

            default:
              throw new ArgumentOutOfRangeException(kind.ToString());
          }
        }    
      });
    }
    
#nullable restore
    private LifetimeDefinition? TryPreBindValue(Lifetime lifetime, V? value, int index, bool bindAlso)
    {
      if (OptimizeNested || value == null)
        return null;

      var definition = new LifetimeDefinition();
      try
      {
        value.PreBindPolymorphic(definition.Lifetime, this, "["+index+"]"); //todo name will be not unique when you add elements in the middle of the list
        if (bindAlso)
          value.BindPolymorphic();
        
        lifetime.Definition.Attach(definition, true);
        return definition;
      }
      catch
      {
        definition.Terminate();
        throw;
      }
    }
#nullable disable


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

    public override RdBindableBase FindByRName(RName rName)
    {
      var rootName = rName.GetNonEmptyRoot();
      var localName = rootName.LocalName.ToString();
      if (!localName.StartsWith("[") || !localName.EndsWith("]"))
        return null;

      var stringIndex = localName.Substring(1, localName.Length - 2);
      if (!int.TryParse(stringIndex, out var index))
        return null;

      if (!(myList.ElementAtOrDefault(index) is RdBindableBase element))
        return null;

      if (rootName == rName)
        return element;

      return element.FindByRName(rName.DropNonEmptyRoot());
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