using System;
using System.Collections;
using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl;

public class AsyncRdSet<T> : IRdBindable, IAsyncSource<SetEvent<T>>, 
#if NET35
    ICollection<T>
#else
  ISet<T>, IReadOnlyCollection<T>
#endif 
  where T : notnull
{
  private readonly RdSetBackend mySet;
  private readonly AsyncSignal<SetEvent<T>> mySignal = new();

  public AsyncRdSet(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
  {
    mySet = new RdSetBackend(readValue, writeValue);
    mySet.Change.Advise(Lifetime.Eternal, x => mySignal.Fire(x));
  }

  public IAsyncSource<SetEvent<T>> Change => mySignal;
  
  public bool IsMaster = false;
  
  public RdId RdId
  {
    get
    {
      lock (mySet)
        return mySet.RdId;
    }
    set
    {
      lock (mySet)
        mySet.RdId = value;
    }
  }

  public void PreBind(Lifetime lf, IRdDynamic parent, string name)
  {
    lock (mySet) 
      mySet.PreBind(lf, parent, name);
  }

  public void Bind()
  {
    lock (mySet) 
      mySet.Bind();
  }

  public void Identify(IIdentities identities, RdId id)
  {
    lock (mySet) 
      mySet.Identify(identities, id);
  }

  public bool OptimizeNested
  {
    get => true;
    set { }
  }
  
  public bool ValueCanBeNull
  {
    get => mySet.ValueCanBeNull;
    set => mySet.ValueCanBeNull = value;
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
      lock (mySet)
        return mySet.Location;
    }
  }

  public IProtocol? TryGetProto()
  {
    return mySet.TryGetProto();
  }

  public bool TryGetSerializationContext(out SerializationCtx ctx)
  {
    return mySet.TryGetSerializationContext(out ctx);
  }

  public int Count => mySet.Count;
  public bool IsReadOnly => mySet.IsReadOnly;

#if !NET35
  bool ISet<T>.Add(T item)
  {
    lock (mySet)
      return mySet.Add(item);
  }

  public void UnionWith(IEnumerable<T> other)
  {
    lock (mySet) 
      mySet.UnionWith(other);
  }

  public void IntersectWith(IEnumerable<T> other)
  {
    lock (mySet) 
      mySet.IntersectWith(other);
  }

  public void ExceptWith(IEnumerable<T> other)
  {
    lock (mySet) 
      mySet.ExceptWith(other);
  }

  public void SymmetricExceptWith(IEnumerable<T> other)
  {
    lock (mySet) 
      mySet.SymmetricExceptWith(other);
  }

  public bool IsSubsetOf(IEnumerable<T> other)
  {
    return mySet.IsSubsetOf(other);
  }

  public bool IsSupersetOf(IEnumerable<T> other)
  {
    return mySet.IsSubsetOf(other);
  }

  public bool IsProperSupersetOf(IEnumerable<T> other)
  {
    return mySet.IsProperSupersetOf(other);
  }

  public bool IsProperSubsetOf(IEnumerable<T> other)
  {
    return mySet.IsProperSubsetOf(other);
  }

  public bool Overlaps(IEnumerable<T> other)
  {
    return mySet.Overlaps(other);
  }

  public bool SetEquals(IEnumerable<T> other)
  {
    return mySet.Overlaps(other);
  }
#endif

  public void Add(T item)
  {
    lock (mySet)
      mySet.Add(item);
  }

  public void Clear()
  {
    lock (mySet)
      mySet.Clear();
  }

  public bool Contains(T item)
  {
    return mySet.Contains(item);
  }

  public void CopyTo(T[] array, int arrayIndex)
  {
    mySet.CopyTo(array, arrayIndex);
  }

  public bool Remove(T item)
  {
    lock (mySet) 
      return mySet.Remove(item);
  }

  public IEnumerator<T> GetEnumerator() => mySet.GetEnumerator();
  IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();

  public static AsyncRdSet<T> Read(SerializationCtx ctx, UnsafeReader reader)
  {
    return Read(ctx, reader, Polymorphic<T>.Read, Polymorphic<T>.Write);
  }

  public static AsyncRdSet<T> Read(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
  {
    var id = reader.ReadRdId();
    return new AsyncRdSet<T>(readValue, writeValue).WithId(id);
  }

  public static void Write(SerializationCtx ctx, UnsafeWriter writer, AsyncRdSet<T> value)
  {
    Assertion.Assert(!value.RdId.IsNil);
    writer.Write(value.RdId);
  }

  public CtxReadDelegate<T> ReadValueDelegate => mySet.ReadValueDelegate;
  public CtxWriteDelegate<T> WriteValueDelegate => mySet.WriteValueDelegate;

  private class RdSetBackend : RdSet<T>
  {
    public RdSetBackend(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue) : base(readValue, writeValue)
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
    
    public void Dispatch(IScheduler? scheduler, Action action)
    {
      myDispatchHelper.Dispatch(SynchronousScheduler.Instance, action);
    }
  }

  public void Print(PrettyPrinter printer)
  {
    lock (mySet) 
      mySet.Print(printer);
  }

  public void AdviseOn(Lifetime lifetime, IScheduler scheduler, Action<SetEvent<T>> action)
  {
    lock (mySet)
    {
      mySet.Advise(lifetime, e =>
      {
        scheduler.Queue(() => action(e));
      });
    }
  }
}