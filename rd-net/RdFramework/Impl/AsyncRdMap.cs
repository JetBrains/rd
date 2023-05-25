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

namespace JetBrains.Rd.Impl;

public class AsyncRdMap<K, V> : IRdBindable, IAsyncSource<MapEvent<K, V>>, IDictionary<K, V> where K : notnull
{
  private readonly RdMapBackend myMap;
  private readonly AsyncSignal<MapEvent<K, V>> mySignal = new();

  public AsyncRdMap(CtxReadDelegate<K> readKey, CtxWriteDelegate<K> writeKey, CtxReadDelegate<V> readValue, CtxWriteDelegate<V> writeValue)
  {
    myMap = new RdMapBackend(readKey, writeKey, readValue, writeValue);
    myMap.Change.Advise(Lifetime.Eternal, x => mySignal.Fire(x));
  }

  public IAsyncSource<MapEvent<K, V>> Change => mySignal;
  
  public bool IsMaster = false;
  
  public RdId RdId
  {
    get
    {
      lock (myMap)
        return myMap.RdId;
    }
    set
    {
      lock (myMap)
        myMap.RdId = value;
    }
  }

  public void PreBind(Lifetime lf, IRdDynamic parent, string name)
  {
    lock (myMap) 
      myMap.PreBind(lf, parent, name);
  }

  public void Bind()
  {
    lock (myMap) 
      myMap.Bind();
  }

  public void Identify(IIdentities identities, RdId id)
  {
    lock (myMap) 
      myMap.Identify(identities, id);
  }

  public bool OptimizeNested
  {
    get => true;
    set { }
  }
  
  public bool ValueCanBeNull
  {
    get => myMap.ValueCanBeNull;
    set => myMap.ValueCanBeNull = value;
  }

  public bool Async
  {
    get => true;
    set { }
  }

  public RName Location
  {
    get
    {
      lock (myMap)
        return myMap.Location;
    }
  }

  public IProtocol? TryGetProto()
  {
    return myMap.TryGetProto();
  }

  public bool TryGetSerializationContext(out SerializationCtx ctx)
  {
    return myMap.TryGetSerializationContext(out ctx);
  }

  public bool ContainsKey(K key)
  {
    return myMap.ContainsKey(key);
  }

  public void Add(K key, V value)
  {
    lock (myMap)
      myMap.Add(key, value);
  }

  public bool Remove(K key)
  {
    lock (myMap)
      return myMap.Remove(key);
  }

  public bool TryGetValue(K key, out V value)
  {
    return myMap.TryGetValue(key, out value);
  }

  public V this[K key]
  {
    get
    {
      lock (myMap)
        return myMap[key];
    }
    set
    {
      lock (myMap)
        myMap[key] = value; 
    }
  }

  public ICollection<K> Keys => myMap.Keys;
  public ICollection<V> Values => myMap.Values;

  void ICollection<KeyValuePair<K, V>>.Add(KeyValuePair<K, V> item) => Add(item.Key, item.Value);

  public void Clear()
  {
    lock (myMap) 
      myMap.Clear();
  }

  public bool Contains(KeyValuePair<K, V> item) => myMap.Contains(item);

  public void CopyTo(KeyValuePair<K, V>[] array, int arrayIndex)
  {
    myMap.CopyTo(array, arrayIndex);
  }

  public bool Remove(KeyValuePair<K, V> item)
  {
    lock (myMap)
      return myMap.Remove(item);
  }

  public int Count => myMap.Count;
  public bool IsReadOnly => myMap.IsReadOnly;
  
  public IEnumerator<KeyValuePair<K, V>> GetEnumerator() => myMap.GetEnumerator();
  IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();

  [PublicAPI]
  public static AsyncRdMap<K, V> Read(SerializationCtx ctx, UnsafeReader reader)
  {
    return Read(ctx, reader, Polymorphic<K>.Read, Polymorphic<K>.Write, Polymorphic<V>.Read, Polymorphic<V>.Write);
  }
  
  [PublicAPI]
  public static AsyncRdMap<K,V> Read(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<K> readKey, CtxWriteDelegate<K> writeKey, CtxReadDelegate<V> readValue, CtxWriteDelegate<V> writeValue)
  {
    var id = reader.ReadRdId();
    return new AsyncRdMap<K, V>(readKey, writeKey, readValue, writeValue).WithId(id);
  }

  [PublicAPI]
  public static void Write(SerializationCtx ctx, UnsafeWriter writer, AsyncRdMap<K, V> value)
  {
    Assertion.Assert(!value.RdId.IsNil);
    writer.Write(value.RdId);
  }


  [PublicAPI] public CtxReadDelegate<K> ReadKeyDelegate => myMap.ReadKeyDelegate;
  [PublicAPI] public CtxWriteDelegate<K> WriteKeyDelegate => myMap.WriteKeyDelegate;

  [PublicAPI] public CtxReadDelegate<V> ReadValueDelegate => myMap.ReadValueDelegate;
  [PublicAPI] public CtxWriteDelegate<V> WriteValueDelegate => myMap.WriteValueDelegate;

  private class RdMapBackend : RdMap<K, V>
  {
    public RdMapBackend(CtxReadDelegate<K> readKey, CtxWriteDelegate<K> writeKey, CtxReadDelegate<V> readValue, CtxWriteDelegate<V> writeValue) : base(readKey, writeKey, readValue, writeValue)
    {
      OptimizeNested = true;
      Async = true;
    }
    
    protected override void AssertBindingThread()
    {
    }

    protected override void AssertThreading()
    {
      
    }

    public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader stream, IRdWireableDispatchHelper dispatchHelper)
    {
      lock (this)
        base.OnWireReceived(proto, ctx, stream, new DelegatingDispatchHelper(dispatchHelper));
    }
  }
  
  private class DelegatingDispatchHelper : IRdWireableDispatchHelper
  {
    private readonly IRdWireableDispatchHelper myDispatchHelper;
    
    public RdId RdId => myDispatchHelper.RdId;
    public Lifetime Lifetime => myDispatchHelper.Lifetime;

    public DelegatingDispatchHelper(IRdWireableDispatchHelper dispatchHelper)
    {
      myDispatchHelper = dispatchHelper;
    }
    
    public void Dispatch(Lifetime lifetime, IScheduler? scheduler, Action action)
    {
      myDispatchHelper.Dispatch(SynchronousScheduler.Instance, action);
    }
  }

  public void Print(PrettyPrinter printer)
  {
    lock (myMap) 
      myMap.Print(printer);
  }

  public void AdviseOn(Lifetime lifetime, IScheduler scheduler, Action<MapEvent<K, V>> action)
  {
    lock (myMap)
    {
      myMap.Advise(lifetime, e =>
      {
        scheduler.Queue(() => action(e));
      });
    }
  }
}