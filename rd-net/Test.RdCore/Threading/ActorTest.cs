using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Lifetimes;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.RdCore.Threading
{
#if !NET35
  public class ActorTest : RdCoreTestBase
  {
    [Test]
    public void TestBackgroundThreadActor()
    {
      var log = new List<int>();
      var def = new LifetimeDefinition();
      var actor = new Actor<int>("TestActor", def.Lifetime, async x =>
      {
        log.Add(x);
        await Task.Yield();
        log.Add(-x);
      });
      
      Assert.True(actor.IsEmpty);
      actor.SendBlocking(1);
      actor.SendBlocking(2);
      actor.SendBlocking(3);
      actor.WaitForEmpty();
      Assert.True(actor.IsEmpty);

      var expected = new List<int> {1, -1, 2, -2, 3, -3};
      Assert.AreEqual(expected, log);
      def.Terminate();

      Assert.True(actor.SendAsync(4).IsCanceled);
      Assert.AreEqual(expected, log);
    }

    [Test]
    public void TestSeveralThreads()
    {
      long sum = 0;
      const int nThreads = 10;
      const long limit = 10000;
      var actor = new Actor<int>("TestActor", TestLifetime,  x => { sum += x; });
      var tasks = new Task[nThreads];
      for (int i = 0; i < 10; i++)
      {
        tasks[i] = Task.Run(() =>
        {
          for (int j = 0; j < limit; j++)
          {
            actor.SendBlocking(j);
          }
        });
      }

      Task.WaitAll(tasks);
      actor.WaitForEmpty();
      
      Assert.AreEqual(nThreads * (limit - 1) * limit / 2, sum);
    }

    [Test]
    public void TestEmpty()
    {
      var def = new LifetimeDefinition();

      int sum = 0;
      var e = new AutoResetEvent(false);
      var actor = new Actor<int>("TestActor", def.Lifetime,  x =>
      {
        e.WaitOne(); //will deadlock if continuations are not async
        sum += 1;
      }, maxQueueSize: 0);

      for (int i = 0; i < 10; i++)
      {
        actor.SendBlocking(i);
        Assert.False(actor.IsEmpty);
        e.Set();
      }
      actor.WaitForEmpty();
      Assert.AreEqual(10, sum);
      
      def.Terminate();
      Assert.Throws<AggregateException>(() => actor.SendBlocking(0));
      Assert.True(actor.IsEmpty);
    }

    [Test]
    public void TestRecursive()
    {
      const int n = 10;
      int count = n;
      Actor<int> actor = null;
      var log = new List<int>();
      actor = new Actor<int>("TestActor", TestLifetime, x =>
      {
        if (count -- > 0)
        {
          actor.SendBlocking(x+1);
          log.Add(x);
        }
      });
      
      actor.SendBlocking(0);
      actor.WaitForEmpty();
      Assert.AreEqual(Enumerable.Range(0, n).ToList(), log);

      
      //inplace
      count = n;
      log.Clear();
      actor = new Actor<int>("TestActor", TestLifetime, x =>
      {
        if (count -- > 0)
        {
          actor.SendOrExecuteInline(x+1).NoAwait();
          log.Add(x);
        }
      });
      actor.SendBlocking(0);
      actor.WaitForEmpty();
      Assert.AreEqual(Enumerable.Range(0, n).Reverse().ToList(), log);
      
      //inplace with async
      count = n;
      log.Clear();
      actor = new Actor<int>("TestActor", TestLifetime, async x =>
      {
        await Task.Yield();
        if (count -- > 0)
        {
          await actor.SendOrExecuteInline(x+1);
          log.Add(x);
        }
      });
      actor.SendBlocking(0);
      actor.WaitForEmpty();
      Assert.AreEqual(Enumerable.Range(0, n).Reverse().ToList(), log);
    }
    
    
  }
#endif
}