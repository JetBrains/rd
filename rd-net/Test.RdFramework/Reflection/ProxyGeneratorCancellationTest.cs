using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Lifetimes;
using JetBrains.Rd.Reflection;
using JetBrains.Threading;
using NUnit.Framework;


namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ProxyGeneratorCancellationTest : ProxyGeneratorTestBase
  {
#if NET35
    private static TaskHack Task = new TaskHack();
#endif

    protected override bool IsAsync => true;

    [RdRpc]
    public interface IAsyncCallsTest
    {
      Task<string> GetLongRunningString(Lifetime cancellationLifetime, string result);
      Task<int> GetLongRunningInt(int arg, Lifetime cancellationLifetime );
      Task<int> GetInt(int arg, Lifetime cancellationLifetime );
      Task AlwaysCancelled();
    }

    [RdExt]
    internal class AsyncCallsTest : RdExtReflectionBindableBase, IAsyncCallsTest
    {
      public async Task<string> GetLongRunningString(Lifetime cancellationLifetime, string result)
      {
        await Task.Delay(1000, cancellationLifetime);
        return result;
      }

      public async Task<int> GetLongRunningInt(int arg, Lifetime cancellationLifetime)
      {
        await Task.Delay(1000, cancellationLifetime);
        return arg;
      }

      public Task<int> GetInt(int arg, Lifetime cancellationLifetime) => Task.FromResult(1);

      public Task AlwaysCancelled()
      {
        return Task.Run(() =>
        {
          Thread.Sleep(100);
          throw new OperationCanceledException();
        });
      }
    }

    [Test]
    public async Task TestAsyncCancel()
    {
      bool? isCancelled = null;
      await TestAsyncCalls(model =>
      {
        var cancellationLifetimeDef = new LifetimeDefinition();
        model.GetLongRunningInt(100, cancellationLifetimeDef.Lifetime).
          ContinueWith(t => isCancelled = t.IsCanceled, TaskContinuationOptions.ExecuteSynchronously);

        Task.Run(async () => { await Task.Delay(10); cancellationLifetimeDef.Terminate();});
        cancellationLifetimeDef.Terminate();
        return Task.CompletedTask;
      });

      SpinWaitEx.SpinUntil(TimeSpan.FromSeconds(1), () => isCancelled != null);
      Assert.AreEqual(true, isCancelled);
    }

    [Test]
    public async Task TestAsyncExternalCancellation()
    {
      bool? isCancelled = null;
      await TestAsyncCalls(model =>
      {
        model.AlwaysCancelled().
          ContinueWith(t => isCancelled = t.IsCanceled, TaskContinuationOptions.ExecuteSynchronously);
        return Task.CompletedTask;
      });

      SpinWaitEx.SpinUntil(TimeSpan.FromSeconds(1), () => isCancelled != null);
      Assert.AreEqual(true, isCancelled);
    }

    [Test]
    public async Task TestAsync()
    {
      bool? isCancelled = null;
      var testAsyncCalls = TestAsyncCalls(model =>
      {
        var cancellationLifetimeDef = new LifetimeDefinition();
        model.GetInt(100, cancellationLifetimeDef.Lifetime).
          ContinueWith(t => isCancelled = t.IsCanceled, TaskContinuationOptions.ExecuteSynchronously);
        return Task.CompletedTask;
      });

      SpinWaitEx.SpinUntil(TimeSpan.FromSeconds(1), () => isCancelled != null);
      await testAsyncCalls;
      Assert.AreEqual(false, isCancelled);
    }

    private async Task TestAsyncCalls(Func<IAsyncCallsTest, Task> run) => await TestTemplate<AsyncCallsTest, IAsyncCallsTest>(run);
  }
}