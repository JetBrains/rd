using System;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.Lifetimes.Serialization
{
  [TestFixture]
  public class NativeMemoryPoolTests : LifetimesTestBase
  {
    [Test]
    public void Test1()
    {
      while (NativeMemoryPool.TryFreeMemory()) {}

      var th = new Thread(() =>
      {
        using (var cookie = UnsafeWriter.NewThreadLocalWriter())
        {
          cookie.Writer.Write(false);
          Assert.AreEqual(1, NativeMemoryPool.SampleUsed());
        }
        Assert.AreEqual(1, NativeMemoryPool.SampleCount());
      });

      th.Start();
      th.Join();
      Assert.AreEqual(0, NativeMemoryPool.SampleUsed());

      SpinWait.SpinUntil(() =>
      {
        if (NativeMemoryPool.SampleCount() > 0)
          GC.Collect();
        else
          return true;
        return false;
      }, 1000);

      Assert.AreEqual(0, NativeMemoryPool.SampleCount());
    }

    [Test]
    public void TestManyUsages()
    {
      while (NativeMemoryPool.TryFreeMemory()) { }

      var count = 33;
      var cookies = Enumerable.Range(0, count).Select(i => NativeMemoryPool.Reserve()).ToList();
      Assert.AreEqual(count, NativeMemoryPool.SampleCount());
      Assert.AreEqual(count, NativeMemoryPool.SampleUsed());

      foreach (var cookie in cookies)
        cookie.Dispose();
      Assert.AreEqual(0, NativeMemoryPool.SampleUsed());
      Assert.AreEqual(count, NativeMemoryPool.SampleCount());
      
      while (NativeMemoryPool.TryFreeMemory()) { }
      Assert.AreEqual(0, NativeMemoryPool.SampleCount());
    }


    [Test, Explicit("Roughly measure performance")]
    public void Perf()
    {
      var sw = Stopwatch.StartNew();
      Parallel.For(0, Environment.ProcessorCount, new ParallelOptions()
      {
        MaxDegreeOfParallelism = -1
      }, x =>
      {
        for (int a = 0; a < 100000000; a++)
        {
          using (var y = UnsafeWriter.NewThreadLocalWriter())
          {
            y.Writer.Write("hhhhdd");
          }
        }
      });
      Console.WriteLine(sw.ElapsedMilliseconds);
    }
  }
}