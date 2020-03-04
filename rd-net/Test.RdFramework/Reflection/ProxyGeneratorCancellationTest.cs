using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;
using JetBrains.Threading;
using NUnit.Framework;

#if NET35
    private static TaskHack Task = new TaskHack();
#endif

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ProxyGeneratorCancellationTest : RdReflectionTestBase
  {
    [RdRpc]
    public interface IAsyncCallsTest
    {
      Task<string> GetLongRunningString(Lifetime cancellationLifetime, string result);
      Task<int> GetLongRunningInt(int arg, Lifetime cancellationLifetime );
      Task<int> GetInt(int arg, Lifetime cancellationLifetime );
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

      public async Task<int> GetInt(int arg, Lifetime cancellationLifetime) => 1;
    }

    [Test]
    public void TestAsyncCancel()
    {
      bool? isCancelled = null;
      TestAsyncCalls(model =>
      {
        var cancellationLifetimeDef = new LifetimeDefinition();
        model.GetLongRunningInt(100, cancellationLifetimeDef.Lifetime).
          ContinueWith(t => isCancelled = t.IsCanceled, TaskContinuationOptions.ExecuteSynchronously);

        Task.Run(async () => { await Task.Delay(10); cancellationLifetimeDef.Terminate();});
        cancellationLifetimeDef.Terminate();
      });

      SpinWaitEx.SpinUntil(TimeSpan.FromSeconds(1), () => isCancelled != null);
      Assert.AreEqual(true, isCancelled);
    }

    [Test]
    public void TestAsync()
    {
      bool? isCancelled = null;
      TestAsyncCalls(model =>
      {
        var cancellationLifetimeDef = new LifetimeDefinition();
        model.GetInt(100, cancellationLifetimeDef.Lifetime).
          ContinueWith(t => isCancelled = t.IsCanceled, TaskContinuationOptions.ExecuteSynchronously);
      });

      SpinWaitEx.SpinUntil(TimeSpan.FromSeconds(1), () => isCancelled != null);
      Assert.AreEqual(false, isCancelled);
    }

    private void TestAsyncCalls(Action<IAsyncCallsTest> run) => TestTemplate<AsyncCallsTest, IAsyncCallsTest>(run);

    protected void TestTemplate<TImpl, TInterface>(Action<TInterface> runTest) where TImpl : RdBindableBase where TInterface : class
    {
      ClientProtocol.Scheduler.Queue(() =>
      {
        var client = CFacade.Activator.ActivateBind<TImpl>(TestLifetime, ClientProtocol);
      });

      TInterface proxy = null;
      ServerProtocol.Scheduler.Queue(() => { proxy = SFacade.ActivateProxy<TInterface>(TestLifetime, ServerProtocol); });

      WaitMessages();

      using (var barrier = new ManualResetEvent(false))
      {
        ServerProtocol.Scheduler.Queue(() =>
          Assertion.Assert((proxy as RdExtReflectionBindableBase).NotNull().Connected.Value, "((RdReflectionBindableBase)proxy).Connected.Value"));
        runTest(proxy);
        ServerProtocol.Scheduler.Queue(() => barrier.Set());

        WaitMessages();
        barrier.WaitOne();
      }
    }

    private void WaitMessages()
    {
      bool IsIdle(IRdDynamic p) => ((SingleThreadScheduler) p.Proto.Scheduler).IsIdle;
      SpinWaitEx.SpinUntil(() => IsIdle(ServerProtocol) && IsIdle(ClientProtocol));
    }

    protected override IScheduler CreateScheduler(bool isServer)
    {
      string name = (isServer ? "server" : "client") + " scheduler";
      IScheduler result = null;

      var thread = new Thread(() => SingleThreadScheduler.RunInCurrentStackframe(TestLifetime, name, s => result = s)) { Name = name };
      thread.Start();
      SpinWaitEx.SpinUntil(() => result != null);
      return result;
    }
  }

}