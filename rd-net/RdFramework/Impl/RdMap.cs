using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

// ReSharper disable InconsistentNaming

namespace JetBrains.Rd.Impl
{

  public class RdMap<K, V> : RdReactiveBase, IViewableMap<K, V>
  {
    private readonly ViewableMap<K, V> myMap = new ViewableMap<K, V>();

    public RdMap() : this(Polymorphic<K>.Read, Polymorphic<K>.Write, Polymorphic<V>.Read, Polymorphic<V>.Write) {}

    public RdMap(CtxReadDelegate<K> readKey, CtxWriteDelegate<K> writeKey, CtxReadDelegate<V> readValue, CtxWriteDelegate<V> writeValue)
    {
      ValueCanBeNull = false;

      ReadKeyDelegate = readKey;
      WriteKeyDelegate = writeKey;
      
      ReadValueDelegate = readValue;
      WriteValueDelegate = writeValue;
    }


    #region Serializers

    [PublicAPI]
    public CtxReadDelegate<K> ReadKeyDelegate { get; private set; }
    [PublicAPI]
    public CtxWriteDelegate<K> WriteKeyDelegate { get; private set; }

    [PublicAPI]
    public CtxReadDelegate<V> ReadValueDelegate { get; private set; }
    [PublicAPI]
    public CtxWriteDelegate<V> WriteValueDelegate { get; private set; }

    [PublicAPI]
    public static RdMap<K, V> Read(SerializationCtx ctx, UnsafeReader reader)
    {
      return Read(ctx, reader, Polymorphic<K>.Read, Polymorphic<K>.Write, Polymorphic<V>.Read, Polymorphic<V>.Write);
    }
    [PublicAPI]
    public static RdMap<K,V> Read(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<K> readKey, CtxWriteDelegate<K> writeKey, CtxReadDelegate<V> readValue, CtxWriteDelegate<V> writeValue)
    {
      var id = reader.ReadRdId();
      return new RdMap<K, V>(readKey, writeKey, readValue, writeValue).WithId(id);
    }

    [PublicAPI]
    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdMap<K, V> value)
    {
      Assertion.Assert(!value.RdId.IsNil, "!value.RdId.IsNil");
      writer.Write(value.RdId);
    }
    #endregion


    #region Versions

    private const int versionedFlagShift = 8;
    private const int Ack = (int)AddUpdateRemove.Remove + 1;
    
    public bool IsMaster = false;
    private long myNextVersion;
    private readonly Dictionary<K, long> myPendingForAck = new Dictionary<K, long>();

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

          AssertNullability(it.Key);

          if (it.Kind != AddUpdateRemove.Remove) AssertNullability(it.NewValue);

          if (!OptimizeNested && it.Kind != AddUpdateRemove.Remove)
          {
            it.NewValue.IdentifyPolymorphic(Proto.Identities, Proto.Identities.Next(RdId));
          }

