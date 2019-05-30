using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.RdCore.Lifetimes
{
  [TestFixture]
  public class TestLifetime : RdCoreTestBase
  {
    [Test]
    public void T000_Items()
    {
      int count = 0;
      Lifetime.Using(lifetime =>
      {
        lifetime.AddAction(() => count++);
        lifetime.AddDispose(Disposable.CreateAction(() => count++));
        lifetime.OnTermination(() => count++);
        lifetime.OnTermination(Disposable.CreateAction(() => count++));
      });

      Assert.AreEqual(4, count, "Mismatch.");
    }

    [Test]
    public void T010_SimpleOrder()
    {
      var entries = new List<int>();
      int x= 0 ;
      Lifetime.Using(lifetime =>
      {
        int a = x++;
        lifetime.AddAction(() => entries.Add(a));
        int b = x++;
        lifetime.AddDispose(Disposable.CreateAction(() => entries.Add(b)));
        int c = x++;
        lifetime.OnTermination(Disposable.CreateAction(() => entries.Add(c)));
        int d = x++;
        lifetime.AddDispose(Disposable.CreateAction(() => entries.Add(d)));
      });

      CollectionAssert.AreEqual(System.Linq.Enumerable.Range(0, entries.Count).Reverse().ToArray(), entries, "Order FAIL.");
    }

    [Test]
    public void T020_DefineNestedOrder()
    {
      var entries = new List<int>();
      int x= 0 ;

      Func<Action> FMakeAdder = () => { var a = x++; return () => entries.Add(a); };  // Fixes the X value at the moment of FMakeAdder call.

      bool flag = false;

      Lifetime.Using(lifetime =>
      {
        lifetime.AddAction(FMakeAdder());
        lifetime.AddDispose(Disposable.CreateAction(FMakeAdder()));
        Lifetime.Define(lifetime, atomicAction:(lifeNested) => { lifeNested.AddAction(FMakeAdder()); lifeNested.AddAction(FMakeAdder()); lifeNested.AddAction(FMakeAdder());});
        lifetime.AddDispose(Disposable.CreateAction(FMakeAdder()));
        Lifetime.Define(lifetime, atomicAction:(lifeNested) => { lifeNested.AddAction(FMakeAdder()); lifeNested.AddAction(FMakeAdder()); lifeNested.AddAction(FMakeAdder());});
        lifetime.AddDispose(Disposable.CreateAction(FMakeAdder()));
        Lifetime.Define(lifetime, atomicAction:(lifeNested) => lifeNested.AddAction(() => flag = true)).Terminate();
        Assert.IsTrue(flag, "Nested closing FAIL.");
        flag = false;
        lifetime.AddDispose(Disposable.CreateAction(FMakeAdder()));
      });

      Assert.IsFalse(flag, "Nested closed twice.");

      CollectionAssert.AreEqual(System.Linq.Enumerable.Range(0, entries.Count).Reverse().ToArray(), entries, "Order FAIL.");
      
    }

#if !NET35
    [Test]
    public void CancellationTokenTest()
    {
      var def = Lifetime.Define();      
      
      var sw = new SpinWait();
      var task = Task.Run(() =>
      {
        while (true)
        {
          def.Lifetime.ThrowIfNotAlive();
          sw.SpinOnce();
        }
      }, def.Lifetime);
      
      Thread.Sleep(100);
      def.Terminate();

      try
      {
        task.Wait();
      }
      catch (AggregateException e)
      {
        Assert.True(task.IsCanceled);
        Assert.True(e.IsOperationCanceled());
        return;
      }          

      Assert.Fail("Unreachable");
    }
    
    
    [Test]
    public void CancellationTokenTestAlreadyCancelled()
    {
      var def = Lifetime.Define();
      def.Terminate();
      
      var task = Task.Run(() =>
      {
        Assertion.Fail("Unreachable");
      }, def.Lifetime);

      Assert.Throws<AggregateException>(() => task.Wait());
      
      Assert.True(task.IsCanceled);
    }
    
    [Test]
    public void TestCancellationEternalLifetime()
    {
      var lt = Lifetime.Eternal;
      
      var task = Task.Run(() =>
      {
        lt.ThrowIfNotAlive();
        Thread.Yield();        
      }, lt);

      task.Wait();
      
      Assert.True(task.Status == TaskStatus.RanToCompletion);
    }
#endif
  }
}