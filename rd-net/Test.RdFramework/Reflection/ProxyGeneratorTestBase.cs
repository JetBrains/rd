using System;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Reflection;
using JetBrains.Threading;

namespace Test.RdFramework.Reflection
{
  public class ProxyGeneratorTestBase : RdReflectionTestBase
  {
    protected virtual bool IsAsync => false;

    protected async Task TestTemplate<TImpl, TInterface>(Action<TInterface> runTest) where TImpl : RdBindableBase where TInterface : class
    {
      await YieldToClient();
      var client = CFacade.Activator.ActivateBind<TImpl>(TestLifetime, ClientProtocol);

      await YieldToServer();
      var proxy = SFacade.ActivateProxy<TInterface>(TestLifetime, ServerProtocol);

      await Wait();

      SaveGeneratedAssembly();
      
      await YieldToServer();
      Assertion.Assert((proxy as RdExtReflectionBindableBase).NotNull().Connected.Value,
        "((RdReflectionBindableBase)proxy).Connected.Value");

      await Task.Run(() => runTest(proxy));

      await Wait();
    }

    private Task Wait()
    {
      bool IsIdle(IRdDynamic p) => ((SingleThreadScheduler) p.Proto.Scheduler).IsIdle;
      return Task.Run(() => SpinWaitEx.SpinUntil(() => IsIdle(ServerProtocol) && IsIdle(ClientProtocol)));
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