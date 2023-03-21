using System;
using System.Diagnostics;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Util;
using NUnit.Framework;

namespace Test.RdFramework;

#if !NET35
[TestFixture]
public class AsyncRdTaskTest : RdFrameworkTestBase
{
  protected override IScheduler CreateScheduler(bool isServer)
  {
    return SingleThreadScheduler.RunOnSeparateThread(LifetimeDefinition.Lifetime, $"{(isServer ? "Server" : "Client")} Scheduler");
  }

  [Test]
  public void BindableRdCallListTest()
  {  
    ClientWire.AutoTransmitMode = true;
    ServerWire.AutoTransmitMode = true;
    
    LifetimeDefinition.Lifetime.UsingNested(lifetime =>
    {
      var entity_id = 1;

      var callsite = new RdCall<Unit, RdList<int>>(Serializers.ReadVoid, Serializers.WriteVoid, RdList<int>.Read, RdList<int>.Write);
      var endpoint = new RdCall<Unit, RdList<int>>(Serializers.ReadVoid, Serializers.WriteVoid, RdList<int>.Read, RdList<int>.Write);

      var n = 1;

      var bindServerTask = lifetime.Start(ServerProtocol.Scheduler.AsTaskScheduler(), () =>
      {
        BindToServer(lifetime, endpoint, entity_id);
        endpoint.SetSync(_ =>
        {
          var list = new RdList<int>(Polymorphic<int>.Read, Polymorphic<int>.Write);
          for (var i = 0; i < n; i++)
            list.Add(i);

          return list;
        });
      });
      Assert.IsTrue(bindServerTask.Wait(Timeout(TimeSpan.FromSeconds(10))));

      var bindClientTask = lifetime.StartAsync(ClientProtocol.Scheduler.AsTaskScheduler(), async () =>
      {
        BindToClient(lifetime, callsite, entity_id);

        var list = await callsite.Start(lifetime, Unit.Instance).AsTask();
        var count = 0;

        list.View(lifetime, (lt, index, value) =>
        {
          Assert.AreEqual(count++, value);
          Assert.AreEqual(index, value);
        });

        var stopwatch = LocalStopwatch.StartNew();
        while (count != n)
        {
          if (stopwatch.Elapsed > Timeout(TimeSpan.FromSeconds(10)))
            throw new TimeoutException();
          
          await Task.Yield();
        }
      });
      
      Assert.IsTrue(bindClientTask.Wait(Timeout(TimeSpan.FromSeconds(10))));
    });
  }

  private static TimeSpan Timeout(TimeSpan timeout)
  {
    return Debugger.IsAttached ? TimeSpan.FromDays(1) : timeout;
  }
}
#endif