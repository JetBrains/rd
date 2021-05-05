using System;
using System.Diagnostics;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
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
    /// Sync call which allow nested call execution with help of <see cref="SwitchingScheduler"/>
    /// </summary>
    public static TRes SyncNested<TReq, TRes>(RdCall<TReq, TRes> call, TReq request, RpcTimeouts timeouts = null)
    {
      return SyncNested(call, Lifetime.Eternal, request, timeouts);
    }

    /// <summary>
    /// Sync call which allow nested call execution with help of <see cref="SwitchingScheduler"/>
    /// </summary>
    public static TRes SyncNested<TReq, TRes>(RdCall<TReq, TRes> call, Lifetime lifetime, TReq request, RpcTimeouts timeouts = null)
    {
      Assertion.Require(call.IsBound, "Not bound: {0}", call);

      // Sync calls can called only under the protocol's scheduler.
      // If you want to mitigate this limitation, keep in mind that if you make a sync call from background thread
      // with some small probability your call can be merged in the sync execution of other call. Usually it is not
      // desired behaviour as you can accidentally obtain undesired locks.
      call.Proto.Scheduler.AssertThread();

      var nestedCallsScheduler = new LifetimeDefinition();
      var responseScheduler = new RdSimpleDispatcher(nestedCallsScheduler.Lifetime, Log.GetLog(call.GetType()));

      using (new SwitchingScheduler.SwitchCookie(responseScheduler))
      {
        var task = call.Start(lifetime, request, responseScheduler);

        task.Result.Advise(nestedCallsScheduler.Lifetime, result => { nestedCallsScheduler.Terminate(); });

        RpcTimeouts timeoutsToUse = RpcTimeouts.GetRpcTimeouts(timeouts);
        responseScheduler.MessageTimeout = timeoutsToUse.ErrorAwaitTime;

        var stopwatch = Stopwatch.StartNew();
        responseScheduler.Run();
        if (!task.Result.HasValue())
        {
          throw new TimeoutException($"Sync execution of rpc `{call.Location}` is timed out in {timeoutsToUse.ErrorAwaitTime.TotalMilliseconds} ms");
        }

        stopwatch.Stop();

        var freezeTime = stopwatch.ElapsedMilliseconds;
        if (freezeTime > timeoutsToUse.WarnAwaitTime.TotalMilliseconds)
        {
          Log.Root.Error("Sync execution of rpc `{0}` executed too long: {1} ms", call.Location, freezeTime);
        }

        return task.Result.Value.Unwrap();
      }
    }
  }
}