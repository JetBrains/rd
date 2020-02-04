using System;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;

namespace JetBrains.Rd.Tasks
{
  [PublicAPI] 
  public interface IRdTask<T>
  {
    //todo make RdTask as type alias for IReadonlyProperty<RdTaskResult<T>>
    IReadonlyProperty<RdTaskResult<T>> Result { get; }
  }
  
  public interface IRdEndpoint<TReq, TRes>
  {
    void Set(Func<Lifetime, TReq, RdTask<TRes>> handler, IScheduler cancellationAndRequestScheduler = null);
  }

  public interface ILifetimedRdCall<in TReq, TRes>
  {
    IRdTask<TRes> Start(Lifetime lifetime, TReq request, IScheduler responseScheduler = null);
  }

  public interface IRdCall<in TReq, TRes> : ILifetimedRdCall<TReq, TRes>
  {
    TRes Sync(TReq request, RpcTimeouts timeouts = null);
    IRdTask<TRes> Start(TReq request, IScheduler responseScheduler = null);
  }

  public interface ILifetimedRdCallWithEndpoint<TReq, TRes> : ILifetimedRdCall<TReq, TRes>, IRdEndpoint<TReq, TRes>
  {
  }
}