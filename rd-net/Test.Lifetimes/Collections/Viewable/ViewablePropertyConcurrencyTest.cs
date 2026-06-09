using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Viewable;

// Stresses the lock-free read paths of ViewableProperty: the atomic fast path (primitives/refs)
// and the seqlock path used for every struct (GetBigValueSlowNoLock). A torn read on the seqlock
// path surfaces as an inconsistent BigStruct; a stale/premature read surfaces as a value outside
// the written set or as HasValue carrying default.
[TestFixture]
public class ViewablePropertyConcurrencyTest
{
  private static readonly TimeSpan ourTimeout = TimeSpan.FromMinutes(1);

  // A non-primitive value type, so reads go through the seqlock path. All fields share one seed,
  // so any torn read (a mix of two writes) is caught by IsConsistent.
  private record struct BigStruct(
    long A, long B, long C, long D, long E, long F, long G, long H,
    long I, long J, long K, long L, long M, long N, long O, long P)
  {
    public static BigStruct Of(long v) => new(v, v, v, v, v, v, v, v, v, v, v, v, v, v, v, v);

    public bool IsConsistent =>
      A == B && B == C && C == D && D == E && E == F && F == G && G == H && H == I &&
      I == J && J == K && K == L && L == M && M == N && N == O && O == P;
  }

  private static long NextLong(Random rng) => ((long)rng.Next() << 31) | (uint)rng.Next();

  [Test]
  public void WriteRead_BigStruct()
  {
    var prop = new ViewableProperty<BigStruct>(BigStruct.Of(0));
    for (long i = -100; i <= 100; i++)
    {
      prop.Value = BigStruct.Of(i);
      var v = prop.Value;
      Assert.AreEqual(i, v.A);
      Assert.IsTrue(v.IsConsistent);
    }
  }

  // Acquire/release: a spinning reader on the atomic path must not cache the value and must
  // eventually observe the latest write (it need not see every intermediate one).
  [Test]
  public void Visibility_Int()
  {
    var prop = new ViewableProperty<int>(0);
    const int n = 1_000_000;
    var started = new ManualResetEvent(false);
    var task = Task.Run(() =>
    {
      started.Set();
      var prev = 0;
      while (prev < n)
        prev = prop.Value;
    });

    Assert.IsTrue(started.WaitOne(ourTimeout));
    for (var i = 1; i <= n; i++)
      prop.Value = i;

    Assert.IsTrue(task.Wait(ourTimeout));
  }

  // Same for the seqlock path: eventually observe the latest write, and never a torn read.
  [Test]
  public void Visibility_BigStruct()
  {
    var prop = new ViewableProperty<BigStruct>(BigStruct.Of(0));
    const long n = 1_000_000;
    var started = new ManualResetEvent(false);
    var torn = false;
    var task = Task.Run(() =>
    {
      started.Set();
      long prev = 0;
      while (prev < n)
      {
        var v = prop.Value;
        if (!v.IsConsistent)
          torn = true;
        prev = v.A;
      }
    });

    Assert.IsTrue(started.WaitOne(ourTimeout));
    for (long i = 1; i <= n; i++)
      prop.Value = BigStruct.Of(i);

    Assert.IsTrue(task.Wait(ourTimeout));
    Assert.IsFalse(torn, "Torn read on the seqlock path");
  }

  [Test]
  public void NoTearing_BigStruct_SingleWriter()
  {
    var prop = new ViewableProperty<BigStruct>(BigStruct.Of(0));
    var readerCount = Math.Max(1, Environment.ProcessorCount - 1);
    var barrier = new Barrier(readerCount + 1);
    var running = true;
    var errors = 0;

    var readers = Enumerable.Range(0, readerCount).Select(_ => Task.Run(() =>
    {
      barrier.SignalAndWait(-1);
      while (Volatile.Read(ref running))
      {
        if (!prop.Value.IsConsistent)
          Interlocked.Increment(ref errors);
      }
    })).ToArray();

    barrier.SignalAndWait(-1);
    var rng = new Random(42);
    for (var i = 0; i < 1_000_000; i++)
      prop.Value = BigStruct.Of(NextLong(rng));

    Volatile.Write(ref running, false);
    Assert.IsTrue(Task.WaitAll(readers, ourTimeout));
    Assert.AreEqual(0, errors, "Torn reads on the seqlock path");
  }

