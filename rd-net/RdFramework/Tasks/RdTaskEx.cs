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
    [PublicAPI] public static bool IsSucceed<T>(this IRdTask<T> task) => task.Result.HasValue() && task.Result.Value.Status == RdTaskStatus.Success;
    [PublicAPI] public static bool IsCanceled<T>(this IRdTask<T> task) => task.Result.HasValue() && task.Result.Value.Status == RdTaskStatus.Canceled;
    [PublicAPI] public static bool IsFaulted<T>(this IRdTask<T> task) => task.Result.HasValue() && task.Result.Value.Status == RdTaskStatus.Faulted;

    public static bool Wait<T>(this IRdTask<T> task, TimeSpan timeout) => SpinWaitEx.SpinUntil(Lifetime.Eternal, timeout, () => task.Result.HasValue());
    

    public static RdTask<T> ToRdTask<T>(this Task<T> task)
    {
      var res = new RdTask<T>();
      task.ContinueWith(t =>
      {
        if (t.IsOperationCanceled())
          res.SetCancelled();
        else if (t.IsFaulted)
          res.Set(t.Exception?.Flatten().GetBaseException());
        else
          res.Set(t.Result);
      }, CancellationToken.None, TaskContinuationOptions.ExecuteSynchronously, TaskScheduler.Current);
      return res;
    }

    public static RdTask<Unit> ToRdTask(this Task task)
    {
      var res = new RdTask<Unit>();
      task.ContinueWith(t =>
      {
        if (t.IsOperationCanceled())
          res.SetCancelled();
        else if (t.IsFaulted)
          res.Set(t.Exception?.Flatten().GetBaseException());
        else
          res.Set(Unit.Instance);
      }, CancellationToken.None, TaskContinuationOptions.ExecuteSynchronously, TaskScheduler.Current);
      return res;
    }


    [PublicAPI] public static void Set<TReq, TRes>(this IRdEndpoint<TReq, TRes> endpoint, Func<Lifetime, TReq, Task<TRes>> handler, IScheduler cancellationAndRequestScheduler = null)
    {
      endpoint.Set((lt, req) => handler(lt, req).ToRdTask(), cancellationAndRequestScheduler);
    }
    

    
    [PublicAPI] public static void Set<TReq, TRes>(this IRdEndpoint<TReq, TRes> endpoint, Func<TReq, TRes> handler, IScheduler cancellationAndRequestScheduler = null)
    {
      endpoint.Set((_, req) => RdTask<TRes>.Successful(handler(req)), cancellationAndRequestScheduler);
    }
    
    [PublicAPI] public static void SetVoid<TReq>(this IRdEndpoint<TReq, Unit> endpoint, Action<TReq> handler, IScheduler cancellationAndRequestScheduler = null)
    {
      endpoint.Set(req =>
      {
        handler(req);
        return Unit.Instance;
      }, cancellationAndRequestScheduler);
    }
    
    [PublicAPI]
    public static Task<T> AsTask<T>([NotNull] this IRdTask<T> task)
    {
      if (task == null) throw new ArgumentNullException(nameof(task));
      var tcs = new TaskCompletionSource<T>();
      task.Result.AdviseOnce(Lifetime.Eternal, result =>
      {
        switch (result.Status)
        {
          case RdTaskStatus.Success:
            tcs.SetResult(result.Result);
            break;
          case RdTaskStatus.Canceled:
            tcs.SetCanceled();
            break;
          case RdTaskStatus.Faulted:
            tcs.SetException(result.Error);
            break;
          default:
            throw new ArgumentOutOfRangeException(result.Status.ToString());
        }
      });
      return tcs.Task;
    }   
  }
}