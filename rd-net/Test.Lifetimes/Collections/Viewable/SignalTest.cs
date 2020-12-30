using System;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Lifetimes;
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
  }
}