using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Util.Internal;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Viewable
{
  public class WriteOncePropertyTest : LifetimesTestBase
  {
    [Test]
    public void SimpleTest()
    {
      using var def = new LifetimeDefinition();
      var lifetime = def.Lifetime;
      var prop = new WriteOnceProperty<int>();

      Assert.Throws<InvalidOperationException>(() => { var _ = prop.Value; });

      var v1 = 0;
      prop.Advise(lifetime, value => v1 = value);

      Assert.AreEqual(0, v1);
      Assert.IsFalse(prop.Maybe.HasValue);

      Assert.IsTrue(prop.SetIfEmpty(1));
      Assert.AreEqual(1, v1);
      Assert.AreEqual(1, prop.Value);

      Assert.IsFalse(prop.SetIfEmpty(2));
      Assert.AreEqual(1, v1);
      Assert.AreEqual(1, prop.Value);

      Assert.Throws<InvalidOperationException>(() => prop.Value = 2);

      Assert.AreEqual(1, v1);
      Assert.AreEqual(1, prop.Value);


      var v3 = 0;
      prop.Advise(lifetime, value => v3 = value);

      Assert.AreEqual(1, v1);
      Assert.AreEqual(1, v3);
      Assert.AreEqual(1, prop.Value);

      Assert.AreEqual(1, v1);
      Assert.AreEqual(1, v3);
      Assert.AreEqual(1, prop.Value);

      prop.fireInternal(3);

      Assert.AreEqual(1, v1);
      Assert.AreEqual(1, v3);
      Assert.AreEqual(1, prop.Value);
    }

    [Test]
    public void ConcurrentWriteTest()
    {
      const int threadsCount = 10;

      for (int i = 0; i < 200; i++)
      {
        using var def = new LifetimeDefinition();
        var lifetime = def.Lifetime;

        var prop = new WriteOnceProperty<int>();
        var value1 = new AtomicValue();
        
        prop.Advise(lifetime, v =>
        {
          if (!value1.SetIfDefault(v))
            Assert.Fail("Handler must not be called twice");
        });

        var value2 = new AtomicValue();
        var count = 0;

        var tasks = Enumerable.Range(0, threadsCount).Select(j => Task.Factory.StartNew(() =>
        {
          Interlocked.Increment(ref count);
          SpinWait.SpinUntil(() => Memory.VolatileRead(ref count) == threadsCount); // sync threads
          
          if (!prop.SetIfEmpty(j)) return;

          if (!value2.SetIfDefault(j))
            Assert.Fail("Value myst be written once");
          
        }, lifetime)).ToArray();

        Assert.IsTrue(Task.WaitAll(tasks, TimeSpan.FromMinutes(1)), "Task.WaitAll(tasks, TimeSpan.FromMinutes(1))");


        value1.AssertNonDefault();
        value2.AssertNonDefault();

        Assert.AreEqual(value1.Value, value2.Value);
        Assert.AreEqual(value1.Value, prop.Value);
        
        prop.fireInternal(1000);
      }
    }

    [Test]
    public void ConcurrentWriteAndAdviseTest()
    {
      const int threadsCount = 10;

      for (int i = 0; i < 200; i++)
      {
        using var def = new LifetimeDefinition();
        var lifetime = def.Lifetime;
        
        var prop = new WriteOnceProperty<int>();

        var value1 = new AtomicValue();
        var count = 0;

        var tasks = Enumerable.Range(0, threadsCount).Select(j => Task.Factory.StartNew(() =>
        {
          Interlocked.Increment(ref count);
          SpinWait.SpinUntil(() => Memory.VolatileRead(ref count) == threadsCount); // sync threads
          
          if (!prop.SetIfEmpty(j)) return;
          
          if (!value1.SetIfDefault(j))
            Assert.Fail("Value must be written once");
          
        }, lifetime)).ToArray();

        var values = Enumerable.Range(0, i).Select(j =>
        {
          var localValue = new AtomicValue();

          prop.Advise(lifetime, v =>
          {
            if (!localValue.SetIfDefault(v))
              Assert.Fail("Handled must not be called twice");
          });
          return localValue;
        }).ToArray();

        Assert.IsTrue(Task.WaitAll(tasks, TimeSpan.FromMinutes(1)), "Task.WaitAll(tasks, TimeSpan.FromMinutes(1))");
        
        value1.AssertNonDefault();
        
        if (values.Length != 0)
        {
          var value = values.Select(x => x.Value).Distinct().Single();
          Assert.AreEqual(value, value1.Value);
        }
        
        Assert.AreEqual(value1.Value, prop.Value);
        
        prop.fireInternal(10000);
      }
    }
    
    private class AtomicValue
    {
      private const int Default = -1;
      private volatile int myValue = -1;

      public int Value => myValue;

      public bool SetIfDefault(int value)
      {
        return Interlocked.CompareExchange(ref myValue, value, Default) == Default;
      }

      public void AssertNonDefault() => Assert.AreNotEqual(Default, myValue);
    }
  }
}