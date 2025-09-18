using System;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Reflection;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  public class ProxyGeneratorTestBase : RdReflectionTestBase
  {

    protected RdBindableBase myClient;
    protected object myProxy;

    protected virtual bool IsAsync => false;

    protected async Task TestTemplate<TImpl, TInterface>(Func<TInterface, Task> runTest) where TImpl : RdBindableBase where TInterface : class
    {
      await YieldToClient();
      myClient = CFacade.Activator.ActivateBind<TImpl>(TestLifetime, ClientProtocol);

      await YieldToServer();
      var proxy = SFacade.ActivateProxy<TInterface>(TestLifetime, ServerProtocol);
      myProxy = proxy;

      CollectionAssert.AreEquivalent(
        ((IReflectionBindable)myClient).BindableChildren.Select(m => m.Key),
        ((IReflectionBindable)myProxy).BindableChildren.Select(m => m.Key)
      );

      await Wait();

      SaveGeneratedAssembly();
      
      await YieldToServer();
      Assertion.Assert((myProxy as RdExtReflectionBindableBase).NotNull().Connected.Value,
        "((RdReflectionBindableBase)proxy).Connected.Value");

      await runTest(proxy);
      
      await Wait();
    }

    protected async Task Wait()
    {
      await YieldToClient();
      await YieldToServer();
      await Task.Run(() => { });
    }

    protected SchedulerAwaitable YieldToClient() => new SchedulerAwaitable(ClientProtocol.Scheduler);
    protected SchedulerAwaitable YieldToServer() => new SchedulerAwaitable(ServerProtocol.Scheduler);

    protected override IScheduler CreateScheduler(bool isServer)
    {
      if (!IsAsync)
        return base.CreateScheduler(isServer);

      string name = (isServer ? "server" : "client") + " scheduler";
      IScheduler result = null;

      var thread = new Thread(() => SingleThreadScheduler.RunInCurrentStackframe(TestLifetime, name, s => result = s)) { Name = name };
      thread.Start();
      SpinWaitEx.SpinUntil(() => result != null);
      return result;
    }
  }

  public class SchedulerAwaitable : INotifyCompletion
  {
    private readonly IScheduler myScheduler;

    public bool IsCompleted => false;
    public void GetResult() { }
    public SchedulerAwaitable(IScheduler scheduler) => myScheduler = scheduler;
    public SchedulerAwaitable GetAwaiter() => this;
    public void OnCompleted(Action continuation) => myScheduler.Queue(continuation);
  }
}