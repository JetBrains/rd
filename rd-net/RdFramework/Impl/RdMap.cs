using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Collections;
using JetBrains.Collections.Synchronized;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

// ReSharper disable InconsistentNaming

namespace JetBrains.Rd.Impl
{

  public class RdMap<K, V> : RdReactiveBase, IViewableMap<K, V> where K : notnull
  {
    private readonly ViewableMap<K, V> myMap = new(new SynchronizedDictionary<K, V>()/*to have thread safe print*/);

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
      Assertion.Assert(!value.RdId.IsNil);
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
    private volatile SynchronizedDictionary<K, LifetimeDefinition?>? myBindDefinitions;
    
    protected override void PreInit(Lifetime lifetime, IProtocol proto)
    {
      base.PreInit(lifetime, proto);

      if (!OptimizeNested)
      {
        var definitions  = new SynchronizedDictionary<K, LifetimeDefinition?>(myMap.Count); 
        
        foreach (var (key, value) in this)
        {
          if (value != null)
          {
            value.IdentifyPolymorphic(proto.Identities, proto.Identities.Next(RdId));
            var definition = TryPreBindValue(lifetime, key, value, false);
            if (definition != null)
              definitions.Add(key, definition);
          }
        }

        using var cookie = lifetime.UsingExecuteIfAlive();
        if (cookie.Succeed)
        {
          Assertion.Assert(myBindDefinitions == null);
          myBindDefinitions = definitions;
        }
        else
          return;
      }

      proto.Wire.Advise(lifetime, this);
    }

    protected override void Init(Lifetime lifetime, IProtocol proto, SerializationCtx ctx)
    {
      base.Init(lifetime, proto, ctx);
      
      if (!OptimizeNested)
      {
        Change.Advise(lifetime, it =>
        {
          AssertNullability(it.Key);

          if (it.Kind != AddUpdateRemove.Remove) AssertNullability(it.NewValue);

          if (IsLocalChange)
          {
            var definitions = TryGetBindDefinitions(lifetime);
            if (definitions == null)
              return;

            if (it.Kind != AddUpdateRemove.Add) 
              definitions[it.Key]?.Terminate();

            if (it.Kind == AddUpdateRemove.Remove)
              definitions.Remove(it.Key);

            if (it.Kind != AddUpdateRemove.Remove)
            {
              it.NewValue.IdentifyPolymorphic(proto.Identities, proto.Identities.Next(RdId));
              var definition = TryPreBindValue(lifetime, it.Key, it.NewValue, false);
              definitions[it.Key] = definition;
            }
          }
        });
      }

      using (UsingLocalChange())
      {
        Advise(lifetime, it =>
        {
          if (!IsLocalChange) return;

          proto.Wire.Send(RdId, SendContext.Of(ctx, it, this), static (sendContext, stream) =>
          {
            var sContext = sendContext.SzrCtx;
            var evt = sendContext.Event;
            var me = sendContext.This;
            var versionedFlag = me.IsMaster ? 1 << versionedFlagShift : 0;
            stream.Write(versionedFlag | (int)evt.Kind);

            var version = ++me.myNextVersion;
            if (me.IsMaster)
            {
              lock(me.myPendingForAck)
                me.myPendingForAck[evt.Key] = version;
              
              stream.Write(version);
            }

            me.WriteKeyDelegate(sContext, stream, evt.Key);
            if (evt.IsUpdate || evt.IsAdd)
              me.WriteValueDelegate(sContext, stream, evt.NewValue);


            SendTrace?.Log($"{me} :: {evt.Kind} :: key = {evt.Key.PrintToString()}"
                           + (me.IsMaster ? " :: version = " + version : "")
                           + (evt.Kind != AddUpdateRemove.Remove ? " :: value = " + evt.NewValue.PrintToString() : ""));
          });

          if (!OptimizeNested) 
            it.NewValue.BindPolymorphic();
        });
      }
    }

    protected override void Unbind()
    {
      base.Unbind();
      myBindDefinitions = null;
    }

    public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader stream, IRdWireableDispatchHelper dispatchHelper)
    {
      var header = stream.ReadInt();
      var msgVersioned = (header >> versionedFlagShift) != 0;
      var opType = header & ((1 << versionedFlagShift) - 1);

      var version = msgVersioned ? stream.ReadLong() : 0L;

      var key = ReadKeyDelegate(ctx, stream);

      var lifetime = dispatchHelper.Lifetime;
      if (opType == Ack)
      {
        dispatchHelper.Dispatch(() =>
        {
          lock (myPendingForAck)
          {
            string? error = null;
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
        });
      }
      else
      {
        var kind = (AddUpdateRemove)opType;
        var isPut = kind is AddUpdateRemove.Add or AddUpdateRemove.Update;
        var value = isPut ? ReadValueDelegate(ctx, stream) : default;
        var definition = TryPreBindValue(lifetime, key, value, true);

        dispatchHelper.Dispatch(() =>
        {
          if (msgVersioned || !IsMaster || !IsPendingForAck(key))
          {
            ReceiveTrace?.Log($"{this} :: {kind} :: key = {key.PrintToString()}" +
                              (msgVersioned ? " :: version = " + version : "") +
                              (isPut ? $" :: value = {value.PrintToString()}" : ""));
            
            if (isPut)
            {
              if (TryGetBindDefinitions(lifetime) is { } definitions)
              {
                if (kind == AddUpdateRemove.Update)
                  definitions[key]?.Terminate();

                definitions[key] = definition;
              }

              myMap[key] = value!;
            }
            else
            {
              if (TryGetBindDefinitions(lifetime) is { } definitions && definitions.TryGetValue(key, out var prevDefinition))
              {
                prevDefinition?.Terminate();
                definitions.Remove(key);
              }

              myMap.Remove(key);
            }
          }
          else
          {
            ReceiveTrace?.Log(">> CHANGE IGNORED");
          }

          if (msgVersioned)
          {
            proto.Wire.Send(RdId, innerWriter =>
            {
              innerWriter.Write((1 << versionedFlagShift) | Ack);
              innerWriter.Write(version);
              WriteKeyDelegate.Invoke(ctx, innerWriter, key);

              SendTrace?.Log($"{this} :: ACK :: key = {key.PrintToString()} :: version = {version}");
            });

            if (IsMaster)
              ourLogReceived.Error("Both ends are masters: {0}", Location);
          }
        });
      }
    }

    private SynchronizedDictionary<K, LifetimeDefinition?>? TryGetBindDefinitions(Lifetime lifetime)
    {
      var definitions = myBindDefinitions;
      return lifetime.IsAlive ? definitions : null;
    }

    private bool IsPendingForAck(K key)
    {
      lock (myPendingForAck)
        return myPendingForAck.ContainsKey(key);
    }
    
    private LifetimeDefinition? TryPreBindValue(Lifetime lifetime, K key, V? value,  bool bindAlso)
    {
      if (OptimizeNested || !value.IsBindable())
        return null;
      
      var definition = new LifetimeDefinition { Id = value };
      try
      {
        value.PreBindPolymorphic(definition.Lifetime, this, "["+key+"]");
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

    public override RdBindableBase? FindByRName(RName rName)
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