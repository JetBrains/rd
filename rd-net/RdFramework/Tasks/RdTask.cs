using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Util;

#if !NET35

#endif


namespace JetBrains.Rd.Tasks
{
  public class RdTask<T> : IRdTask<T>
  {
    
    internal readonly WriteOnceProperty<RdTaskResult<T>> ResultInternal = new WriteOnceProperty<RdTaskResult<T>>();

    public IReadonlyProperty<RdTaskResult<T>> Result => ResultInternal;

    public void Set(T value) => ResultInternal.SetIfEmpty(RdTaskResult<T>.Success(value));
    public void SetCancelled() => ResultInternal.SetIfEmpty(RdTaskResult<T>.Cancelled());
    public void Set(Exception e) => ResultInternal.SetIfEmpty(RdTaskResult<T>.Faulted(e));

    private static RdTask<T> FromResult(RdTaskResult<T> result)
    {
      var res = new RdTask<T>();
      res.ResultInternal.Value = result;
      return res;
    }

    public static RdTask<T> Successful(T result) => FromResult(RdTaskResult<T>.Success(result));
    public static RdTask<T> Faulted(Exception exception) => FromResult(RdTaskResult<T>.Faulted(exception));

#if !NET35
    [PublicAPI] public static implicit operator Task<T>(RdTask<T> task) => task.AsTask();
#endif
  }
}