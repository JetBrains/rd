using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.Lifetimes.Threading;

#if !NET35
public class AsyncGroupingEventTest : LifetimesTestBase
{
  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void SimpleTest(Kind kind)
  {
    var groupingEvent = new AsyncGroupingEvent();
    var version = 0;
    var scheduler = new PumpScheduler(TestLifetime);
    var taskScheduler = scheduler.AsTaskScheduler();
    
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);

    TestLifetime.UsingNested(lifetime =>
    {
      Advise(groupingEvent, kind, lifetime, TimeSpan.FromMilliseconds(1), taskScheduler, () => version++);
      Assert.AreEqual(0, version);

      groupingEvent.Fire();

      Assert.AreEqual(0, version);
      SpinWait.SpinUntil(() =>
      {
        if (!scheduler.PumpOnce()) 
          Assert.AreEqual(0, version);

        return version != 0;
      }, TimeSpan.FromSeconds(10));

      Assert.AreEqual(1, version);
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation

    version = 0;

    groupingEvent.Fire();

    SpinWait.SpinUntil(() =>
    {
      scheduler.PumpOnce();
      return version != 0;
    }, TimeSpan.FromMilliseconds(100));

    Assert.AreEqual(0, version);
  }
  
  [Test]
  public void SampleManyFireTest()
  {
    var groupingEvent = new AsyncGroupingEvent();
    var version = 0;

    var scheduler = new PumpScheduler(TestLifetime);
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      groupingEvent.Sample(lifetime, TimeSpan.FromMilliseconds(20), taskScheduler, () => version++);

      var stopwatch = Stopwatch.StartNew();
      for (var i = 0; i < 5; i++)
      {
        while (version != i)
        {
          groupingEvent.Fire();
          scheduler.PumpOnce();
          Assert.IsTrue(stopwatch.Elapsed < TimeSpan.FromSeconds(10));
        }
      }
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation
  }
  
  [Test]
  public void DebounceManyFireTest()
  {
    var groupingEvent = new AsyncGroupingEvent();
    var version = 0;

    var scheduler = new PumpScheduler(TestLifetime);
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      groupingEvent.Debounce(lifetime, TimeSpan.FromMilliseconds(20), taskScheduler, () => version++);

      var stopwatch = Stopwatch.StartNew();
      
      while (stopwatch.Elapsed <= TimeSpan.FromMilliseconds(100))
      {
        Assert.AreEqual(0, version);
        do
        {
          groupingEvent.Fire();
        } while (scheduler.PumpOnce());
      }
    });
    
