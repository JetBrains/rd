using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ProxyGeneratorAsyncCallsTest : ProxyGeneratorTestBase
  {
    [RdRpc]
    public interface IAsyncCallsTest
    {
      Task<string> GetStringAsync();
      Task RunSomething();
    }

    [RdExt]
    public class AsyncCallsTest : RdReflectionBindableBase, IAsyncCallsTest
    {
      public Task<string> GetStringAsync()
      {
        return Task.FromResult("result");
      }

      public Task RunSomething()
      {
        return Task.CompletedTask;
      }
    }

    protected override IScheduler CreateScheduler(bool isServer)
    {
      string name = (isServer ? "server" : "client") + " scheduler";
      IScheduler result = null;

      var thread = new Thread(() => SingleThreadScheduler.RunInCurrentStackframe(TestLifetime, name, s => result = s)) { Name = name };
      thread.SetApartmentState(ApartmentState.STA);
      thread.Start();
      SpinWait.SpinUntil(() => result != null);
      return result;
    }

    /*    public class ProxyX : IAsyncCallsTest
    {
      private IRdCall<Unit, string> myRdCall;

      public Task<string> GetStringAsync()
      {
        return ProxyGeneratorUtil.ToTask(myRdCall.Start(Unit.Instance, null));
      }
    }*/

    [Test]
    public void TestAsync()
    {
      string result = null;
      TestTemplate(model =>
      {
        Assertion.Assert(((RdReflectionBindableBase) model).Connected.Value, "((RdReflectionBindableBase)proxy).Connected.Value");
        model.GetStringAsync().ContinueWith(t => result = t.Result, TaskContinuationOptions.ExecuteSynchronously);
      });
      Assert.AreEqual(result, "result");
    }


    [Test]
    public void TestAsyncVoid()
    {
      // todo: really check long running task result
      TestTemplate(model => { model.RunSomething(); });
    }

    private void TestTemplate(Action<IAsyncCallsTest> runTest)
    {
      ClientProtocol.Scheduler.Queue(() =>
      {
        var client = ReflectionRdActivator.ActivateBind<AsyncCallsTest>(TestLifetime, ClientProtocol);
      });

      IAsyncCallsTest proxy = null;
      ServerProtocol.Scheduler.Queue(() => { proxy = CreateServerProxy<IAsyncCallsTest>(); });

      WaitMessages();

      using var barrier = new ManualResetEvent(false);
      ServerProtocol.Scheduler.Queue(() => runTest(proxy));
      ServerProtocol.Scheduler.Queue(() => barrier.Set());

      WaitMessages();
      barrier.WaitOne();
    }

    private void WaitMessages()
    {
      bool IsIdle(IRdDynamic p) => ((SingleThreadScheduler) p.Proto.Scheduler).IsIdle;
      SpinWait.SpinUntil(() => IsIdle(ServerProtocol) && IsIdle(ClientProtocol));
    }
  }
}