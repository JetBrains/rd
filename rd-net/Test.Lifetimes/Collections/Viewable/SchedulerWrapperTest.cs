using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Viewable
{
#if !NET35
  public class SchedulerWrapperTest : LifetimesTestBase
  {
    [Test]
    public void SimpleTest()
    {
      var log = Log.GetLog<SchedulerWrapperTest>();
      var scheduler = SingleThreadScheduler.RunOnSeparateThread(TestLifetime, "Test Scheduler");
      
      DoTest(TestLifetime, scheduler, log.GetSublogger("1"));

      // IScheduler, but not TaskScheduler
      var schedulerWrapper = new MyTestSchedulerWrapper(scheduler);
      DoTest(TestLifetime, schedulerWrapper, log.GetSublogger("2"));

      static void DoTest(Lifetime lifetime, IScheduler scheduler, ILog log)
      {
        log.Verbose("start");
        
        var taskScheduler = scheduler.AsTaskScheduler();
        lifetime.StartAsync(taskScheduler, async () =>
        {
          log.Verbose($"Point 1. Thread: {Thread.CurrentThread.ManagedThreadId}");
          
          var count = 0;
          Assert.IsTrue(scheduler.IsActive);
          Assert.AreEqual(taskScheduler, TaskScheduler.Current);

          scheduler.Queue(() =>
          {
            count = 1;
            log.Verbose($"Point 5. Thread: {Thread.CurrentThread.ManagedThreadId}");
            Assert.IsTrue(scheduler.IsActive);
            Assert.AreNotEqual(taskScheduler, TaskScheduler.Current);
          });
          
          log.Verbose($"Point 2. Thread: {Thread.CurrentThread.ManagedThreadId}");

          Assert.AreEqual(0, count);

          var task = lifetime.Start(TaskScheduler.Default, () =>
          {
            log.Verbose($"Point 4. Thread: {Thread.CurrentThread.ManagedThreadId}");
            Assert.IsFalse(scheduler.IsActive);
          });
          
          log.Verbose($"Point 3. Thread: {Thread.CurrentThread.ManagedThreadId}");

          await task;
          
          log.Verbose($"Point 6. Thread: {Thread.CurrentThread.ManagedThreadId}");
          
          Assert.AreEqual(taskScheduler, TaskScheduler.Current);
          Assert.IsTrue(scheduler.IsActive);
          Assert.AreEqual(1, count);
        }).Wait(TimeSpan.FromSeconds(10));
        
        log.Verbose("end");
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
#endif
}