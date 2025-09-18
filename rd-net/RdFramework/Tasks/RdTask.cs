using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Util;




namespace JetBrains.Rd.Tasks
{
  public class RdTask<T> : IRdTask<T>
  {

    internal readonly WriteOnceProperty<RdTaskResult<T>> ResultInternal = new WriteOnceProperty<RdTaskResult<T>>();

    public IReadonlyProperty<RdTaskResult<T>> Result => ResultInternal;

    public void Set(T value) => ResultInternal.SetIfEmpty(RdTaskResult<T>.Success(value));
    public void SetCancelled() => ResultInternal.SetIfEmpty(RdTaskResult<T>.Cancelled());
    public void Set(Exception e) => ResultInternal.SetIfEmpty(RdTaskResult<T>.Faulted(e));

    [Obsolete("Use 'RdTask.Successful<T>(T)' instead")]
    public static RdTask<T> Successful(T result) => RdTask.Successful(result);
    [Obsolete("Use 'RdTask.Faulted<T>(Exception)' instead")]
    public static RdTask<T> Faulted(Exception exception) => RdTask.Faulted<T>(exception);
    [Obsolete("Use 'RdTask.Cancelled<T>()' instead")]
    public static RdTask<T> Cancelled() => RdTask.Cancelled<T>();

    [PublicAPI] public static implicit operator Task<T>(RdTask<T> task) => task.AsTask();
  }

  public static class RdTask
  {
    [PublicAPI] public static RdTask<T> Successful<T>(T result) => FromResult(RdTaskResult<T>.Success(result));
    [PublicAPI] public static RdTask<T> Faulted<T>(Exception exception) => FromResult(RdTaskResult<T>.Faulted(exception));
    [PublicAPI] public static RdTask<T> Cancelled<T>() => FromResult(RdTaskResult<T>.Cancelled());
    private static RdTask<T> FromResult<T>(RdTaskResult<T> result) =>
      new() { ResultInternal = { Value = result } };
  }
}