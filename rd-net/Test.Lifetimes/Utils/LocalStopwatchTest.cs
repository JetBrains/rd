using System.Diagnostics;
using System.Threading;
using JetBrains.Util;
using JetBrains.Util.Internal;
using NUnit.Framework;

namespace Test.Lifetimes.Utils
{
  public class LocalStopwatchTest
  {
    [Test]
    public void SimpleTest()
    {
      for (int j = 0; j < 10; j++)
      {
        var stopwatch2 = Stopwatch.StartNew();
        Memory.Barrier();
        var localStopwatch = LocalStopwatch.StartNew();
        Memory.Barrier();
        var stopwatch1 = Stopwatch.StartNew();
      
        for (int i = 0; i < 50; i++)
        {
          var milliseconds1 = stopwatch1.ElapsedMilliseconds;
          Memory.Barrier();
          var localMilliseconds = localStopwatch.ElapsedMilliseconds;
          Memory.Barrier();
          var milliseconds2 = stopwatch2.ElapsedMilliseconds;
          Memory.Barrier();
        
          Assert.GreaterOrEqual(localMilliseconds, milliseconds1);
          Assert.GreaterOrEqual(milliseconds2, localMilliseconds);

          var elapsed1 = stopwatch1.Elapsed;
          Memory.Barrier();
          var localElapsed = localStopwatch.Elapsed;
          Memory.Barrier();
          var elapsed2 = stopwatch2.Elapsed;
          Memory.Barrier();
        
          Assert.GreaterOrEqual(localElapsed, elapsed1);
          Assert.GreaterOrEqual(elapsed2, localElapsed);

          var elapsedTicks1 = stopwatch1.ElapsedTicks;
          Memory.Barrier();
          var localElapsedTicks1 = localStopwatch.ElapsedTicks;
          Memory.Barrier();
          var elapsedTicks2 = stopwatch2.ElapsedTicks;
          Memory.Barrier();
        
          Assert.GreaterOrEqual(localElapsedTicks1, elapsedTicks1);
          Assert.GreaterOrEqual(elapsedTicks2, localElapsedTicks1);

          Thread.Sleep(i % 16);
        }
      }
    }
  }
}