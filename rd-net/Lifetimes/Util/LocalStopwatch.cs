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
}