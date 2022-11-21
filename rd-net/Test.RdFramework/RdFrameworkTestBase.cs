using System;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using NUnit.Framework;
using Test.Lifetimes;
using Test.RdFramework.Components;

namespace Test.RdFramework
{ 
  public abstract class RdFrameworkTestBase : LifetimesTestBase
  {
    protected IProtocol ClientProtocol;
    protected IProtocol ServerProtocol;

    protected TestWire ClientWire;
    protected TestWire ServerWire;

    [SetUp]
    public override void SetUp()
    {
      base.SetUp();
      
      var identities = new Identities(IdKind.Server);

      var serverDispatcher = CreateScheduler(true);
      var clientDispatcher = CreateScheduler(false);
      var serverR = "Server (R#)";
      ServerProtocol = new Protocol(serverR, CreateSerializers(true), identities, serverDispatcher, new TestWire(serverDispatcher, serverR, true), LifetimeDefinition.Lifetime);
      var clientIdea = "Client (IDEA)";
      ClientProtocol = new Protocol(clientIdea, CreateSerializers(false), identities, clientDispatcher, new TestWire(clientDispatcher, clientIdea, false), LifetimeDefinition.Lifetime);

      ServerWire = (ServerProtocol.Wire as TestWire).NotNull();
      ClientWire = (ClientProtocol.Wire as TestWire).NotNull();

      ServerWire.Connection = ClientWire;
      ClientWire.Connection = ServerWire;
    }

    protected virtual IScheduler CreateScheduler(bool isServer)
    {
      var dispatcher = SynchronousScheduler.Instance;
      dispatcher.SetActive(LifetimeDefinition.Lifetime);
      return dispatcher;
    }

    protected virtual ISerializers CreateSerializers(bool isServer)
    {
      return new Serializers();
    }

    public override void TearDown()
    {
      if (ServerWire.HasMessages)
        throw new InvalidOperationException("There is messages in ServerWire");
      if (ClientWire.HasMessages)
        throw new InvalidOperationException("There is messages in ClientWire");

      int barrier = 0;
      ServerProtocol.Scheduler.InvokeOrQueue(() => Interlocked.Increment(ref barrier));
      ClientProtocol.Scheduler.InvokeOrQueue(() => Interlocked.Increment(ref barrier));
      if (!SpinWait.SpinUntil(() => barrier == 2, 100))
        Log.Root.Error("Either Server or Client scheduler is not empty in 100ms!");
      base.TearDown();
    }

    protected T BindToClient<T>(Lifetime lf, T x, int staticId) where T : IRdReactive
    {
      x.Static(staticId).Bind(lf, ClientProtocol, "client");
      return x;
    }

    protected T BindToServer<T>(Lifetime lf, T x, int staticId) where T : IRdReactive
    {
      x.Static(staticId).Bind(lf, ServerProtocol, "server");
      return x;
    }
  }
}