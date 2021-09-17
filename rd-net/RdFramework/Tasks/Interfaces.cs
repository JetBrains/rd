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
    void Set(Func<Lifetime, TReq, RdTask<TRes>> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null);
  }

  public interface IRdCall<in TReq, TRes>
  {
    TRes Sync(TReq request, RpcTimeouts? timeouts = null);
    
    [Obsolete("Use overload with Lifetime")]
    IRdTask<TRes> Start(TReq request, IScheduler? responseScheduler = null);
    IRdTask<TRes> Start(Lifetime lifetime, TReq request, IScheduler? responseScheduler = null);
  }
}