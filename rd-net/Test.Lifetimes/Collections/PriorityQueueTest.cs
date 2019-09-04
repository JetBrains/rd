using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Collections;
using JetBrains.Lifetimes;
using JetBrains.Util;
using NUnit.Framework;

namespace Test.Lifetimes.Collections
{
  [TestFixture]
  public class PriorityQueueTest : LifetimesTestBase
  {
    [Test]
    public void TestEnumerator01()
    {
      var q = new JetPriorityQueue<int>();
      Assert.IsTrue(EmptyArray<int>.Instance.SequenceEqual(q), "EmptyArray<int>.Instance.SequenceEqual(q)");
    }

    [Test]
    public void TestEnumerator02()
    {
      var q = new JetPriorityQueue<int>();
      q.Add(1);
      q.Add(2);
      q.Add(3);
      Assert.IsTrue(new int[] { 1, 2, 3}.SequenceEqual(q), "new int[] {{ 1, 2, 3}}.SequenceEqual(q)");
    }

    [Test]
    public void TestNoComparer()
    {
      var q = new JetPriorityQueue<int>();
      Assert.AreEqual(0, q.Count);
      Assert.True(!q.Any());
      q.Add(5);
      q.Add(3);
      q.Add(4);
      q.Add(1);
      q.Add(2);
      Assert.AreEqual(5, q.Count);
      Assert.False(!q.Any());


      AssertExtract(1, q);
      AssertExtract(2, q);
      AssertExtract(3, q);
      Assert.AreEqual(2, q.Count);

      q.Add(5);
      q.Add(3);
      Assert.AreEqual(4, q.Count);

      AssertExtract(3, q);
      q.Add(1);
      AssertExtract(1, q);


      AssertExtract(4, q);
      AssertExtract(5, q);
      AssertExtract(5, q);

      int x;
      Assert.False(q.TryPeek(out x));
      Assert.False(q.TryExtract(out x));      
      Assert.AreEqual(0, q.ExtractOrDefault());
    }


    class Timed
    {
      internal string Name { get; private set; }
      internal DateTime Time { get; private set; }

      public Timed(string name, DateTime time)
      {
        Name = name;
        Time = time;
      }

      internal class Comparer : IComparer<Timed>
      {
        public int Compare(Timed x, Timed y)
        {
          if (x.Time > y.Time) return 1;
          if (x.Time < y.Time) return -1;
          return 0;
        }
      }
    }

    [Test]
    public void TestWithDateComparer()
    {
      var q = new BlockingPriorityQueue<Timed>(Lifetime.Eternal, 10, new Timed.Comparer());

      for (int i = 0; i < 10; i++)
      {
        q.Add(new Timed(i.ToString(), DateTime.Now+TimeSpan.FromSeconds(10-i)));
        q.Add(new Timed(i.ToString(), DateTime.Now+TimeSpan.FromSeconds(10-i)));
      }
      Assert.AreEqual("9", q.Extract().Name);
      Assert.AreEqual("9", q.Extract().Name);
      Assert.AreEqual("8", q.Extract().Name);
      Assert.AreEqual("8", q.Extract().Name);
      Assert.AreEqual("7", q.Extract().Name);
      Assert.AreEqual("7", q.Extract().Name);
      Assert.AreEqual("6", q.Extract().Name);
      Assert.AreEqual("6", q.Extract().Name);
      Assert.AreEqual("5", q.Extract().Name);
      Assert.AreEqual("5", q.Extract().Name);
      Assert.AreEqual("4", q.Extract().Name);
      Assert.AreEqual("4", q.Extract().Name);
      Assert.AreEqual("3", q.Extract().Name);
      Assert.AreEqual("3", q.Extract().Name);
      Assert.AreEqual("2", q.Extract().Name);
      Assert.AreEqual("2", q.Extract().Name);
      Assert.AreEqual("1", q.Extract().Name);
      Assert.AreEqual("1", q.Extract().Name);
      Assert.AreEqual("0", q.Extract().Name);
      Assert.AreEqual("0", q.Extract().Name);

    }

    private class DateTimeComparer : IComparer<DateTime>
    {
      public int Compare(DateTime x, DateTime y)
      {
        if (x > y) return 1;
        if (x < y) return -1;
        return 0;
      }
    }

    [Test]
    public void TestTimedQueue()
    {
      var queue = new JetPriorityQueue<DateTime>(10, new DateTimeComparer());

      for (int seed = 0; seed < 1000; seed++)
      {
        var random = new Random(seed);

        var now = DateTime.Now;
        for (int i = 0; i < 11; i++)
          queue.Add(now + TimeSpan.FromMilliseconds(random.Next(0, 1000)));

        var lastDateTime = DateTime.MinValue;
        DateTime res;
        while (queue.TryPeek(out res))
        {
          res = queue.Extract();
          Assert.LessOrEqual(lastDateTime, res, string.Format("Count = {0}, Seed = {1}", queue.Count, seed));

          lastDateTime = res;
        }
      }
    }

    [Test]
    public void TestQueueDifferentSizes()
    {
      for (var size = 0; size <= 239; size++)
      {
        var queue = new JetPriorityQueue<int>(size);

        for (var seed = 0; seed < 100; seed++)
        {
          var random = new Random(seed);

          var source = new List<int>(size);
          for (var i = 0; i < size; i++)
            source.Add(random.Next(0, 1000));

          for (var i = 0; i < size; i++)
          {
            queue.Add(source[i]);
            Assert.AreEqual(queue.Count, i+1, "Add failed.");
          }

          int prevVal = -1;
          int peekVal;
          for (var i = 0; i < size; i++)
          {
            Assert.True(queue.TryPeek(out peekVal), "TryPeek failed.");

            var curVal = queue.Extract();
            Assert.AreEqual(peekVal, curVal, "Peek/Extract mismatch.");

            Assert.LessOrEqual(prevVal, curVal, string.Format("Values are not ordered (Size = {0}, Seed = {1}, Count = {2}).", size, seed, queue.Count));
            prevVal = curVal;
          }
          Assert.False(queue.TryPeek(out peekVal), "TryPeek failed.");
        }
      }
    }


    [Test]
    public void TestStable()
    {
      var q = new JetPriorityQueue<string>();
      var a1 = new string('a', 1);
      var a2 = new string('a', 1);
      var a3 = new string('a', 1);
      var a4 = new string('a', 1);
      q.Add(a1);
      q.Add(a2);
      q.Add(a3);
      q.Add(a4);

      Assert.True(ReferenceEquals(a1, q.Extract()));
      Assert.True(ReferenceEquals(a2, q.Extract()));
      Assert.True(ReferenceEquals(a3, q.Extract()));
      Assert.True(ReferenceEquals(a4, q.Extract()));
    }

    [Test]
    public void TestFailedQueueAt()
    {
      int[] data =
      {
        235,
        21,
        222,
        163,
        522,
        884,
        68,
        17,
        544,
        867,
        444
      };

      var q = new JetPriorityQueue<int>();
      foreach (var i in data)
      {
        q.Add(i);
      }

      var res = new List<int>();
      while (!q.Any())
      {
        res.Add(q.Extract());
      }
      Console.WriteLine(string.Join(",", res.Select(x => x.ToString()).ToArray()) + " : " + res.Count);
    }

    private void AssertExtract<T>(T x, IPriorityQueue<T> q)
    {
      T xx;
      Assert.True(q.TryPeek(out xx));
      Assert.AreEqual(x, xx);

      Assert.True(q.TryExtract(out xx));
      Assert.AreEqual(x, xx);
    }

  }
}