  // Writers are serialized by the property's internal lock, but readers stay lock-free; verifies the
  // seqlock holds when the writer identity keeps changing.
  [Test]
  public void NoTearing_BigStruct_MultipleWriters()
  {
    var prop = new ViewableProperty<BigStruct>(BigStruct.Of(0));
    var writerCount = Math.Max(2, Environment.ProcessorCount / 2);
    var readerCount = Math.Max(1, Environment.ProcessorCount / 2);
    var barrier = new Barrier(writerCount + readerCount);
    var running = true;
    var errors = 0;

    var readers = Enumerable.Range(0, readerCount).Select(_ => Task.Run(() =>
    {
      barrier.SignalAndWait(-1);
      while (Volatile.Read(ref running))
      {
        if (!prop.Value.IsConsistent)
          Interlocked.Increment(ref errors);
      }
    })).ToArray();

    var writers = Enumerable.Range(0, writerCount).Select(w => Task.Run(() =>
    {
      var rng = new Random(w);
      barrier.SignalAndWait(-1);
      for (var i = 0; i < 300_000; i++)
        prop.Value = BigStruct.Of(NextLong(rng));
    })).ToArray();

    Assert.IsTrue(Task.WaitAll(writers, ourTimeout));
    Volatile.Write(ref running, false);
    Assert.IsTrue(Task.WaitAll(readers, ourTimeout));
    Assert.AreEqual(0, errors, "Torn reads on the seqlock path (multiple writers)");
  }

  // Every value a reader observes must belong to the written set - a torn read would produce something else.
  [Test]
  public void ConcurrentReadStress_BigStruct()
  {
    var rng = new Random(42);
    var values = Enumerable.Range(0, 100)
      .Select(_ => BigStruct.Of(NextLong(rng)))
      .Concat(new[] { default(BigStruct) })
      .ToHashSet();

    var currentProperty = new ViewableProperty<BigStruct>(default);
    var running = true;
    var threads = Math.Max(1, Environment.ProcessorCount / 2);
    var barrier = new Barrier(threads);
    var started = new ManualResetEvent(false);
    var bag = new ConcurrentBag<BigStruct>();

    var tasks = Enumerable.Range(0, threads).Select(_ => Task.Run(() =>
    {
      barrier.SignalAndWait(-1);
      started.Set();
      var prev = default(BigStruct);
      while (Volatile.Read(ref running))
      {
        var value = Volatile.Read(ref currentProperty).Value;
        if (value.Equals(prev))
        {
          Thread.Yield();
          continue;
        }

        prev = value;
        bag.Add(value);
      }
    })).ToArray();

    Assert.IsTrue(started.WaitOne(ourTimeout));
    for (var i = 0; i < 1000; i++)
    {
      foreach (var v in values)
        currentProperty.Value = v;

      Volatile.Write(ref currentProperty, new ViewableProperty<BigStruct>(default));
    }

    Volatile.Write(ref running, false);
    Assert.IsTrue(Task.WaitAll(tasks, ourTimeout));
    while (bag.TryTake(out var v))
      Assert.IsTrue(values.Contains(v), $"observed a value not in the written set (torn read): {v}");
  }

  [Test]
  public void ConcurrentReadOnly_BigStruct()
  {
    var initial = BigStruct.Of(0x0123_4567_89AB_CDEF);
    var prop = new ViewableProperty<BigStruct>(initial);
    var threads = Environment.ProcessorCount;
    var barrier = new Barrier(threads);
    var errors = 0;

    var tasks = Enumerable.Range(0, threads).Select(_ => Task.Run(() =>
    {
      barrier.SignalAndWait(-1);
      for (var i = 0; i < 1_000_000; i++)
      {
        if (!prop.Value.Equals(initial))
          Interlocked.Increment(ref errors);
      }
    })).ToArray();

    Assert.IsTrue(Task.WaitAll(tasks, ourTimeout));
    Assert.AreEqual(0, errors, "concurrent reads of an immutable value diverged from initial");
  }

