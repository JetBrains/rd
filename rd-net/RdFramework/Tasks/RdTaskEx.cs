using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Lifetimes;
using JetBrains.Threading;

namespace JetBrains.Rd.Tasks
{
  public static class RdTaskEx
  {
    public static bool IsSuceedeed<T>(this IRdTask<T> task) => task.Result.HasValue() && task.Result.Value.Status == RdTaskStatus.Success;
    public static bool IsCanceled<T>(this IRdTask<T> task) => task.Result.HasValue() && task.Result.Value.Status == RdTaskStatus.Canceled;
    public static bool IsFaulted<T>(this IRdTask<T> task) => task.Result.HasValue() && task.Result.Value.Status == RdTaskStatus.Faulted;

    public static bool Wait<T>(this IRdTask<T> task, TimeSpan timeout) => SpinWaitEx.SpinUntil(Lifetime.Eternal, timeout, () => task.Result.HasValue());
    
#if !NET35
    public static RdTask<T> ToRdTask<T>(this Task<T> task)
    {
      var res = new RdTask<T>();
      task.ContinueWith(t =>
      {
        if (t.IsCanceled)
          res.SetCancelled();
        else if (t.IsFaulted)
          res.Set(t.Exception?.Flatten().GetBaseException());
        else
          res.Set(t.Result);
      }, CancellationToken.None, TaskContinuationOptions.ExecuteSynchronously, TaskScheduler.Current);
      return res;
    }

    [PublicAPI]public static void Set<TReq, TRes>(this IRdEndpoint<TReq, TRes> endpoint, Func<Lifetime, TReq, Task<TRes>> handler)
    {
      endpoint.Set((lt, req) => handler(lt, req).ToRdTask());
    }

#endif

    
    
    [PublicAPI] public static void Set<TReq, TRes>(this IRdEndpoint<TReq, TRes> endpoint, Func<TReq, TRes> handler)
    {
      endpoint.Set((_, req) => RdTask<TRes>.Successful(handler(req)));
    }
    
    [PublicAPI]public static void SetVoid<TReq>(this IRdEndpoint<TReq, Unit> endpoint, Action<TReq> handler)
    {
      endpoint.Set(req =>
      {
        handler(req);
        return Unit.Instance;
      });
    }
  }
}