using System;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Threading;
using NUnit.Framework;
using Lifetime = JetBrains.Lifetimes.Lifetime;

namespace Test.Lifetimes.Collections
{
  public class BlockingPriorityQueueTest
  {
    [Test]
    public void ExtractOrBlockStressTest()
    {
      for (int j = 0; j < 50; j++)
      {
        var t = Lifetime.Using(lf =>
        {
          Exception exception = null;
          var queue = new BlockingPriorityQueue<PrioritizedAction>(lf);
          var count = 0;
          var task = StartThreads();

          const int n = 10000;

          for (int i = 0; i < n; i++)
          {
            queue.Enqueue(new PrioritizedAction(() =>
            {
              Interlocked.Increment(ref count);
              queue.Enqueue(new PrioritizedAction(() => { Interlocked.Increment(ref count); }));
            }));
          }

          SpinWait.SpinUntil(() => Volatile.Read(ref count) == n * 2 || Volatile.Read(ref exception) != null, TimeSpan.FromMinutes(1));
          Assert.IsNull(Volatile.Read(ref exception));
          Assert.AreEqual(n * 2, Volatile.Read(ref count));
          return task;



          Task StartThreads()
          {
            var currentCount = 0;
            var threadsCount = Math.Max(2, Environment.ProcessorCount);
            var tasks = Enumerable.Range(0, threadsCount).Select(x =>
            {
              return Task.Factory.StartNew(() =>
              {
                Interlocked.Increment(ref currentCount);
                while (lf.IsAlive)
                {
                  try
                  {
                    var action = queue.ExtractOrBlock();
                    action.Action();
                  }
                  catch (Exception e) when (e.IsOperationCanceled() && lf.IsNotAlive)
                  {
                    // ok
                  }
                  catch (Exception e)
                  {
                    Volatile.Write(ref exception, e);
                    throw;
                  }
                }
              });
            }).ToArray();

            SpinWait.SpinUntil(() => Volatile.Read(ref currentCount) == threadsCount, TimeSpan.FromMinutes(1));
            Assert.AreEqual(threadsCount, Volatile.Read(ref currentCount));

            return Task.WhenAll(tasks);
          }
        });

        Assert.IsTrue(t.Wait(TimeSpan.FromSeconds(10)), "t.Wait(TimeSpan.FromSeconds(10))");
      }
    }
  }
}