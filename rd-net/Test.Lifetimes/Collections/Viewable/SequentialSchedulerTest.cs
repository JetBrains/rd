using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
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
      var scheduler = CreateSequentialScheduler("TestScheduler", TestLifetime);
      
      Assert.IsFalse(scheduler.OutOfOrderExecution, "scheduler.OutOfOrderExecution");
      
      {
        Assert.IsFalse(scheduler.IsActive);
        
        for (var i = 0; i < 100; i++)
        {
          var sum = 0;
          var range = Enumerable.Range(0, n);
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
      var def = TestLifetime.CreateNested();
      var scheduler = CreateSequentialScheduler("TestScheduler", def.Lifetime);
      var count = 0;
      var reached = false;
      
      def.Lifetime.Start(scheduler, () =>
      {
        for (var i = 0; i < n; i++)
          scheduler.Queue(() => count++);
        Assert.AreEqual(0, count);
        def.Terminate();
        Assert.AreEqual(0, count);
        Assert.Throws<LifetimeCanceledException>(() => scheduler.Queue(() => reached = true));
      }).Wait();
      
      Assert.AreEqual(LifetimeStatus.Terminated, def.Status);
      
      SpinWait.SpinUntil(() => count == n, TimeSpan.FromSeconds(5));
      
      Assert.AreEqual(n, count);
      Assert.IsFalse(reached);
      Assert.Throws<LifetimeCanceledException>(() => scheduler.Queue(() => reached = true));
      Assert.IsFalse(reached);
    }
    
    [Test]
    public void IsActiveTest()
    {
      using var def = TestLifetime.CreateNested();
      
      var scheduler1 = CreateSequentialScheduler("TestScheduler1", def.Lifetime);
      var scheduler2 = CreateSequentialScheduler("TestScheduler2", def.Lifetime);
      
      Assert.IsFalse(scheduler1.IsActive);
      Assert.IsFalse(scheduler2.IsActive);
      
      def.Lifetime.Start(scheduler1, () =>
      {
        Assert.IsTrue(scheduler1.IsActive);
        Assert.IsFalse(scheduler2.IsActive);
        
        def.Lifetime.Start(scheduler2, () =>
        {
          Assert.IsFalse(scheduler1.IsActive);
          Assert.IsTrue(scheduler2.IsActive);
        }).Wait();
        
        Assert.IsTrue(scheduler1.IsActive);
        Assert.IsFalse(scheduler2.IsActive);
      }).Wait();
      
      def.Lifetime.StartAsync(scheduler1, async () =>
      {
        Assert.IsTrue(scheduler1.IsActive);
        Assert.IsFalse(scheduler2.IsActive);
        
        await def.Lifetime.Start(scheduler2, () =>
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

    private SequentialScheduler CreateSequentialScheduler(string id, Lifetime lifetime)
    {
      return mySchedulerKind switch
      {
        SchedulerKind.TaskScheduler => new SequentialScheduler(id, lifetime, TaskScheduler.Default),
        SchedulerKind.ISchduler => SequentialScheduler.FromIScheduler(id, lifetime, DefaultScheduler.Instance),
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