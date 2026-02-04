using System;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using NUnit.Framework;
using Test.Lifetimes;
using Test.RdFramework.Components;

namespace Test.RdFramework.Contexts
{
  /// <summary>
  /// This test replicates a case that happens often in real world: one end is created,
  /// and starts sending messages way before the other end is created/ready to receive messages
  /// </summary>
  public class RdContextEarlyDeliveryTest : LifetimesTestBase
  {
    [Datapoint]
    public static bool TrueDataPoint = true;
    [Datapoint]
    public static bool FalseDataPoint = false;

    private IProtocol myClientProtocol;
    private IProtocol myServerProtocol;

    private TestWire myClientWire;
    private TestWire myServerWire;

    private IScheduler CreateScheduler()
    {
      var dispatcher = SynchronousScheduler.Instance;
      dispatcher.SetActive(LifetimeDefinition.Lifetime);
      return dispatcher;
    }

    private Serializers CreateSerializers()
    {
      return new Serializers();
    }

    public override void TearDown()
    {
      if (myServerWire.HasMessages)
        throw new InvalidOperationException("There is messages in ServerWire");
      if (myClientWire.HasMessages)
        throw new InvalidOperationException("There is messages in ClientWire");
      
      base.TearDown();
    }

    private T BindToClient<T>(Lifetime lf, T x, int staticId) where T : IRdReactive
    {
      var reactive = x.Static(staticId);
      reactive.BindTopLevel(lf, myClientProtocol, "client");
      return x;
    }

    private T BindToServer<T>(Lifetime lf, T x, int staticId) where T : IRdReactive
    {
      var reactive = x.Static(staticId);
      reactive.BindTopLevel(lf, myServerProtocol, "server");
      return x;
    }

    public class TestKeyHeavy : ThreadLocalRdContext<string>
    {
      private TestKeyHeavy() : base("test-key", true, Serializers.ReadString, Serializers.WriteString)
      {
      }
      
      public static readonly TestKeyHeavy Instance = new TestKeyHeavy();

      protected internal override void RegisterOn(ISerializers serializers)
      {
        serializers.Register((_, __) => Instance, (_, __, ___) => { });
      }
    }
    
    public class TestKeyLight : ThreadLocalRdContext<string>
    {
      private TestKeyLight() : base("test-key", false, Serializers.ReadString, Serializers.WriteString)
      {
      }
      
      public static readonly TestKeyLight Instance = new TestKeyLight();

      protected internal override void RegisterOn(ISerializers serializers)
      {
        serializers.Register((_, __) => Instance, (_, __, ___) => { });
      }
    }

    [Theory]
    public void TestEarlyDelivery(bool heavy)
    {
      var key = heavy ? TestKeyHeavy.Instance : (RdContext<string>) TestKeyLight.Instance;
      
      var identities = new SequentialIdentities(IdKind.Server);

      var serverDispatcher = CreateScheduler();
      var clientDispatcher = CreateScheduler();
      var serverR = "Server (R#)";
      myServerProtocol = new Protocol(serverR, CreateSerializers(), identities, serverDispatcher, new TestWire(serverDispatcher, serverR, true), LifetimeDefinition.Lifetime, key);
      myServerWire = (myServerProtocol.Wire as TestWire).NotNull();
      
      var clientIdea = "Client (IDEA)";
      var clientWire = new TestWire(clientDispatcher, clientIdea, false);
      myClientWire = clientWire;
      myClientWire.Connection = myServerWire;
      myServerWire.Connection = myClientWire;
      
      myClientProtocol = new Protocol(clientIdea, CreateSerializers(), identities, clientDispatcher, clientWire, LifetimeDefinition.Lifetime, key);

      myServerWire.AutoTransmitMode = true;
      myClientWire.AutoTransmitMode = true;
      
      var serverSignal = BindToServer(LifetimeDefinition.Lifetime, NewRdSignal<string>(), 1);

      using var _ = key.UpdateValue("1");
      
      serverSignal.Fire("");

      var clientSignal = BindToClient(LifetimeDefinition.Lifetime, NewRdSignal<string>(), 1);

      Lifetime.Using(lt =>
      {
        var fired = false;
        clientSignal.Advise(lt, s =>
        {
          Assert.AreEqual("1", key.Value);
          fired = true;
        });
        serverSignal.Fire("");
        Assert.True(fired, "fired");
      });

      Assert.AreEqual("1", key.Value);
      
      Lifetime.Using(lt =>
      {
        var fired = false;
        serverSignal.Advise(lt, s =>
        {
          Assert.AreEqual("1", key.Value);
          fired = true;
        });
        clientSignal.Fire("");
        Assert.True(fired, "fired");
      });
      
      Assert.AreEqual("1", key.Value);
    }
  }
}