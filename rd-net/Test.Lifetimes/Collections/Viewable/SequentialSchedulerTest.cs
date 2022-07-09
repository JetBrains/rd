using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Util.Internal;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Viewable
{
#if !NET35
  [TestFixture(SchedulerKind.TaskScheduler)]
  [TestFixture(SchedulerKind.ISchduler)]
  public class SequentialSchedulerTest : LifetimesTestBase
  {
    private readonly SchedulerKind mySchedulerKind;

    public SequentialSchedulerTest(SchedulerKind schedulerKind)
    {
      mySchedulerKind = schedulerKind;
    }
    
    [Test]
    public void SimpleTest()
    {
      const int n = 1000;
      var scheduler = CreateSequentialScheduler("TestScheduler");
      
      Assert.IsFalse(scheduler.OutOfOrderExecution, "scheduler.OutOfOrderExecution");
      
      {
        Assert.IsFalse(scheduler.IsActive);
        
        for (var i = 0; i < 100; i++)
        {
          var sum = 0;
          var range = Enumerable.Range(0, n).ToArray();
          var tasks = range.Select(j => TestLifetime.Start(scheduler, () =>
          {
            sum += j;
            Assert.IsTrue(scheduler.IsActive);
          })).ToArray();
          
          Task.WaitAll(tasks);
          Assert.AreEqual(range.Sum(), sum);
        }
      }
      
      Assert.IsFalse(scheduler.IsActive);
      
      {
        TestLifetime.StartAsync(scheduler, async () =>
        {
          Assert.IsTrue(scheduler.IsActive);
          
          var reached = false;
          scheduler.Queue(() =>
          {
            reached = true;
            Assert.IsTrue(scheduler.IsActive);
          });
          
          SpinWait.SpinUntil(() => reached, TimeSpan.FromMilliseconds(100));
          
          Assert.IsFalse(reached);
          
          await Task.Yield();
          
          Assert.IsTrue(scheduler.IsActive);
          Assert.IsTrue(reached);
        }).Wait();
      }
    }
    
    [Test]
    public void DisposeTest()
    {
      const int n = 1000;
      
      var scheduler = CreateSequentialScheduler("TestScheduler");
      var count = 0;
      
      TestLifetime.Start(scheduler, () =>
      {
        for (var i = 0; i < n; i++)
          scheduler.Queue(() => count++);
        Assert.AreEqual(0, count);
        Assert.AreEqual(0, count);
      }).Wait();
      
      SpinWait.SpinUntil(() => Memory.VolatileRead(ref count) == n, TimeSpan.FromSeconds(5));
      
      Assert.AreEqual(n, count);
    }
    
    [Test]
    public void IsActiveTest()
    {
      var scheduler1 = CreateSequentialScheduler("TestScheduler1");
      var scheduler2 = CreateSequentialScheduler("TestScheduler2");
      
      Assert.IsFalse(scheduler1.IsActive);
      Assert.IsFalse(scheduler2.IsActive);
      
      TestLifetime.Start(scheduler1, () =>
      {
        Assert.IsTrue(scheduler1.IsActive);
        Assert.IsFalse(scheduler2.IsActive);
        
        TestLifetime.Start(scheduler2, () =>
        {
          Assert.IsFalse(scheduler1.IsActive);
          Assert.IsTrue(scheduler2.IsActive);
        }).Wait();
        
        Assert.IsTrue(scheduler1.IsActive);
        Assert.IsFalse(scheduler2.IsActive);
      }).Wait();
      
      TestLifetime.StartAsync(scheduler1, async () =>
      {
        Assert.IsTrue(scheduler1.IsActive);
        Assert.IsFalse(scheduler2.IsActive);
        
        await TestLifetime.Start(scheduler2, () =>
        {
          Assert.IsFalse(scheduler1.IsActive);
          Assert.IsTrue(scheduler2.IsActive);
        });
        
        Assert.IsTrue(scheduler1.IsActive);
        Assert.IsFalse(scheduler2.IsActive);
      }).Wait();
      
      Assert.IsFalse(scheduler1.IsActive);
      Assert.IsFalse(scheduler2.IsActive);
    }

    private SequentialScheduler CreateSequentialScheduler(string id)
    {
      return mySchedulerKind switch
      {
        SchedulerKind.TaskScheduler => new SequentialScheduler(id, TaskScheduler.Default),
        SchedulerKind.ISchduler => SequentialScheduler.FromIScheduler(id, DefaultScheduler.Instance),
        _ => throw new ArgumentOutOfRangeException()
      };
    }
  }
  
  public enum SchedulerKind
  {
    TaskScheduler,
    ISchduler
  }
#endif
}