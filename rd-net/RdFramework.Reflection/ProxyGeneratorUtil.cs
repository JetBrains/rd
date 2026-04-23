using System;
using System.Diagnostics;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;

namespace JetBrains.Rd.Reflection
{
  /// <summary>
  /// Helpers method which used by generated proxies.
  /// </summary>
  [UsedImplicitly(ImplicitUseTargetFlags.Members)]
  public static class ProxyGeneratorUtil
  {
    public static Task<T> ToTask<T>(IRdTask<T> task)
    {
      var tcs = new TaskCompletionSource<T>();
      task.Result.Advise(Lifetime.Eternal, r =>
      {
        switch (r.Status)
        {
          case RdTaskStatus.Success:
            tcs.SetResult(r.Result);
            break;
          case RdTaskStatus.Canceled:
            tcs.SetCanceled();
            break;
          case RdTaskStatus.Faulted:
            tcs.SetException(r.Error);
            break;
          default:
            throw new ArgumentOutOfRangeException();
        }
      });

      return tcs.Task;
    }


    /// <summary>
    /// Sync call which allow nested call execution with help of <see cref="IRunWhileScheduler"/>
    /// </summary>
    public static TRes SyncNested<TReq, TRes>(RdCall<TReq, TRes> call, TReq request, RpcTimeouts? timeouts = null)
    {
      return SyncNested(call, Lifetime.Eternal, request, timeouts);
    }

    /// <summary>
    /// Sync call which allow nested call execution with help of <see cref="IRunWhileScheduler"/>
    /// </summary>
    public static TRes SyncNested<TReq, TRes>(RdCall<TReq, TRes> call, Lifetime lifetime, TReq request, RpcTimeouts? timeouts = null)
    {
      Assertion.Require(call.IsBound, "Not bound: {0}", call);

      // Sync calls can called only under the protocol's scheduler.
      // If you want to mitigate this limitation, keep in mind that if you make a sync call from background thread
      // with some small probability your call can be merged in the sync execution of other call. Usually it is not
      // desired behaviour as you can accidentally obtain undesired locks.
      var protocol = call.GetProtoOrThrow();
      protocol.Scheduler.AssertThread();

      var scheduler = protocol.Scheduler as IRunWhileScheduler;
      Assertion.Assert(scheduler != null, "Scheduler must implement IRunWhileScheduler for nested calls. Scheduler type: {0}", protocol.Scheduler.GetType());

      var task = (RdTask<TRes>)call.Start(lifetime, request, null);

      RpcTimeouts timeoutsToUse = RpcTimeouts.GetRpcTimeouts(timeouts);

      var stopwatch = Stopwatch.StartNew();

      var completed = scheduler.RunWhile(() => !task.Result.HasValue(), timeoutsToUse.ErrorAwaitTime);

      stopwatch.Stop();

      if (!completed)
      {
        task.SetCancelled();
        throw new TimeoutException($"Sync execution of rpc `{call.Location}` is timed out in {timeoutsToUse.ErrorAwaitTime.TotalMilliseconds} ms");
      }

      var freezeTime = stopwatch.ElapsedMilliseconds;
      if (freezeTime > timeoutsToUse.WarnAwaitTime.TotalMilliseconds)
        Log.Root.Warn("Sync execution of rpc `{0}` executed too long: {1} ms", call.Location, freezeTime);

      return task.Result.Value.Unwrap();
    }

    public static RpcTimeouts CreateRpcTimeouts(long ticksWarning, long ticksError)
    {
      return new RpcTimeouts(new TimeSpan(ticksWarning), new TimeSpan(ticksError));
    }
  }
}