          Wire.Send(RdId, SendContext.Of(serializationContext, it, this), (sendContext, stream) =>
          {
            var sContext = sendContext.SzrCtx;
            var evt = sendContext.Event;
            var me = sendContext.This;
            var versionedFlag = me.IsMaster ? 1 << versionedFlagShift : 0;
            stream.Write(versionedFlag | (int)evt.Kind);

            var version = ++me.myNextVersion;
            if (me.IsMaster)
            {
              me.myPendingForAck[evt.Key] = version;
              stream.Write(version);
            }              
            
            me.WriteKeyDelegate(sContext, stream, evt.Key);
            if (evt.Kind != AddUpdateRemove.Remove) 
              me.WriteValueDelegate(sContext, stream, evt.NewValue);


            SendTrace?.Log($"{me} :: {evt.Kind} :: key = {evt.Key.PrintToString()}"
              + (me.IsMaster     ? " :: version = " + version : "")
              + (evt.Kind != AddUpdateRemove.Remove  ? " :: value = " + evt.NewValue.PrintToString() : ""));
          });

        });
      }

      Wire.Advise(lifetime, this);


      if (!OptimizeNested && !typeof(V).IsValueType) //means values must be bindable
      {
        this.View(lifetime, (lf, k, v) =>
        {
          RdBindableEx.BindPolymorphic(v!, lf, this, "["+k+"]");
        });
      }
    }


    public override void OnWireReceived(UnsafeReader stream)
    {
      var header = stream.ReadInt();
      var msgVersioned = (header >> versionedFlagShift) != 0;
      var opType = header & ((1 << versionedFlagShift) - 1);

      var version = msgVersioned ? stream.ReadLong() : 0L;

      var key = ReadKeyDelegate(SerializationContext, stream);

      if (opType == Ack)
      {
        string error = null;

        if (!msgVersioned) error = "Received ACK while msg hasn't versioned flag set";
        else if (!IsMaster) error = "Received ACK when not a Master";
        else if (!myPendingForAck.TryGetValue(key, out var pendingVersion)) error = "No pending for ACK";
        else if (pendingVersion < version)
          error = $"Pending version `{pendingVersion}` < ACK version `{version}`";
        // Good scenario
        else if (pendingVersion == version)
          myPendingForAck.Remove(key);
        //else do nothing, silently drop

        var isError = !string.IsNullOrEmpty(error);
        if (ourLogReceived.IsTraceEnabled() || isError)
        {
          ourLogReceived.LogFormat(isError ? LoggingLevel.ERROR : LoggingLevel.TRACE,
            "{0}  :: ACK :: key = {1} :: version = {2}{3}", this, key.PrintToString(), version,
            isError ? " >> " + error : "");
        }
      }
      else
      {
        using (UsingDebugInfo())
        {
          var kind = (AddUpdateRemove) opType;
          switch (kind)
          {
            case AddUpdateRemove.Add:
            case AddUpdateRemove.Update:
              var value = ReadValueDelegate(SerializationContext, stream);
              
              ReceiveTrace?.Log($"{this} :: {kind} :: key = {key.PrintToString()}" +
                                 (msgVersioned ? " :: version = " + version : "") +
                                 $" :: value = {value.PrintToString()}"
                                 );
              

              if (msgVersioned || !IsMaster || !myPendingForAck.ContainsKey(key))
                myMap[key] = value;
              else ReceiveTrace?.Log(">> CHANGE IGNORED");

              break;

            case AddUpdateRemove.Remove:
              
              ReceiveTrace?.Log($"{this} :: {kind} :: key = {key.PrintToString()}"
                + (msgVersioned ? " :: version = " + version : "")
                );
            

              if (msgVersioned || !IsMaster || !myPendingForAck.ContainsKey(key))
                myMap.Remove(key);
              else ReceiveTrace?.Log(">> CHANGE IGNORED");

              break;

            default:
              throw new ArgumentOutOfRangeException(kind.ToString());
          }
        }

        if (msgVersioned)
        {
          Wire.Send(RdId, (innerWriter) =>
          {
            innerWriter.Write((1 << versionedFlagShift) | Ack);
            innerWriter.Write(version);
            WriteKeyDelegate.Invoke(SerializationContext, innerWriter, key);
            
            SendTrace?.Log($"{this} :: ACK :: key = {key.PrintToString()} :: version = {version}");
          });

          if (IsMaster)
            ourLogReceived.Error("Both ends are masters: {0}", Location);
        }
      }
    }

    #endregion


    #region Read delegation

    IEnumerator IEnumerable.GetEnumerator()
    {
      return myMap.GetEnumerator();
    }

    public IEnumerator<KeyValuePair<K, V>> GetEnumerator()
    {
      return myMap.GetEnumerator();
    }

    public bool Contains(KeyValuePair<K, V> item)
    {
      return myMap.Contains(item);
    }

    public void CopyTo(KeyValuePair<K, V>[] array, int arrayIndex)
    {
      myMap.CopyTo(array, arrayIndex);
    }


    public int Count => myMap.Count;

    public bool IsReadOnly => myMap.IsReadOnly;

    public bool ContainsKey(K key)
    {
      return myMap.ContainsKey(key);
    }


    public bool TryGetValue(K key, out V value)
    {
      return myMap.TryGetValue(key, out value);
    }

    public ICollection<K> Keys => myMap.Keys;


    public ICollection<V> Values => myMap.Values;


    public ISource<MapEvent<K, V>> Change => myMap.Change;

    #endregion


    #region Write delegation
    //todo async!
    public void Add(K key, V value)
    {
      AssertNullability(value);
      using (UsingLocalChange())
      {
        myMap.Add(key, value);
      }
    }


    public bool Remove(K key)
    {
      using (UsingLocalChange())
      {
        return myMap.Remove(key);
      }
    }


    public V this[K key]
    {
      get => myMap[key];
      set
      {
        AssertNullability(value);
        using (UsingLocalChange())
        {
          myMap[key] = value;
        }
      }
    }


    public bool Remove(KeyValuePair<K, V> item)
    {
      using (UsingLocalChange())
      {
        return myMap.Remove(item);
      }
    }


    public void Add(KeyValuePair<K, V> item)
    {
      AssertNullability(item.Value);
      using (UsingLocalChange())
      {
        myMap.Add(item);
      }
    }


    public void Clear()
    {
      using (UsingLocalChange())
      {
        myMap.Clear();
      }
    }

    #endregion


    public void Advise(Lifetime lifetime, Action<MapEvent<K, V>> handler)
    {
      if (IsBound) AssertThreading();
      using (UsingDebugInfo())
      {
        myMap.Advise(lifetime, handler);
      }
    }

    public override RdBindableBase FindByRName(RName rName)
    {
      var rootName = rName.GetNonEmptyRoot();
      var localName = rootName.LocalName.ToString();
      if (!localName.StartsWith("[") || !localName.EndsWith("]"))
        return null;

      var stringKey = localName.Substring(1, localName.Length - 2);

      foreach (var (key, value) in myMap)
      {
        if (key.ToString() != stringKey)
          continue;

        if (!(value is RdBindableBase bindableValue))
          break;

        if (rootName == rName)
          return bindableValue;

        return bindableValue.FindByRName(rName.DropNonEmptyRoot());
      }

      return null;
    }

    protected override string ShortName => "map";

    public override void Print(PrettyPrinter printer)
    {
      base.Print(printer);
      if (!printer.PrintContent) return;
      
      printer.Print(" [");
      if (Count > 0) printer.Println();

      using (printer.IndentCookie())
      {
        foreach (var kv in this)
        {
          kv.Key.PrintEx(printer);
          printer.Print(" => ");
          kv.Value.PrintEx(printer);
          printer.Println();
        }
      }
      printer.Println("]");
    }
  }

}