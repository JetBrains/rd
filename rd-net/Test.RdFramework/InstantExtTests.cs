using System;
using demo;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework;

#if !NET35
[TestFixture]
public class InstantExtTests : RdFrameworkTestBase
{
  protected override IScheduler CreateScheduler(bool isServer)
  {
    return SingleThreadScheduler.RunOnSeparateThread(TestLifetime, $"TestScheduler: {(isServer ? "Server" : "Client")}");
  }

  [Test]
  public void InstantExtTest()
  {
    ClientWire.AutoTransmitMode = true;
    ServerWire.AutoTransmitMode = true;
    
    using var testLifetimeDef = TestLifetime.CreateNested();
    var testLifetime = testLifetimeDef.Lifetime;
    
    var modelCounter = 0;
    var baseExtCounter = 0;

    var clientProtocol = (Protocol)ClientProtocol;
    clientProtocol.ExtCreated.Advise(testLifetimeDef.Lifetime, it =>
    {
      if (it.IsLocal)
        return;

      if (it.Info.Name.LocalName.Equals(nameof(InstantExtModel), StringComparison.OrdinalIgnoreCase))
      {
        var parentId = it.Info.Id;
        Assert.NotNull(parentId != null, "it.Info.Id != null");
        if (!clientProtocol.RdEntitiesRegistrar.TryGetEntity(parentId.Value, out var entity))
        {
          Assert.Fail("");
        }

        var extensibleModel = (ExtensibleModel)entity;
        var model = extensibleModel.GetInstantExtModel();
        Assert.IsTrue(model.Connected.Value, "Ext is not connected");

        var bindLifetime = model.BindLifetime.Value;
        model.Checker.Advise(bindLifetime, i =>
        {
          Assert.IsTrue(ClientProtocol.Scheduler.IsActive);

          Assert.AreEqual(modelCounter, i);
          Assert.AreEqual(modelCounter, baseExtCounter);
          modelCounter++;
        });
      }
    });

    var serverInstantHelper = ServerProtocol.GetInstantHelperExt();
    var clientInstantHelper = ClientProtocol.GetInstantHelperExt();

    FlushAllSchedulers();

    clientProtocol.Scheduler.Queue(() =>
    {
      clientInstantHelper.Checker.Advise(testLifetimeDef.Lifetime, i =>
      {
        Assert.IsTrue(clientProtocol.Scheduler.IsActive);

        Assert.AreEqual(baseExtCounter, i);
        baseExtCounter++;
        Assert.AreEqual(modelCounter, baseExtCounter);
      });
    });
    
    FlushAllSchedulers();

    const int n = 10_000;


    var task = testLifetime.Start(ServerProtocol.Scheduler.AsTaskScheduler(), () =>
    {
      for (var i = 0; i < n; i++)
      {
        if (testLifetime.IsNotAlive)
          return;

        var extensibleModel = new ExtensibleModel();
        serverInstantHelper.Value.Value = extensibleModel;
        var model = extensibleModel.GetInstantExtModel();
        Assert.IsTrue(model.Connected.Value, "Ext is not connected");
        model.Checker.Fire(i);
        serverInstantHelper.Checker.Fire(i);
      }
    });

    task.Wait(testLifetime);

    FlushAllSchedulers();

    Assert.AreEqual(n, modelCounter);
    Assert.AreEqual(n, baseExtCounter);
  }
}
#endif