    Assert.IsTrue(SpinWait.SpinUntil(() => scheduler.PumpOnce(), TimeSpan.FromSeconds(10))); // cancellation
  }

  // works correctly in Release configuration only
  // [TestCase(Kind.Sample)]
  // [TestCase(Kind.Debounce)]
  public void MemoryLeakTest(Kind kind)
  {
    var groupingEvent = new AsyncGroupingEvent();
    var weakRef = Lifetime.Using(lifetime => GetWeakRef(groupingEvent, kind, lifetime));

    var stopwatch = Stopwatch.StartNew();
    while (weakRef.TryGetTarget(out _) && stopwatch.Elapsed <= TimeSpan.FromSeconds(10)) 
      GC.GetTotalMemory(true);

    Assert.IsFalse(weakRef.TryGetTarget(out _));
    GC.KeepAlive(groupingEvent);

    
    [MethodImpl(MethodImplOptions.NoInlining)]
    static WeakReference<object> GetWeakRef(AsyncGroupingEvent groupingEvent, Kind kind, Lifetime lifetime)
    {
      var o = new object();
      Advise(groupingEvent, kind, lifetime, TimeSpan.FromMilliseconds(10), TaskScheduler.Default, async _ =>
      {
        await Task.Yield();
        GC.KeepAlive(o);
      });
    
      groupingEvent.Fire();
      return new WeakReference<object>(o);
    }
  }

  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void CancellationTest(Kind kind)
  {
    AsyncGroupingEvent.OverrideMinDuration(TestLifetime, TimeSpan.Zero);
    
    var groupingEvent = new AsyncGroupingEvent();
    var version = 0;
    var scheduler = new PumpScheduler(TestLifetime);
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      Advise(groupingEvent, kind, lifetime, TimeSpan.FromMilliseconds(10), taskScheduler, () => version++);

      for (var i = 0; i < 2; i++)
      {
        groupingEvent.Fire();
        groupingEvent.Cancel();

        Assert.IsTrue(scheduler.PumpOnce());
        Assert.IsFalse(scheduler.PumpOnce());
        Assert.AreEqual(0, version);
      }
      
      Advise(groupingEvent, kind, lifetime, TimeSpan.Zero, taskScheduler, () => version++);

      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsFalse(scheduler.PumpOnce());
      Assert.AreEqual(0, version);
      
      groupingEvent.Fire();
      
      SpinWait.SpinUntil(() =>
      {
        scheduler.PumpOnce();
        return version == 2;
      }, TimeSpan.FromMilliseconds(100));
      
      Assert.AreEqual(2, version);
    });

    Assert.IsTrue(scheduler.PumpOnce()); // cancellation subscription 1
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation subscription 2
  }
  
  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void StressCancelFireTest(Kind kind)
  {
    var groupingEvent = new AsyncGroupingEvent();
    var version = 0;
    var scheduler = new PumpScheduler(TestLifetime);
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      Advise(groupingEvent, kind, lifetime, TimeSpan.FromMilliseconds(20), taskScheduler, () => version++);

      var tasks = Enumerable.Range(0, Environment.ProcessorCount).Select(_ => Task.Run(() =>
      {
        var stopwatch = Stopwatch.StartNew();
        while (stopwatch.Elapsed < TimeSpan.FromMilliseconds(100))
        {
          groupingEvent.Cancel();
          groupingEvent.Fire();
          scheduler.PumpOnce();
        }
      })).ToArray();

      var task = Task.WhenAll(tasks);
      while (!task.IsCompleted) 
        scheduler.PumpOnce();

      SpinWait.SpinUntil(() =>
      {
        scheduler.PumpOnce();
        return version != 0;
      }, TimeSpan.FromMilliseconds(100));
      
      Assert.AreEqual(1, version);
    });

    Assert.IsTrue(scheduler.PumpOnce()); // cancellation subscription
  }
  
  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void StressFireCancelTest(Kind kind)
  {
    var groupingEvent = new AsyncGroupingEvent();
    var version = 0;
    var scheduler = new PumpScheduler(TestLifetime);
    
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      Advise(groupingEvent, kind, lifetime, TimeSpan.FromMilliseconds(100), taskScheduler, () => version++);

      var tasks = Enumerable.Range(0, Environment.ProcessorCount).Select(_ => Task.Run(() =>
      {
        var stopwatch = Stopwatch.StartNew();
        while (stopwatch.Elapsed < TimeSpan.FromMilliseconds(200))
        {
          groupingEvent.Fire();
          scheduler.PumpOnce();
          groupingEvent.Cancel();
          scheduler.PumpOnce();
        }
      })).ToArray();

      var task = Task.WhenAll(tasks);
      while (!task.IsCompleted) 
        scheduler.PumpOnce();

      SpinWait.SpinUntil(() =>
      {
        scheduler.PumpOnce();
        return version != 0;
      }, TimeSpan.FromMilliseconds(200));
      
      Assert.AreEqual(0, version);
    });

    Assert.IsTrue(scheduler.PumpOnce()); // cancellation subscription 1
  }

  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void ActionCancellationTest(Kind kind)
  {
    AsyncGroupingEvent.OverrideMinDuration(TestLifetime, TimeSpan.Zero);
    
    var groupingEvent = new AsyncGroupingEvent();
    var scheduler = new PumpScheduler(TestLifetime);
    
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      var started = false;
      var cancelled = false;
      Advise(groupingEvent, kind, lifetime, TimeSpan.Zero, taskScheduler, async token =>
      {
        started = true;
        Assert.IsFalse(token.IsCancellationRequested);
        while (!token.IsCancellationRequested)
        {
          Assert.IsTrue(scheduler.IsActive);
          await Task.Yield();
        }
        cancelled = token.IsCancellationRequested;
        Assert.IsTrue(token.IsCancellationRequested);
        token.ThrowIfCancellationRequested();
      });
      
      groupingEvent.Fire();
      
      Assert.IsTrue(scheduler.PumpOnce()); // start processing
      Assert.IsTrue(scheduler.PumpOnce()); // action
      Assert.IsTrue(started);
      
      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsFalse(cancelled);
      
      groupingEvent.Cancel();
      
      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsFalse(scheduler.PumpOnce());
      
      Assert.IsTrue(cancelled);
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation
  }

  [Test]
  public void DebounceMaxDurationTest()
  {
    var groupingEvent = new AsyncGroupingEvent();
    var scheduler = new PumpScheduler(TestLifetime);
    
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      var stopped = false;
      var duration = TimeSpan.FromMilliseconds(20);
      var maxDuration = TimeSpan.FromMilliseconds(90);
      var stopwatch = Stopwatch.StartNew();
      groupingEvent.Debounce(lifetime, duration, maxDuration, taskScheduler, () =>
      {
        Assert.IsFalse(stopped);
        stopwatch.Stop();
        stopped = true;
      });

      while (!stopped)
      {
        groupingEvent.Fire();
        scheduler.PumpOnce();
        Assert.IsTrue(stopwatch.Elapsed <= TimeSpan.FromSeconds(10));
      }
      
      Assert.IsTrue(stopped);
      Assert.IsTrue(stopwatch.Elapsed >= maxDuration);
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation
  }

  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void DelayBetweenSlowActionsTest(Kind kind)
  {
    var groupingEvent = new AsyncGroupingEvent();
    var scheduler = new PumpScheduler(TestLifetime);
    
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      var finished = false;
      var first = false;
      var second = false;
      var duration = TimeSpan.FromMilliseconds(50);
      Advise(groupingEvent, kind, lifetime, duration, taskScheduler, async token =>
      {
        Assert.IsFalse(finished);
        
        await Task.Yield();
        first = true;
        await Task.Yield();
        second = true;
        await Task.Delay(100, token);
        finished = true;
      });
      
      groupingEvent.Fire();

      var stopwatch = Stopwatch.StartNew();
      while (!first)
      {
        scheduler.PumpOnce();
        Assert.IsTrue(stopwatch.Elapsed < TimeSpan.FromSeconds(10));
      }
      
      Assert.IsTrue(first);
      Assert.IsFalse(second);
      Assert.IsFalse(finished);
      
      groupingEvent.Fire();
      
      Assert.IsTrue(first);
      Assert.IsFalse(second);
      Assert.IsFalse(finished);
      
      scheduler.PumpOnce();
      
      Assert.IsTrue(first);
      Assert.IsTrue(second);
      Assert.IsFalse(finished);
      
      scheduler.PumpOnce();
      
      Assert.IsTrue(first);
      Assert.IsTrue(second);
      Assert.IsFalse(finished);
      
      while (!finished)
      {
        scheduler.PumpOnce();
        Assert.IsTrue(stopwatch.Elapsed < TimeSpan.FromSeconds(10));
      }
      
      Assert.IsFalse(scheduler.PumpOnce());
      groupingEvent.Fire();
      Assert.IsFalse(scheduler.PumpOnce());
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation
  }

  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void HandleException(Kind kind)
  {
    AsyncGroupingEvent.OverrideMinDuration(TestLifetime, TimeSpan.Zero);
    
    var groupingEvent = new AsyncGroupingEvent();
    var scheduler = new PumpScheduler(TestLifetime);
    
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      var message = "Bla bla bla 1234 bla bla bla GroupingEvent::HandleException::Test";
      Advise(groupingEvent, kind, lifetime, TimeSpan.Zero, taskScheduler, () => throw new InvalidOperationException(message));
      
      groupingEvent.Fire();
      Assert.IsTrue(scheduler.PumpOnce()); // start processing
      Assert.IsTrue(scheduler.PumpOnce()); // action
      Assert.IsFalse(scheduler.PumpOnce());


      var exception = TestLogger.ExceptionLogger.RecycleLoggedExceptions();
      Assert.IsTrue(exception.Message.Contains(message));
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation
  }

  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void SimpleAggregatedValuesTest(Kind kind)
  {
    AsyncGroupingEvent.OverrideMinDuration(TestLifetime, TimeSpan.Zero);
    
    var groupingEvent = new AsyncGroupingEvent<int>();
    var scheduler = new PumpScheduler(TestLifetime);
    
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      AppendOnlyList<int>? capturedList = null; 
      Advise(groupingEvent, kind, lifetime, TimeSpan.Zero, taskScheduler, list =>
      {
        capturedList = list;
        groupingEvent.Fire(3);  
      });
      
      groupingEvent.Fire(2);   
      Assert.IsTrue(scheduler.PumpOnce()); // start processing
      Assert.IsTrue(scheduler.PumpOnce()); // action
      
      Assert.IsNotNull(capturedList);
      Assert.AreEqual(1, capturedList.Count);
      Assert.AreEqual(2, capturedList[0]);

      capturedList = null;
      
      Assert.IsTrue(scheduler.PumpOnce()); // action
      Assert.IsNotNull(capturedList);
      Assert.AreEqual(1, capturedList.Count);
      Assert.AreEqual(3, capturedList[0]);
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation
  }
  
  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void SimpleAggregatedValuesMaxCountTest(Kind kind)
  {
    AsyncGroupingEvent.OverrideMinDuration(TestLifetime, TimeSpan.Zero);
    
    var groupingEvent = new AsyncGroupingEvent<int>();
    var scheduler = new PumpScheduler(TestLifetime);
    
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      AppendOnlyList<int>? capturedList = null;
      var called = false;
      Advise(groupingEvent, kind, 3, lifetime, TimeSpan.Zero, taskScheduler, list =>
      {
        capturedList = list;
        called = true;
      });
      
      groupingEvent.Fire(2);   
      groupingEvent.Fire(3);   
      groupingEvent.Fire(4);   
      groupingEvent.Fire(5);   
      Assert.IsTrue(scheduler.PumpOnce()); // start processing
      Assert.IsTrue(scheduler.PumpOnce()); // action
      
      Assert.IsTrue(called);
      Assert.IsNull(capturedList);
      
      groupingEvent.Fire(2);   
      Assert.IsTrue(scheduler.PumpOnce()); // start processing
      Assert.IsTrue(scheduler.PumpOnce()); // action
      
      Assert.IsNotNull(capturedList);
      Assert.AreEqual(1, capturedList.Count);
      Assert.AreEqual(2, capturedList[0]);
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation
  }
  
  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void ConcurrentAggregatedValuesTest(Kind kind)
  {
    AsyncGroupingEvent.OverrideMinDuration(TestLifetime, TimeSpan.Zero);
    
    var groupingEvent = new AsyncGroupingEvent<int>();
    var scheduler = new PumpScheduler(TestLifetime);
    
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      var all = new List<AppendOnlyList<int>>();
      Advise(groupingEvent, kind, lifetime, TimeSpan.Zero, taskScheduler, list =>
      {
        all.Add(list);
      });
      for (var i = 0; i < 20; i++)
      {
        all.Clear();
        
        const int n = 1235;
        const int threads = 5;
        const int itemsPerThread = n / threads;
      
        var tasks = Enumerable.Range(0, threads).Select(x => Task.Factory.StartNew(() =>
        {
          var start = itemsPerThread * x;
          var spinner = new SpinWaitEx();
          for (var item = start; item < start + itemsPerThread; item++)
          {
            groupingEvent.Fire(item);
            scheduler.PumpOnce();
            spinner.SpinOnce(false);
          }
        })).ToArray();

        var task = Task.WhenAll(tasks);
        while (!task.IsCompleted) 
          scheduler.PumpOnce();

        while (scheduler.PumpOnce()) { }
      
        var ints = all.SelectMany(x => x).ToList();
        Assert.AreEqual(n, ints.Count);
        var bools = new bool[n];
        foreach (var value in ints)
        {
          if (bools[value])
            Assert.Fail();

          bools[value] = true;
        }
      }
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation
  }

  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void SimpleCancellationAggregatedValuesTest(Kind kind)
  {
    AsyncGroupingEvent.OverrideMinDuration(TestLifetime, TimeSpan.Zero);
    
    var groupingEvent = new AsyncGroupingEvent<int>();
    var scheduler = new PumpScheduler(TestLifetime);
    
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      AppendOnlyList<int>? capturedList = null; 
      Advise(groupingEvent, kind, lifetime, TimeSpan.Zero, taskScheduler, list =>
      {
        capturedList = list;
      });
      
      Assert.IsTrue(scheduler.PumpOnce()); // start processing
      
      groupingEvent.Fire(2);
      groupingEvent.Cancel();
      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsFalse(scheduler.PumpOnce());
      
      Assert.IsNull(capturedList);
      
      groupingEvent.Fire(3);
      
      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsNotNull(capturedList);
      Assert.AreEqual(1, capturedList.Count);
      Assert.AreEqual(3, capturedList[0]);
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation
  }
  
  [TestCase(Kind.Sample)]
  [TestCase(Kind.Debounce)]
  public void SuspendTest(Kind kind)
  {
    AsyncGroupingEvent.OverrideMinDuration(TestLifetime, TimeSpan.Zero);
    
    var groupingEvent = new AsyncGroupingEvent();
    var scheduler = new PumpScheduler(TestLifetime);
    
    var taskScheduler = scheduler.AsTaskScheduler();
    AsyncGroupingEvent.OverrideProcessingScheduler(TestLifetime, taskScheduler);
    
    TestLifetime.UsingNested(lifetime =>
    {
      var called = false;
      Advise(groupingEvent, kind, lifetime, TimeSpan.Zero, taskScheduler, () =>
      {
        called = true;
      });
      
      Assert.IsTrue(scheduler.PumpOnce()); // start processing
      
      groupingEvent.Fire();

      lifetime.UsingNested(suspendLifetime =>
      {
        groupingEvent.Suspend(suspendLifetime);

        for (var i = 0; i < 100; i++)
        {
          Assert.IsFalse(called);
          scheduler.PumpOnce();
        }
      });
      
      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsFalse(scheduler.PumpOnce());
      Assert.IsTrue(called);

      called = false;
      
      groupingEvent.Fire();
      
      lifetime.UsingNested(suspendLifetime =>
      {
        groupingEvent.Suspend(suspendLifetime);

        for (var i = 0; i < 100; i++)
        {
          Assert.IsFalse(called);
          scheduler.PumpOnce();
        }
        
        groupingEvent.Cancel();
      });
      
      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsFalse(scheduler.PumpOnce());
      Assert.IsFalse(called);
      
      called = false;
      
      groupingEvent.Fire();
      
      lifetime.UsingNested(suspendLifetime =>
      {
        groupingEvent.Suspend(suspendLifetime);

        for (var i = 0; i < 100; i++)
        {
          Assert.IsFalse(called);
          scheduler.PumpOnce();
        }
        
        groupingEvent.Cancel();
        groupingEvent.Fire();
        
        for (var i = 0; i < 100; i++)
        {
          Assert.IsFalse(called);
          Assert.IsTrue(scheduler.PumpOnce()); // action
        }
      });
      
      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsTrue(scheduler.PumpOnce());
      Assert.IsFalse(scheduler.PumpOnce());
      Assert.IsTrue(called);
    });
    
    Assert.IsTrue(scheduler.PumpOnce()); // cancellation
  }

  
  
  
  public enum Kind
  {
    Sample,
    Debounce
  }
  
  private static void Advise<T>(AsyncGroupingEvent<T> groupingEvent, Kind kind, Lifetime lifetime, TimeSpan timeSpan, TaskScheduler scheduler, Action<AppendOnlyList<T>> action)
  {
    switch (kind)
    {
      case Kind.Sample:
        groupingEvent.Sample(lifetime, timeSpan, scheduler, action);
        break;
      case Kind.Debounce:
        groupingEvent.Debounce(lifetime, timeSpan, scheduler, action);
        break;
      default:
        throw new ArgumentOutOfRangeException(nameof(kind), kind, null);
    }
  }
  
  private static void Advise<T>(AsyncGroupingEvent<T> groupingEvent, Kind kind, int maxCount, Lifetime lifetime, TimeSpan timeSpan, TaskScheduler scheduler, Action<AppendOnlyList<T>?> action)
  {
    switch (kind)
    {
      case Kind.Sample:
        groupingEvent.Sample(lifetime, timeSpan, maxCount, scheduler, action);
        break;
      case Kind.Debounce:
        groupingEvent.Debounce(lifetime, timeSpan, maxCount, scheduler, action);
        break;
      default:
        throw new ArgumentOutOfRangeException(nameof(kind), kind, null);
    }
  }

  private static void Advise(AsyncGroupingEvent groupingEvent, Kind kind, Lifetime lifetime, TimeSpan timeSpan, TaskScheduler scheduler, Action action)
  {
    switch (kind)
    {
      case Kind.Sample:
        groupingEvent.Sample(lifetime, timeSpan, scheduler, action);
        break;
      case Kind.Debounce:
        groupingEvent.Debounce(lifetime, timeSpan, scheduler, action);
        break;
      default:
        throw new ArgumentOutOfRangeException(nameof(kind), kind, null);
    }
  }
  
  private static void Advise(AsyncGroupingEvent groupingEvent, Kind kind, Lifetime lifetime, TimeSpan timeSpan, TaskScheduler scheduler, Func<CancellationToken, Task> action)
  {
    switch (kind)
    {
      case Kind.Sample:
        groupingEvent.Sample(lifetime, timeSpan, scheduler, action);
        break;
      case Kind.Debounce:
        groupingEvent.Debounce(lifetime, timeSpan, scheduler, action);
        break;
      default:
        throw new ArgumentOutOfRangeException(nameof(kind), kind, null);
    }
  }
}

#endif