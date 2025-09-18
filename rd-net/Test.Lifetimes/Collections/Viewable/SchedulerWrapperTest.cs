using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Viewable
{
  public class SchedulerWrapperTest : LifetimesTestBase
  {
    [Test]
    public void SimpleTest()
    {
      var scheduler = SingleThreadScheduler.RunOnSeparateThread(TestLifetime, "Test Scheduler");
      DoTest(TestLifetime, scheduler);

      // IScheduler, but not TaskScheduler
      var schedulerWrapper = new MyTestSchedulerWrapper(scheduler);
      DoTest(TestLifetime, schedulerWrapper);

      static void DoTest(Lifetime lifetime, IScheduler scheduler)
      {
        var taskScheduler = scheduler.AsTaskScheduler();
        lifetime.StartAsync(taskScheduler, async () =>
        {
          var count = 0;
          Assert.IsTrue(scheduler.IsActive);
          Assert.AreEqual(taskScheduler, TaskScheduler.Current);

          scheduler.Queue(() =>
          {
            count = 1;
            Assert.IsTrue(scheduler.IsActive);
            Assert.AreNotEqual(taskScheduler, TaskScheduler.Current);
          });

          Assert.AreEqual(0, count);

          await lifetime.Start(TaskScheduler.Default, () =>
          {
            Assert.IsFalse(scheduler.IsActive);
            SpinWait.SpinUntil(() => Volatile.Read(ref count) == 1, TimeSpan.FromSeconds(10));
            Assert.AreEqual(1, count);

            count++;
          });

          Assert.AreEqual(2, count);
          Assert.IsTrue(scheduler.IsActive);
          Assert.AreEqual(taskScheduler, TaskScheduler.Current);
        }).Wait(TimeSpan.FromSeconds(10));
      }
    }
    
    private class MyTestSchedulerWrapper : IScheduler
    {
      private readonly IScheduler myScheduler;
      private readonly SyncContext mySyncContext;

      public MyTestSchedulerWrapper(IScheduler scheduler)
      {
        myScheduler = scheduler;
        mySyncContext = new SyncContext(this);
      }
      
      public void Queue(Action action)
      {
        myScheduler.Queue(() =>
        {
          var old = SynchronizationContext.Current;
          SynchronizationContext.SetSynchronizationContext(mySyncContext);
          try
          {
            action();
          }
          finally
          {
            SynchronizationContext.SetSynchronizationContext(old);
          }
        });
      }

      public bool IsActive => myScheduler.IsActive;
      public bool OutOfOrderExecution => myScheduler.OutOfOrderExecution;
      
      private class SyncContext : SynchronizationContext
      {
        private readonly MyTestSchedulerWrapper myScheduler;

        public SyncContext(MyTestSchedulerWrapper scheduler)
        {
          myScheduler = scheduler;
        }
        
        public override void Post(SendOrPostCallback d, object state)
        {
          myScheduler.Queue(() => d(state));
        }
      }
    }
  }
}