  // Guards the HasValue-during-first-write regression on the atomic path (string): a reader must never
  // see HasValue with a default (null) value while the first Set is in progress.
  [Test]
  public void FirstWrite_NeverObservesDefaultOnAtomicPath()
  {
    ViewableProperty<string> currentProperty = null;
    var running = true;
    var errors = 0;
    var readerCount = Math.Max(1, Environment.ProcessorCount - 1);
    var barrier = new Barrier(readerCount + 1);

    var readers = Enumerable.Range(0, readerCount).Select(_ => Task.Run(() =>
    {
      barrier.SignalAndWait(-1);
      while (Volatile.Read(ref running))
      {
        var property = Volatile.Read(ref currentProperty);
        if (property == null)
          continue;

        var maybe = property.Maybe;
        if (maybe.HasValue && maybe.Value == null)
          Interlocked.Increment(ref errors);
      }
    })).ToArray();

    barrier.SignalAndWait(-1);
    for (var i = 0; i < 1_000_000; i++)
    {
      var property = new ViewableProperty<string>();
      Volatile.Write(ref currentProperty, property);
      property.Value = "value";
    }

    Volatile.Write(ref running, false);
    Assert.IsTrue(Task.WaitAll(readers, ourTimeout));
    Assert.AreEqual(0, errors, "observed HasValue carrying default(null) during the first write");
  }

  // Seqlock counterpart of the above: while a fresh property gets its first value, a reader must never
  // observe HasValue carrying a default or torn struct (catches an early HasValue flip or publishing
  // the "set" timestamp before myValue).
  [Test]
  public void FirstWrite_NeverObservesDefaultOrTornOnSeqlockPath()
  {
    ViewableProperty<BigStruct> currentProperty = null;
    var running = true;
    var errors = 0;
    var readerCount = Math.Max(1, Environment.ProcessorCount - 1);
    var barrier = new Barrier(readerCount + 1);

    var readers = Enumerable.Range(0, readerCount).Select(_ => Task.Run(() =>
    {
      barrier.SignalAndWait(-1);
      while (Volatile.Read(ref running))
      {
        var property = Volatile.Read(ref currentProperty);
        if (property == null)
          continue;

        var maybe = property.Maybe;
        if (maybe.HasValue && (!maybe.Value.IsConsistent || maybe.Value.Equals(default(BigStruct))))
          Interlocked.Increment(ref errors);
      }
    })).ToArray();

    barrier.SignalAndWait(-1);
    var rng = new Random(42);
    for (var i = 0; i < 1_000_000; i++)
    {
      var property = new ViewableProperty<BigStruct>();
      Volatile.Write(ref currentProperty, property);
      property.Value = BigStruct.Of(NextLong(rng) | 1); // | 1 keeps it non-default
    }

    Volatile.Write(ref running, false);
    Assert.IsTrue(Task.WaitAll(readers, ourTimeout));
    Assert.AreEqual(0, errors, "observed HasValue with a default/torn struct during the first write");
  }

  // The Advise handler fires synchronously under the property's lock after myValue is published, so a
  // handler reading the property back must see exactly the fired value, fully published and consistent.
  // Guards the writer's publish-then-Fire ordering.
  [Test]
  public void AdviseHandler_ObservesFiredValue_BigStruct()
  {
    var prop = new ViewableProperty<BigStruct>(BigStruct.Of(0));
    var errors = 0;

    Lifetime.Using(lt =>
    {
      prop.Advise(lt, fired =>
      {
        if (!fired.IsConsistent || !prop.Value.Equals(fired))
          Interlocked.Increment(ref errors);
      });

      var writerCount = Math.Max(2, Environment.ProcessorCount / 2);
      var barrier = new Barrier(writerCount);
      var writers = Enumerable.Range(0, writerCount).Select(w => Task.Run(() =>
      {
        var rng = new Random(w);
        barrier.SignalAndWait(-1);
        for (var i = 0; i < 200_000; i++)
          prop.Value = BigStruct.Of(NextLong(rng));
      })).ToArray();

      Assert.IsTrue(Task.WaitAll(writers, ourTimeout));
    });

    Assert.AreEqual(0, errors, "Advise handler observed a torn or stale value");
  }
}
