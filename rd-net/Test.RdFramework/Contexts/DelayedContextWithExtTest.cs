using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using NUnit.Framework;
using Test.RdFramework.Interning;

namespace Test.RdFramework.Contexts
{
  public class DelayedContextWithExtTest : RdFrameworkTestBase
  {
    protected override IScheduler CreateScheduler(bool isServer)
    {
      return SingleThreadScheduler.RunOnSeparateThread(LifetimeDefinition.Lifetime, isServer ? "Server" : "Client");
    }

    [Datapoint]
    public static bool TrueValue = true;

    [Datapoint]
    public static bool FalseValue = false;
    
    [Theory]
    public void TestExtNoFailureOnQueuedNewContextValue(bool useHeavyContext)
    {
      var context = useHeavyContext
        ? RdContextBasicTest.TestKeyHeavy.Instance
        : (RdContext<string>) RdContextBasicTest.TestKeyLight.Instance;
      
      ServerWire.AutoTransmitMode = true;
      ClientWire.AutoTransmitMode = true;

      var barrierRegister = new Barrier(2);
      var barrier0 = new Barrier(2);
      var barrier1 = new Barrier(2);
      var barrier2 = new Barrier(2);
      
      var fireValues = new[] { "a", "b", "c" };
      
      ServerProtocol.Scheduler.Queue(() =>
      {
        barrierRegister.SignalAndWait();
        
        context.RegisterOn(ServerProtocol.Contexts);

        var serverModel = new InterningRoot1(LifetimeDefinition.Lifetime, ServerProtocol);
        
        ServerWire.TransmitAllMessages();
        
        barrier0.SignalAndWait(); // root model also uses ext semantics, so make sure both ends have created it and processed its connection message
        
        var serverExt = serverModel.GetOrCreateExtension("test", () => new InterningExt());
        foreach (var fireValue in fireValues)
        {
          context.Value = fireValue;
          serverExt.Root.Value = new InterningExtRootModel();
          context.Value = null;
        }

        barrier1.SignalAndWait();
      });
      
      var numReceives = 0;
      var receivedContexts = new HashSet<string>();
      
      ClientProtocol.Scheduler.Queue(() =>
      {
        context.RegisterOn(ClientProtocol.Serializers);
        
        barrierRegister.SignalAndWait();

        var clientModel = new InterningRoot1(LifetimeDefinition.Lifetime, ClientProtocol);

        barrier0.SignalAndWait();

        barrier1.SignalAndWait();
        
        ServerProtocol.Scheduler.Queue(() => barrier2.SignalAndWait());
        barrier2.SignalAndWait();
        
        var clientExt = clientModel.GetOrCreateExtension("test", () => new InterningExt());
        
        Thread.Sleep(500);
        
        clientExt.Root.AdviseNotNull(LifetimeDefinition.Lifetime, _ =>
        {
          numReceives++;
          receivedContexts.Add(context.Value);
        });
      });

      SpinWait.SpinUntil(() => numReceives == 3, TimeSpan.FromMilliseconds(5_000));
      
      Assert.AreEqual(3, numReceives);
      Assert.AreEqual(fireValues, receivedContexts);
    }
  }
}