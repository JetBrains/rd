using System;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Lifetimes;
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
  }
}