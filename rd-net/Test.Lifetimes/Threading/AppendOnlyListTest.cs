using System;using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.Lifetimes;
using JetBrains.Threading;
using JetBrains.Util.Internal;
using NUnit.Framework;

namespace Test.Lifetimes.Threading;

public class AppendOnlyListTest
{
  [Test]
  public void SimpleTryAddTest()
  {
    var list = new AppendOnlyList<int>(1, 10);
    for (var i = 0; i < 10; i++) 
      Assert.IsTrue(list.TryAppend(i));

    Assert.IsFalse(list.TryAppend(10));

    var index = 0;
    foreach (var i in list) 
      Assert.AreEqual(index++, i);
    
    Assert.AreEqual(10, list.Count);
  }

  [Test]
  public void SimpleIndexerTest()
  {
    var list = new AppendOnlyList<int>(1);
    var n = 1000;
    for (var i = 0; i < n; i++)
    {
      list.TryAppend(i);
      Assert.AreEqual(i, list[i]);
    }

    for (var i = 0; i < n; i++) 
      Assert.AreEqual(i, list[i]);

    for (var i = 0; i < n; i++)
    {
      if (i % 2 == 0)
      {
        Assert.AreEqual(i, list[i]);
      }
      else
      {
        var index = n - i - 1;
        Assert.AreEqual(index, list[index]);
      }
    }
  }

  [Test]
  public void SimpleEnumerationTest()
  {
    var list = new AppendOnlyList<int>(1);
    Assert.AreEqual(0 ,list.ToList().Count);

    list.TryAppend(1);
    Assert.AreEqual(1, list.ToList().Count);
    Assert.AreEqual(1, list.ToList().Single());
    
    
    list.TryAppend(2);
    Assert.AreEqual(2, list.ToList().Count);
    Assert.IsTrue(list.ToList().SequenceEqual(new []{1,2}));
    
    list = new AppendOnlyList<int>(1);
    for (var i = 0; i < 1000; i++)
    {
      using var enumerator = list.GetEnumerator();
      Assert.IsTrue(list.TryAppend(i));
      
      for (var j = 0; j < i; j++) 
        Assert.IsTrue(enumerator.MoveNext());
      
      Assert.IsFalse(enumerator.MoveNext());

      Assert.IsTrue(list.ToList().SequenceEqual(Enumerable.Range(0, i + 1)));
    }
  }

  [Test]
  public void ConcurrentTryAddTest()
  {
    for (var i = 0; i < 100; i++)
    {
      const int n = 1235;
      const int threads = 5;
      const int itemsPerThread = n / threads;
      
      var list = new AppendOnlyList<int>(1, n);

      var tasks = Enumerable.Range(0, threads).Select(x => Task.Factory.StartNew(() =>
      {
        var start = itemsPerThread * x;
        for (var item = start; item < start + itemsPerThread; item++) 
          Assert.IsTrue(list.TryAppend(item));
      })).ToArray();

      Task.WaitAll(tasks);
      
      Assert.IsFalse(list.TryAppend(0));

      Assert.AreEqual(n, list.Count);

      var bools = new bool[n];
      foreach (var value in list)
      {
        if (bools[value])
          Assert.Fail();

        bools[value] = true;
      }
    }
  }
  
  [Test]
  public void ConcurrentTryAddMaxCountTest()
  {
    for (var i = 0; i < 100; i++)
    {
      const int n = 1235;
      const int threads = 5;
      
      var list = new AppendOnlyList<int>(1, n);
      var tasks = Enumerable.Range(0, threads).Select(_ => Task.Factory.StartNew(() =>
      {
        var index = 0;
        var localList = new List<int>();
        while (true)
        {
          if (list.TryAppend(index))
            localList.Add(index++);
          else
          {
            Assert.IsTrue(list.Count == n);
            return localList;
          }
        }
      })).ToArray();

      Task.WaitAll(tasks);
      
      Assert.IsFalse(list.TryAppend(0));

      var expected = tasks.Select(x => x.Result).SelectMany(x => x).ToList();
      Assert.AreEqual(expected.Count, list.Count);
      expected.Sort();
      
      var copyList = list.ToList();
      copyList.Sort();
      
      for (var index = 0; index < copyList.Count; index++) 
        Assert.IsTrue(expected[index] == copyList[index]);

      var ints = new List<int>();
      foreach (var value in list) 
        ints.Add(value);

      ints.Sort();
      
      for (var index = 0; index < copyList.Count; index++) 
        Assert.IsTrue(expected[index] == ints[index]);
    }
  }

  [Test]
  public void ConcurrentWriteReadTest()
  {
    var taskArray = Lifetime.Using(lifetime =>
    {
      var o = new object();
      var list = new AppendOnlyList<object>(1);

      var tasks = Enumerable.Range(0, Environment.ProcessorCount).Select(x => Task.Factory.StartNew(() =>
      {
        switch (x % 3)
        {
          case 0:
          {
            while (lifetime.IsAlive)
            {
              var copy = Memory.VolatileRead(ref list).ToList();
              foreach (var o in copy)
                Assert.NotNull(o);
            }

            break;
          }
          case 1:
          {
            while (lifetime.IsAlive)
            {
              var local = Memory.VolatileRead(ref list);
              foreach (var o in local)
                Assert.NotNull(o);
            }

            break;
          }
          case 2:
          {
            while (lifetime.IsAlive)
            {
              var local = Memory.VolatileRead(ref list);
              for (var index = 0; index < local.Count; index++)
              {
                var o = local[index];
                Assert.NotNull(o);
              }
            }

            break;
          }
        }
      })).ToArray();

      for (var i = 0; i < 50_000; i++)
      {
        for (var j = 0; j < 5; j++)
          list.TryAppend(o);

        list = new AppendOnlyList<object>(1);
      }

      return tasks;
    });

    Task.WaitAll(taskArray);
  }

  [Test]
  public void SimpleFreezeTest()
  {
    for (var i = 1; i <= 10; i++)
    {
      var list = new AppendOnlyList<int>(1);
      for (var j = 0; j < i; j++) 
        Assert.IsTrue(list.TryAppend(j));
      
      list.Freeze();
      Assert.IsFalse(list.TryAppend(i));
    }
  }

  [Test]
  public void FreezeStressTest()
  {
    const int maxLength = 50;
    var sharedList = new AppendOnlyList<int>(1, maxLength);
    var tasks = Enumerable.Range(0, 5).Select(_ => Task.Factory.StartNew(() =>
    {
      var spinner = new SpinWaitEx();
      while (true)
      {
        var list = Memory.VolatileRead(ref sharedList);
        if (list == null)
          return;

        while (list.TryAppend(0)) 
          spinner.SpinOnce(false);
      }
    })).ToArray();
    
    var values = new List<KeyValuePair<int, AppendOnlyList<int>>>();
    for (var i = 0; i < 1000; i++)
    for (var j = 1; j <= 32; j++)
    {
      var copy = sharedList;
      var count = copy.Freeze();
      if (count == maxLength)
        continue;

      values.Add(new KeyValuePair<int, AppendOnlyList<int>>(count, copy));

      Memory.VolatileWrite(ref sharedList, new AppendOnlyList<int>(j, maxLength));
    }

    Memory.VolatileWrite(ref sharedList, null);

    Task.WaitAll(tasks);
    Console.WriteLine(values.Count);
    Assert.IsTrue(values.All(x =>x.Value.IsFrozen), "values.All(x =>x.Value.IsFrozen)");
    Assert.IsTrue(values.All(x => x.Key < maxLength && x.Key == x.Value.Count), "huita");
  }
}

