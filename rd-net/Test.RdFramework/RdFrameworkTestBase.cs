using System;
using System.IO;
using System.Threading;
using JetBrains.Annotations;
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
    
    [CanBeNull] private TestWireTapping WireTapping;

    [SetUp]
    public override void SetUp()
    {
      base.SetUp();
      
      var identities = new SequentialIdentities(IdKind.Server);

      var serverDispatcher = CreateScheduler(true);
      var clientDispatcher = CreateScheduler(false);
      
      var serverR = "Server (R#)";
      ServerWire = new TestWire(TryCreateWireScheduler(true) ?? serverDispatcher, serverR, true);
      ServerProtocol = new Protocol(serverR, CreateSerializers(true), identities, serverDispatcher, ServerWire, LifetimeDefinition.Lifetime);
      
      var clientIdea = "Client (IDEA)";
      ClientWire = new TestWire(TryCreateWireScheduler(false) ?? clientDispatcher, clientIdea, false);
      ClientProtocol = new Protocol(clientIdea, CreateSerializers(false), identities, clientDispatcher, ClientWire, LifetimeDefinition.Lifetime);

      // EnableWireTapping();

      ServerWire.Connection = ClientWire;
      ClientWire.Connection = ServerWire;
    }

    /// <summary>
    /// Record all communication over the wire and write them to temporary directory
    /// </summary>
    private void EnableWireTapping()
    {
      var filename = Path.Combine(Path.Combine(Path.GetTempPath(), "RdTestWireTapping"), TestContext.CurrentContext.Test.FullName + ".txt");
      var directoryName = Path.GetDirectoryName(filename);
      if (directoryName != null && !Directory.Exists(directoryName))
        Directory.CreateDirectory(directoryName);
      WireTapping = new TestWireTapping(filename, ClientWire, ServerWire);
    }

    protected virtual IScheduler CreateScheduler(bool isServer)
    {
      var dispatcher = SynchronousScheduler.Instance;
      dispatcher.SetActive(LifetimeDefinition.Lifetime);
      return dispatcher;
    }
    
    [CanBeNull]
    protected virtual IScheduler TryCreateWireScheduler(bool isServer)
    {
      return null;
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
      if (!SpinWait.SpinUntil(() => barrier == 2, 500))
        Log.Root.Error("Either Server or Client scheduler is not empty in 100ms!");
      WireTapping?.Dispose();
      base.TearDown();
    }

    protected T BindToClient<T>(Lifetime lf, T x, int staticId) where T : IRdReactive
    {
      using var _ = AllowBindCookie.Create();
      var reactive = x.Static(staticId);
      reactive.PreBind(lf, ClientProtocol, "client");
      reactive.Bind();
      return x;
    }

    protected T BindToServer<T>(Lifetime lf, T x, int staticId) where T : IRdReactive
    {
      using var _ = AllowBindCookie.Create();
      var reactive = x.Static(staticId);
      reactive.PreBind(lf, ServerProtocol, "server");
      reactive.Bind();
      return x;
    }
  }
}