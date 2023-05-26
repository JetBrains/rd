global using static Test.RdFramework.Util.ReactiveFactory;

using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;

namespace Test.RdFramework.Util;

/// <summary>
/// Test-only helper to simplify creating of reactive primitives with polymorphic serializers
/// </summary>
public static class ReactiveFactory
{
  public static RdProperty<T> NewRdProperty<T>(bool isMaster = false)
  {
    return new RdProperty<T>(Polymorphic<T>.Read, Polymorphic<T>.Write) { IsMaster = isMaster };
  }
  
  public static AsyncRdProperty<T> NewAsyncRdProperty<T>(bool isMaster = false)
  {
    return new AsyncRdProperty<T>(Polymorphic<T>.Read, Polymorphic<T>.Write) { IsMaster = isMaster };
  }

  public static RdSignal<T> NewRdSignal<T>()
  {
    return new RdSignal<T>(Polymorphic<T>.Read, Polymorphic<T>.Write);
  }

  public static RdList<T> NewRdList<T>(bool optimizeNested = false)
  {
    return new RdList<T>(Polymorphic<T>.Read, Polymorphic<T>.Write) { OptimizeNested = optimizeNested };
  }

  public static RdSet<T> NewRdSet<T>(bool isMaster = false)
  {
    return new RdSet<T>(Polymorphic<T>.Read, Polymorphic<T>.Write) { IsMaster = isMaster };
  }
  
  public static AsyncRdSet<T> NewAsyncRdSet<T>(bool isMaster = false)
  {
    return new AsyncRdSet<T>(Polymorphic<T>.Read, Polymorphic<T>.Write) { IsMaster = isMaster };
  }

  public static RdMap<TKey, TValue> NewRdMap<TKey, TValue>(bool isMaster = false, bool optimizeNested = false)
  {
    return new RdMap<TKey, TValue>(Polymorphic<TKey>.Read, Polymorphic<TKey>.Write, Polymorphic<TValue>.Read, Polymorphic<TValue>.Write)
    {
      IsMaster = isMaster, 
      OptimizeNested = optimizeNested
    };
  }
  
  public static AsyncRdMap<TKey, TValue> NewAsyncRdMap<TKey, TValue>(bool isMaster = false)
  {
    return new AsyncRdMap<TKey, TValue>(Polymorphic<TKey>.Read, Polymorphic<TKey>.Write, Polymorphic<TValue>.Read, Polymorphic<TValue>.Write) { IsMaster = isMaster };
  }


  public static RdCall<TReq, TRes> NewRdCall<TReq, TRes>()
  {
    return new RdCall<TReq, TRes>(Polymorphic<TReq>.Read, Polymorphic<TReq>.Write, Polymorphic<TRes>.Read, Polymorphic<TRes>.Write);
  }
}