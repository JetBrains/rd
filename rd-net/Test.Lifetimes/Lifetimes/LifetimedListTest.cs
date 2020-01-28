using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.Lifetimes.Lifetimes
{
  public class LifetimedListTest : LifetimesTestBase
  {
    [Test]
    public void TestSimple()
    {
      var lst = new LifetimedList<int>();
      
      var l1 = new LifetimeDefinition();
      l1.Lifetime.OnTermination(() => lst.ClearValuesIfNotAlive());
      lst.Add(l1.Lifetime, 1);
      lst.Add(l1.Lifetime, 2);
      
      var l2 = new LifetimeDefinition();
      l2.Lifetime.OnTermination(() => lst.ClearValuesIfNotAlive());
      lst.AddPriorityItem(new ValueLifetimed<int>(l2.Lifetime, 3));
      lst.AddPriorityItem(new ValueLifetimed<int>(l2.Lifetime, 4));
      
      Assert.AreEqual(new List<int> {3, 4, 1, 2}, lst.Select(x => x.Value).ToList());
      
      l1.Terminate();
      Assert.AreEqual(new List<int> {3, 4}, lst.Select(x => x.Value).ToList());
      
      lst.Add(l2.Lifetime, 5);
      Assert.AreEqual(new List<int> {3, 4, 5}, lst.Select(x => x.Value).ToList());
      
      l2.Terminate();
      Assert.AreEqual(new List<int> {}, lst.Select(x => x.Value).ToList());
      
      //again
      l1 = new LifetimeDefinition();
      l2 = new LifetimeDefinition();
      
      lst.Add(l1.Lifetime, 1);
      Assert.AreEqual(new List<int> {1}, lst.Select(x => x.Value).ToList());
      
      lst.AddPriorityItem(new ValueLifetimed<int>(l2.Lifetime, 2));
      Assert.AreEqual(new List<int> {2, 1}, lst.Select(x => x.Value).ToList());
    }

    [Test]
    public void TestReentrancyInEnumeration()
    {
      var lst = new LifetimedList<Action>();
      var l1 = new LifetimeDefinition { Id = "l1"};
      var l2 = new LifetimeDefinition { Id = "l2"};
      l2.Lifetime.OnTermination(() => lst.ClearValuesIfNotAlive());

      var log = new List<int>();
      lst.Add(l1.Lifetime, () =>
      {
        log.Add(1);
        lst.Add(l1.Lifetime, () =>
        {
          log.Add(2);
        });
        l2.Terminate();
      });

      lst.Add(l2.Lifetime, () => { log.Add(3); });

      void Enumerate()
      {
        foreach (var (lf, action) in lst)
        {
          if (lf.IsNotAlive) continue;
          action.Invoke();
        }
      }
      
      Enumerate();
      Assert.AreEqual(new List<int> {1}, log);
      log.Clear();
      
      Enumerate();
      Assert.AreEqual(new List<int> {1, 2}, log);
      log.Clear();


      Enumerate();
      Assert.AreEqual(new List<int> {1, 2, 2}, log);
      
    }
  }
}