using System;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Serialization;
using NUnit.Framework;

#if NET472
using BenchmarkDotNet.Attributes;
using BenchmarkDotNet.Running;
#endif

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
        for (int a = 0; a < 100_000_000; a++)
        {
          using (var y = UnsafeWriter.NewThreadLocalWriter())
          {
            y.Writer.Write("hhhhdd");
          }
        }
      });
      Console.WriteLine(sw.ElapsedMilliseconds);
    }

#if NET472
    [Test, Explicit("Scientifically measure performance")]
    public void Bdn()
    {
      // 13.11.2020
      // BenchmarkDotNet=v0.12.1, OS=Windows 10.0.19041.572 (2004/?/20H1)
      // Intel Core i7-4790K CPU 4.00GHz (Haswell), 1 CPU, 8 logical and 4 physical cores
      //   [Host]     : .NET Framework 4.8 (4.8.4250.0), X64 RyuJIT
      //   DefaultJob : .NET Framework 4.8 (4.8.4250.0), X64 RyuJIT
      // | Method |     Mean |    Error |   StdDev |
      // |------- |---------:|---------:|---------:|
      // |      M | 24.34 ns | 0.153 ns | 0.143 ns |
      BenchmarkRunner.Run<NativeMemoryPoolTests>();
    }

    [Benchmark]
    public void M()
    {
      using (var y = UnsafeWriter.NewThreadLocalWriter())
      {
        y.Writer.Write("hhhhdd");
      }
    }
#endif
  }
}