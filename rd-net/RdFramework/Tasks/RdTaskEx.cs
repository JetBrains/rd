using System;
using System.Runtime.CompilerServices;
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

    public static bool Wait<T>(this IRdTask<T> task, TimeSpan timeout)
    {
      if (task.Result.HasValue())
        return true;

      var spinWait = new SpinWait();
      do
      {
        spinWait.SpinOnce();
        
        if (task.Result.HasValue())
          return true;
      } while (!spinWait.NextSpinWillYield); // to avoid Thread.Sleep(1)
      
      Thread.Sleep(0);
      
      if (task.Result.HasValue())
        return true;

      WaitSlow(task, timeout);
      return task.Result.HasValue();
    }

    private static void WaitSlow<T>(IRdTask<T> task, TimeSpan timeout)
    {
      // Do not use the slim version, as it will spin and may cause Thread.Sleep(1) in the .NET Framework, which may result in a 16 ms wait, and besides, we have already covered this above.
      var mre = new ManualResetEvent(false);
      using var def = Lifetime.Define();
      task.Result.AdviseOnce(def.Lifetime, _ => mre.Set());
      mre.WaitOne(millisecondsTimeout: timeout.TotalMilliseconds >= int.MaxValue ? Timeout.Infinite : (int)timeout.TotalMilliseconds);
    }


    public static RdTask<T> ToRdTask<T>(this Task<T> task)
    {
      var res = new RdTask<T>();
      task.ContinueWith(t =>
      {
        if (t.IsOperationCanceled())
          res.SetCancelled();
        else if (t.IsFaulted)
          res.Set(t.Exception?.Flatten().GetBaseException() ?? new Exception("Unknown exception"));
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
          res.Set(t.Exception?.Flatten().GetBaseException() ?? new Exception("Unknown exception"));
        else
          res.Set(Unit.Instance);
      }, CancellationToken.None, TaskContinuationOptions.ExecuteSynchronously, TaskScheduler.Current);
      return res;
    }


    [PublicAPI] public static void Set<TReq, TRes>(this IRdEndpoint<TReq, TRes> endpoint, Func<Lifetime, TReq, Task<TRes>> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null)
    {
      endpoint.SetRdTask((lt, req) => handler(lt, req).ToRdTask(), cancellationScheduler, handlerScheduler);
    }
    
    [PublicAPI] public static void SetAsync<TReq, TRes>(this IRdEndpoint<TReq, TRes> endpoint, Func<Lifetime, TReq, Task<TRes>> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null)
    {
      endpoint.Set(handler, cancellationScheduler, handlerScheduler);
    }
    
    [PublicAPI] public static void SetVoidAsync<TReq>(this IRdEndpoint<TReq, Unit> endpoint, Func<Lifetime, TReq, Task> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null)
    {
      endpoint.SetAsync(async (lt, x) =>
      {
        await handler(lt, x);
        return Unit.Instance;
      }, cancellationScheduler, handlerScheduler);
    }

    [PublicAPI] public static void Set<TReq, TRes>(this IRdEndpoint<TReq, TRes> endpoint, Func<Lifetime, TReq, TRes> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null)
    {
      endpoint.SetRdTask((lifetime, req) => RdTask.Successful(handler(lifetime, req)), cancellationScheduler, handlerScheduler);
    }
    
    [PublicAPI] public static void SetSync<TReq, TRes>(this IRdEndpoint<TReq, TRes> endpoint, Func<Lifetime, TReq, TRes> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null)
    {
      endpoint.Set(handler, cancellationScheduler, handlerScheduler);
    }

    [PublicAPI] public static void Set<TReq, TRes>(this IRdEndpoint<TReq, TRes> endpoint, Func<TReq, TRes> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null)
    {
      endpoint.SetRdTask((_, req) => RdTask.Successful(handler(req)), cancellationScheduler, handlerScheduler);
    }
    
    [PublicAPI] public static void SetSync<TReq, TRes>(this IRdEndpoint<TReq, TRes> endpoint, Func<TReq, TRes> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null)
    {
      endpoint.Set(handler, cancellationScheduler, handlerScheduler);
    }

    [PublicAPI] public static void SetVoid<TReq>(this IRdEndpoint<TReq, Unit> endpoint, Action<TReq> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null)
    {
      endpoint.Set(req =>
      {
        handler(req);
        return Unit.Instance;
      }, cancellationScheduler, handlerScheduler);
    }

    [PublicAPI]
    public static Task<T> AsTask<T>(this IRdTask<T> task)
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

    [PublicAPI]
    public static TaskAwaiter<T> GetAwaiter<T>(this IRdTask<T> task) => task.AsTask().GetAwaiter();
  }
}