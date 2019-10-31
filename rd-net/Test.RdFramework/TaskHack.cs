using System.Threading;
using System.Threading.Tasks;

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
        SpinWait.SpinUntil(() => token.IsCancellationRequested, ms);
        token.ThrowIfCancellationRequested();
      }, token);
    }
  }
}
#endif
