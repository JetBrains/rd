using System;
using System.Runtime.CompilerServices;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Lifetimes;
using JetBrains.Util;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Viewable
{
  public class SignalTest : LifetimesTestBase
  {
    [Test]
    public void TestSignalStress()
    {
      var sig = new Signal<bool>();
      using (var run = new LifetimeDefinition())
      {
        var lt = run.Lifetime;
        var fireTask = Task.Factory.StartNew(() =>
        {
          while (lt.IsAlive) 
            sig.Fire(true);
        }, lt);

        Parallel.For(0, 100000, i =>
        {
          using (var ld = new LifetimeDefinition())
            sig.Advise(ld.Lifetime, x => { });
        });

        run.Terminate();
        fireTask.Wait(TimeSpan.FromSeconds(1));
        Assert.AreEqual(TaskStatus.RanToCompletion, fireTask.Status);
      }
    }

    [Test]
    public void ReentrancyPriorityAdviceTest()
    {
      using var lifetimeDefinition = new LifetimeDefinition();
      var lifetime = lifetimeDefinition.Lifetime;

      var priorityAdvice = 0;
      var advice1 = 0;
      var advice2 = 0;
      
      var signal = new Signal<Unit>();
      var lifetimes = new SequentialLifetimes(TestLifetime);
      signal.Advise(lifetime, _ =>
      {
        advice1++;
        using (Signal.PriorityAdviseCookie.Create())
        {
          signal.Advise(lifetimes.Next(), _ => priorityAdvice++);
        }
      });
      
      signal.Advise(lifetime, _ => advice2++);

      for (int i = 0; i < 1000; i++)
      {
        signal.Fire();
      
        Assert.AreEqual(i + 1, advice1);
        Assert.AreEqual(i + 1, advice2);
        Assert.AreEqual(i, priorityAdvice);
      }
    }

    [Test, Explicit("This test doesn't actually check anything, just show some measurements")]
    //before
    //net472
    // Signal: 572 ms
    // event: 5 ms
    //net8.0
    // Signal: 222 ms
    // event: 3 ms
    //after
    //net472
    // Signal: 65 ms without aggressive inlining and 23 with aggressive inlining on JetBrains.Lifetimes.LifetimedList<T>.GetSnapshot
    // event: 5 ms
    //net8.0
    // Signal: 18 ms
    // event: 3 ms
    [TestCase(1)]
    //before
    //net472
    // Signal: 608 ms
    // event: 47 ms
    //net8.0
    // Signal: 276 ms
    // event: 50 ms
    //after
    //net472
    // Signal: 80 ms without aggressive inlining and 44 ms with aggressive inlining on JetBrains.Lifetimes.LifetimedList<T>.GetSnapshot 
    // event: 49 ms
    //net8.0
    // Signal: 34 ms
    // event: 50 ms
    [TestCase(2)]
    public void PerfTest(int count)
    {
      for (int i = 0; i < 100; i++)
      {
        using var def = new LifetimeDefinition();
        var holder = new Holder();
        for (int j = 0; j < count; j++)
        {
          holder.Subscribe(def.Lifetime);
        }

        var n = 10_000_000;
        var signalMs = TestSignal(n, holder);
        Console.WriteLine($"Signal: {signalMs} ms");

        var eventMs = TestEvent(n, holder);
        Console.WriteLine($"event: {eventMs} ms");
      }
    }

    [MethodImpl(MethodImplOptions.NoInlining)]
    private static long TestSignal(int n, Holder holder)
    {
      var stopwatch = LocalStopwatch.StartNew();
      for (int j = 0; j < n; j++)
      {
        holder.Signal.Fire(j);
      }

      return stopwatch.ElapsedMilliseconds;
    }
    [MethodImpl(MethodImplOptions.NoInlining)]
    private static long TestEvent(int n, Holder holder)
    {
      var stopwatch = LocalStopwatch.StartNew();
      for (int j = 0; j < n; j++)
      {
        holder.Fire(j);
      }

      return stopwatch.ElapsedMilliseconds;
    }

    private class Holder
    {
      public Signal<int> Signal = new Signal<int>();
      public event Action<int> Event;
      
      public void Fire(int value)
      {
        Event(value);
      }

      public void Subscribe(Lifetime lifetime)
      {
        var a = 0;
        Signal.Advise(lifetime, _ => a++);
        Event += _ => a++;
      }
    }
  }
}