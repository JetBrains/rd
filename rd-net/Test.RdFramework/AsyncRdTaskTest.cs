using System;
using System.Diagnostics;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Threading;
using JetBrains.Util;
using NUnit.Framework;

namespace Test.RdFramework;

[TestFixture]
public class AsyncRdTaskTest : RdFrameworkTestBase
{
  protected override IScheduler CreateScheduler(bool isServer)
  {
    return SingleThreadScheduler.RunOnSeparateThread(LifetimeDefinition.Lifetime, $"{(isServer ? "Server" : "Client")} Scheduler");
  }

  [Test]
  public void BindableRdCallListUseSystemTaskTest()
  {
    BindableRdCallListTest(TaskKind.System);
  }

  [Test]
  public void BindableRdCallListUseRdTaskTest()
  {
    BindableRdCallListTest(TaskKind.Rd);
  }

  public enum TaskKind
  {
    System,
    Rd,
  }

  private void BindableRdCallListTest(TaskKind taskKind)
  {  
    ClientWire.AutoTransmitMode = true;
    ServerWire.AutoTransmitMode = true;
    
    var lifetime = LifetimeDefinition.Lifetime; 
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

        var list = taskKind switch
        {
          TaskKind.System => await callsite.Start(lifetime, Unit.Instance).AsTask(),
          TaskKind.Rd     => await callsite.Start(lifetime, Unit.Instance),
          _ => throw new ArgumentOutOfRangeException(nameof(taskKind), taskKind, null)
        };
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
    }
  }
  
  [Test]
  [TestCase(TaskKind.Rd)]
  [TestCase(TaskKind.System)]
  public void TestRdTaskAwaiter(TaskKind kind)
  {
    var rdTask = new RdTask<Unit>();
    var scheduler = new TaskSchedulerWrapper(new ConcurrentExclusiveSchedulerPair(TaskScheduler.Default).ExclusiveScheduler, false);

    var task = TestLifetime.StartAsync(scheduler.AsTaskScheduler(), async () =>
    {
      scheduler.AssertThread();

      TestLifetime.Start(scheduler.AsTaskScheduler(), () =>
      {
        scheduler.AssertThread();
        TestLifetime.Start(TaskScheduler.Default, () =>
        {
          rdTask.ResultInternal.Set(RdTaskResult<Unit>.Success(Unit.Instance));
        }).NoAwait();
      }).NoAwait();
      
      _ = kind switch
      {
        TaskKind.System => await rdTask.AsTask(),
        TaskKind.Rd     => await rdTask,
        _ => throw new ArgumentOutOfRangeException(nameof(kind), kind, null)
      };
      
      scheduler.AssertThread();
    });

    task.Wait(TimeSpan.FromSeconds(10));
    Assert.IsTrue(task.IsCompleted);
  } 

  private static TimeSpan Timeout(TimeSpan timeout)
  {
    return Debugger.IsAttached ? TimeSpan.FromDays(1) : timeout;
  }
}