using System;
using System.Diagnostics;
using System.Runtime.CompilerServices;
using JetBrains.Diagnostics;

namespace JetBrains.Util
{
  /// <summary>
  /// This structure can be used as a non-allocated version of the <see cref="Stopwatch"/>
  /// </summary>
  public readonly struct LocalStopwatch
  {
    #region Statics

    /// TicksPerMillisecond and TicksPerSecond values are synchronized with the internal values of <see cref="Stopwatch"/> 
    private const long TicksPerMillisecond = 10000;
    private const long TicksPerSecond = TicksPerMillisecond * 1000;

    private static readonly long ourFrequency = Stopwatch.Frequency;
    private static readonly double ourTickFrequency;

    static LocalStopwatch()
    {
      ourTickFrequency = Stopwatch.IsHighResolution ? (double)TicksPerSecond / ourFrequency : 1.0;
    }

    public static LocalStopwatch StartNew() => new LocalStopwatch(Stopwatch.GetTimestamp());

    #endregion

    private readonly long myStartTimeStamp;

    public TimeSpan Elapsed => new TimeSpan(GetElapsedDateTimeTicks());

    public long ElapsedMilliseconds => GetElapsedDateTimeTicks() / TicksPerMillisecond;
    
    public long ElapsedTicks
    {
      get
      {
        AssertTimeStamp();
        return Stopwatch.GetTimestamp() - myStartTimeStamp;
      }
    }

    private LocalStopwatch(long startTimeStamp)
    {
      myStartTimeStamp = startTimeStamp;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private long GetElapsedDateTimeTicks() => unchecked((long)(ElapsedTicks * ourTickFrequency));

    private void AssertTimeStamp()
    {
      if (Mode.IsAssertion) Assertion.Assert(myStartTimeStamp != 0, $"{nameof(LocalStopwatch)} must be created using `{nameof(StartNew)}` method");
    }
  }
  
  /// <summary>
  /// Non-allocating, low-resolution stopwatch backed by <see cref="Environment.TickCount64"/> on
  /// .NET 5+ and by <see cref="Environment.TickCount"/> on older targets. Resolution is whatever
  /// the OS tick granularity is (typically ~10–16 ms on Windows), which makes this much cheaper
  /// than <see cref="Stopwatch"/> when sub-millisecond accuracy is not needed.
  /// </summary>
  /// <remarks>
  /// <para>
  /// Always create instances via <see cref="StartNew"/>; a default-constructed value will trip an
  /// assertion in <see cref="ElapsedMilliseconds"/>.
  /// </para>
  /// <para>
  /// <b>Warning — wrap-around on pre-.NET 5 targets (net472 / netstandard2.0):</b>
  /// the underlying <see cref="Environment.TickCount"/> is a 32-bit counter. The unsigned
  /// subtraction used here recovers the correct elapsed value across one wrap, giving an
  /// unambiguous range of <b>~49.71 days (2^32 ms)</b>. If the stopwatch is read after a longer
  /// interval the result silently aliases (true elapsed modulo ~49.71 days) — do not rely on
  /// this type for measurements that may exceed that bound on pre-net5 targets. The .NET 5+
  /// branch uses a 64-bit counter and is not subject to this limit in practice.
  /// </para>
  /// </remarks>
  public readonly struct FastStopwatch
  {
#if NET5_0_OR_GREATER
    private readonly long myStartTimeStamp;
    private FastStopwatch(long startTimestamp) => myStartTimeStamp = startTimestamp;
    private static long GetTicks() => Environment.TickCount64;
#else
    private readonly uint myStartTimeStamp;
    private FastStopwatch(uint startTimestamp) => myStartTimeStamp = startTimestamp;
    private static uint GetTicks() => (uint)Environment.TickCount;
#endif

    public static FastStopwatch StartNew() => new(GetTicks());
    public long ElapsedMilliseconds
    {
      get
      {
        AssertTimeStamp();
        return unchecked(GetTicks() - myStartTimeStamp);
      }
    }

    public TimeSpan Elapsed => TimeSpan.FromMilliseconds(ElapsedMilliseconds);
  
    private void AssertTimeStamp()
    {
      if (Mode.IsAssertion) Assertion.Assert(myStartTimeStamp != 0, $"{nameof(FastStopwatch)} must be created using `{nameof(StartNew)}` method");
    }
  }
}
