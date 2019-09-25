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
    }

    [RdExt]
    public class AsyncCallsTest : RdReflectionBindableBase, IAsyncCallsTest
    {
      public Task<string> GetStringAsync()
      {
        return Task.FromResult("result");
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
      ClientProtocol.Scheduler.Queue(() =>
      {
        var client = ReflectionRdActivator.ActivateBind<AsyncCallsTest>(TestLifetime, ClientProtocol);
      });

      IAsyncCallsTest proxy = null;
      ServerProtocol.Scheduler.Queue(() => { proxy = CreateServerProxy<IAsyncCallsTest>(); });

      WaitMessages();

      string proxyResult = null;
      ServerProtocol.Scheduler.Queue(() =>
      {
        Assertion.Assert(((RdReflectionBindableBase)proxy).Connected.Value, "((RdReflectionBindableBase)proxy).Connected.Value");
        proxy.GetStringAsync().ContinueWith(t => proxyResult = t.Result, TaskContinuationOptions.ExecuteSynchronously);
      });

      WaitMessages();
      SpinWait.SpinUntil(() => proxyResult != null);
      Assert.AreEqual(proxyResult, "result");
    }

    private void WaitMessages()
    {
      bool IsIdle(IRdDynamic p) => ((SingleThreadScheduler) p.Proto.Scheduler).IsIdle;
      SpinWait.SpinUntil(() => IsIdle(ServerProtocol) && IsIdle(ClientProtocol));
    }
  }
}