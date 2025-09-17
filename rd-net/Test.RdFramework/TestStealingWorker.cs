using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework
{
  public class TestStealingScheduler
  {
    private const int TestsCount = 100000;

    [TestCase(TestsCount)] // Basic corruption test.
    [TestCase(TestsCount / 2)] // >half of runs should be executed synchronously in Join
    public void TestExclusive(int defectiveRuns)
    {
      var w = new StealingScheduler(new ConcurrentExclusiveSchedulerPair(new DefectiveScheduler(defectiveRuns)).ExclusiveScheduler, false);
      int n = 0;
      for (int i = 0; i < TestsCount; i++)
        new Task(() => { n++; }).Start(w);

      w.Join();
      Assert.AreEqual(TestsCount, n);
    }

    [Test]
    public void TestMultithreadSimple()
    {
      var w = new StealingScheduler(TaskScheduler.Default);
      int n = 0;
      for (int i = 0; i < TestsCount; i++)
        new Task(() => { Interlocked.Increment(ref n); }).Start(w);
      w.Join();
      Assert.AreEqual(TestsCount, n);
    }

    [TestCase(0)] // scheduler is not available, all runs should be processed in Join
    [TestCase(TestsCount)] // Basic corruption test.
    public void TestMultithreadDefective(int defectiveRuns)
    {
      var w = new StealingScheduler(new DefectiveScheduler(defectiveRuns));
      int n = 0;
      for (int i = 0; i < TestsCount; i++)
        new Task(() => { Interlocked.Increment(ref n); }).Start(w);

      // may be caused by starvation in real life, simulating it by defective scheduler
      if (defectiveRuns == 0)
        Assert.AreEqual(0, n);

      w.Join();
      Assert.AreEqual(TestsCount, n);
    }

    /// <summary>
    /// Scheduler which works only limited number of times
    /// </summary>
    public class DefectiveScheduler : TaskScheduler
    {
      private int myRunCount;

      public DefectiveScheduler(int runCount) { myRunCount = runCount; }
      protected override IEnumerable<Task> GetScheduledTasks() { return Array.Empty<Task>();}

      protected override void QueueTask(Task task)
      {
        if (Interlocked.Decrement(ref myRunCount) >= 0)
          Task.Run(() => TryExecuteTask(task));
      }

      protected override bool TryExecuteTaskInline(Task task, bool taskWasPreviouslyQueued) { return false; }
    }
  }
}