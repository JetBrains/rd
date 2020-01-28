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

    public Task CompletedTask { get; }

    // only for test purposes
    public Task Delay(int ms, CancellationToken token)
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
