using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Lifetimes;
using JetBrains.Threading;

#if NET35
namespace Test.RdFramework
{
  public class TaskHack
  {
    public TaskHack()
    {
      CompletedTask = Task.Factory.StartNew(() => { });
    }

    public Task<T> FromResult<T>(T result)
    {
      return Task.Factory.StartNew(() => result);
    }

    public Task Run(Action action) => Task.Factory.StartNew(action);
    public Task<TResult> Run<TResult>(Func<TResult> function) => Task.Factory.StartNew(function);
    public Task Run(Func<Task?> function) => Task.Factory.StartNew(function);
    // public Task<TResult> Run<TResult>(Func<Task<TResult>?> function) => Task.Factory.StartNew<Task<TResult>>(function, default);

//     public Task<TResult> Run<TResult>(Func<TResult> function) => Task.Factory.StartNew(function);
//     public Task Run(Func<Task> function) => Task.Factory.StartNew(function);
//     public Task Run(Action action) => Task.Factory.StartNew(action);

    public Task CompletedTask { get; }

    // only for test purposes
    public Task Delay(int ms, CancellationToken token = default)
    {
      return Task.Factory.StartNew(() =>
      {
        SpinWaitEx.SpinUntil(Lifetime.Eternal, TimeSpan.FromMilliseconds(ms), () => token.IsCancellationRequested);
        token.ThrowIfCancellationRequested();
      }, token);
    }
  }
}
#endif
