using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Viewable
{
  public class SingleThreadSchedulerTest : LifetimesTestBase
  {

    [Test]
    public void TestSeparateThread()
    {
      var sc = SingleThreadScheduler.RunOnSeparateThread(TestLifetime, "s");
      int x = 0;
      Action a = () => { x++; };
      
      Assert.True(sc.IsIdle);
      sc.Queue(a);
      sc.Queue(a);
      sc.Queue(a);
      
      sc.Queue(() =>
      {
        if (sc.IsIdle) 
          x--; //should never happed
      });

      
      Assert.True(sc.PumpAndWaitFor(Lifetime.Eternal, TimeSpan.FromSeconds(5), () => x == 3));
      Assert.True(sc.PumpAndWaitFor(() => sc.IsIdle));
    }
    
    [Test]
    public void TestCurrentThread()
    {
      int x = 0;
      Action a = () => { x++; };

      var ld = Lifetime.Define();
      
      SingleThreadScheduler.RunInCurrentStackframe(ld.Lifetime, "s", s =>
      {
        Task.Run(() =>
        {
          s.Queue(a);
          s.Queue(a);
          s.Queue(() =>
          {
            Assert.False(s.IsIdle);
            s.PumpAndWaitFor(ld.Lifetime, TimeSpan.FromSeconds(5), () => x == 3);
          });
          s.Queue(a);
                    
          s.PumpAndWaitFor(ld.Lifetime, TimeSpan.FromSeconds(5), () => x == 3);
          ld.Terminate();
        });
      });

    }

        
    [Test]
    public void TestPriorities()
    {
      var log = new List<int>();
      
      var normal = SingleThreadScheduler.RunOnSeparateThread(TestLifetime, "s", s =>
      {
        s.Queue(() =>
        {
          log.Add(0);
          log.Add(1);
          log.Add(2);
          log.Add(3);
          log.Add(4);
        });

        var hi = SingleThreadScheduler.CreateOverExisting(s, "Hi", PrioritizedAction.HighPriority);
        hi.Queue(() => log.Add(10));
        hi.Queue(() => log.Add(11));
        hi.Queue(() => log.Add(12));
        hi.Queue(() => log.Add(13));
        hi.Queue(() => log.Add(14));

        var lo = SingleThreadScheduler.CreateOverExisting(s, "Lo", PrioritizedAction.LowPriority);
        lo.Queue(() => log.Add(-10));
        lo.Queue(() => log.Add(-11));
        lo.Queue(() => log.Add(-12));
        lo.Queue(() => log.Add(-13));
        lo.Queue(() => log.Add(-14));
      });

      normal.PumpAndWaitFor(() => normal.IsIdle);
      Assert.AreEqual(new List<int> {10, 11, 12, 13, 14,    0, 1, 2, 3, 4,        -10, -11, -12, -13, -14}, log);
      
      
      
    }
  }
}
