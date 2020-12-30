using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
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

    [Test]
    public void PriorityItemTest()
    {
      using var lifetimeDefinition = new LifetimeDefinition();
      var lifetime = lifetimeDefinition.Lifetime;
      
      var list = new LifetimedList<int>();
      list.Add(lifetime, 1);
      list.Add(lifetime, 2);

      using (var enumerator = list.GetEnumerator())
      {
        Assert.IsTrue(enumerator.MoveNext());
        Assert.AreEqual(1, enumerator.Current.Value);
      
        list.AddPriorityItem(lifetime, 3);

        Assert.IsTrue(enumerator.MoveNext());
        Assert.AreEqual(2, enumerator.Current.Value);

        Assert.IsFalse(enumerator.MoveNext());
      }
      
      using (var enumerator = list.GetEnumerator())
      {
        Assert.IsTrue(enumerator.MoveNext());
        Assert.AreEqual(3, enumerator.Current.Value);
      
        list.AddPriorityItem(lifetime, 4);

        Assert.IsTrue(enumerator.MoveNext());
        Assert.AreEqual(1, enumerator.Current.Value);

        Assert.IsTrue(enumerator.MoveNext());
        Assert.AreEqual(2, enumerator.Current.Value);

        Assert.IsFalse(enumerator.MoveNext());
      }
    }

    [Test]
    public void EnumerationStressTest()
    {
      var hugeStruct = HugeStruct.Create();

      for (var k = 0; k < 300; k++)
      {
        using var lifetimeDefinition = new LifetimeDefinition();
        var lifetime = lifetimeDefinition.Lifetime;
        
        var list = new LifetimedList<HugeStruct>();
        // ReSharper disable once MethodSupportsCancellation
        var task = Task.Factory.StartNew(() =>
        {
          for (var j = 0; j < 3; j++)
          {
            Task.Factory.StartNew(() =>
            {
              // ReSharper disable once AccessToDisposedClosure
              using (lifetimeDefinition)
              {
                while (lifetime.IsAlive)
                  list.ClearValuesIfNotAlive();
              }
            }, TaskCreationOptions.AttachedToParent | TaskCreationOptions.LongRunning);

            Task.Factory.StartNew(() =>
            {
              // ReSharper disable once AccessToDisposedClosure
              using (lifetimeDefinition)
              {
                while (lifetime.IsAlive)
                {
                  var copy = new List<ValueLifetimed<HugeStruct>>(); // fast enumeration
                  foreach (var lifetimed in list) copy.Add(lifetimed);

                  foreach (var value in copy)
                  {
                    value.Value.AssertValues();
                    if (lifetime.IsNotAlive)
                      return;
                  }
                }
              }
            }, TaskCreationOptions.AttachedToParent | TaskCreationOptions.LongRunning);
            
            Task.Factory.StartNew(() =>
            {
              // ReSharper disable once AccessToDisposedClosure
              using (lifetimeDefinition)
              {
                while (lifetime.IsAlive)
                {
                  foreach (var value in list) // slow enumeration
                  {
                    value.Value.AssertValues();
                    if (lifetime.IsNotAlive)
                      return;
                    
                    Thread.Sleep(1);
                  }
                }
              }
            }, TaskCreationOptions.AttachedToParent | TaskCreationOptions.LongRunning);
          }
        });

        Parallel.Invoke(() =>
        {
          using (lifetimeDefinition)
          {
            for (var i = 0; i < 100; i++)
            {
              using var localDef = lifetime.CreateNested();
              for (var j = 0; j < i; j++)
              {
                list.Add(localDef.Lifetime, hugeStruct);
              }

              if (lifetime.IsNotAlive)
                break;
            }
          }
        }, () =>
        {
          var spinWait = new SpinWait();
          for (var i = 0; i < 100; i++)
          {
            list.AddPriorityItem(lifetime, hugeStruct);
            if (lifetime.IsNotAlive)
              break;
            spinWait.SpinOnce();
          }
        });

        task.Wait();
      }
    }
    
    private struct HugeStruct
    {
      public long Value0;
      public long Value1;
      public long Value2;
      public long Value3;
      public long Value4;
      public long Value5;
      public long Value6;
      public long Value7;
      public long Value8;
      public long Value9;
      public long Value10;
      public long Value11;
      public long Value12;
      public long Value13;
      public long Value14;
      public long Value15;
      public long Value16;
      public long Value17;
      public long Value18;
      public long Value19;
      public long Value20;
      public long Value21;
      public long Value22;
      public long Value23;
      public long Value24;
      public long Value25;
      public long Value26;
      public long Value27;
      public long Value28;
      public long Value29;

      public static unsafe HugeStruct Create()
      {
        var instance = new HugeStruct();
        Init(&instance);
        instance.AssertValues();
        return instance;
      }

      private static unsafe void Init(HugeStruct* ptr)
      {
        var fieldInfos = typeof(HugeStruct).GetFields();
        var longPtr = (long*) ptr;
        for (var i = 0; i < fieldInfos.Length; i++)
        {
          *(longPtr + i) = long.MaxValue;
        }
      }

      public void AssertValues()
      {
        foreach (var fieldInfo in typeof(HugeStruct).GetFields())
        {
          Assert.AreEqual(long.MaxValue, fieldInfo.GetValue(this));
        }
      }
    }
  }
}