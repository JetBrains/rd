using System;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Reflection;
using JetBrains.Threading;

namespace Test.RdFramework.Reflection
{
  public class ProxyGeneratorTestBase : RdReflectionTestBase
  {
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
        SaveGeneratedAssembly();
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
  }
}