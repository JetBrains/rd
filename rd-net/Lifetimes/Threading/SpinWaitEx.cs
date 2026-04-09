using System;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Lifetimes;

namespace JetBrains.Threading
{
  /// <summary>
  /// Extensions for <see cref="SpinWait"/> static methods.
  /// </summary>
  public static class SpinWaitEx
  {
    /// <summary>
    /// Spins while <paramref name="condition"/> is false.
    /// </summary>
    /// <param name="condition">Stops spinning when condition is true</param>
    [PublicAPI]
    public static void SpinUntil([InstantHandle] Func<bool> condition)
    {
      SpinUntil(Lifetime.Eternal, TimeSpan.MaxValue, condition);
    }

    /// <summary>
    /// Spins while <paramref name="lifetime"/> is alive and <paramref name="condition"/> is false.
    /// </summary>
    /// <param name="lifetime">Stops spinning and return <c>false</c> when lifetime is no more alive</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns>
    /// <c>false</c> if <paramref name="lifetime"/> is not alive or canceled during spinning.
    /// Otherwise, <c>true</c> (when <paramref name="condition"/> returns true)
    /// </returns>
    [PublicAPI]
    public static bool SpinUntil(Lifetime lifetime, [InstantHandle] Func<bool> condition)
    {
      return SpinUntil(lifetime, TimeSpan.MaxValue, condition);
    }

    /// <summary>
    /// Spins while <paramref name="timeout"/> is not elapsed and <paramref name="condition"/> is false.
    /// </summary>
    /// <param name="timeout">Stops spinning and return <c>false</c> when timeout is alive</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns>
    /// <c>false</c> if <paramref name="timeout"/> is zero or elapsed during spinning.
    /// Otherwise, <c>true</c> (when <paramref name="condition"/> returns true)
    /// </returns>
    [PublicAPI]
    public static bool SpinUntil(TimeSpan timeout, [InstantHandle] Func<bool> condition)
    {
      return SpinUntil(Lifetime.Eternal, (long)timeout.TotalMilliseconds, condition);
    }

    /// <summary>
    /// Spins while <paramref name="lifetime"/> is alive, <paramref name="timeout"/> is not elapsed and <paramref name="condition"/> is false.
    /// </summary>
    /// <param name="lifetime">Stops spinning and return <c>false</c> when lifetime is no more alive</param>
    /// <param name="timeout">Stops spinning and return <c>false</c> when timeout is alive</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns>
    /// <c>false</c> if <paramref name="lifetime"/> is not alive or canceled during spinning, <paramref name="timeout"/> is zero
    /// or elapsed during spinning. Otherwise, <c>true</c> (when <paramref name="condition"/> returns true)
    /// </returns>
    [PublicAPI]
    public static bool SpinUntil(Lifetime lifetime, TimeSpan timeout, [InstantHandle] Func<bool> condition)
    {
      return SpinUntil(lifetime, (long)timeout.TotalMilliseconds, condition);
    }

    /// <summary>
    /// Spins while <paramref name="lifetime"/> is alive, <paramref name="timeoutMs"/> is not elapsed and <paramref name="condition"/> is false.
    /// </summary>
    /// <param name="lifetime">Stops spinning and return <c>false</c> when lifetime is no more alive</param>
    /// <param name="timeoutMs">Stops spinning and return <c>false</c> when timeout is alive</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns>
    /// <c>false</c> if <paramref name="lifetime"/> is not alive or canceled during spinning, <paramref name="timeoutMs"/> is zero
    /// or elapsed during spinning. Otherwise, <c>true</c> (when <paramref name="condition"/> returns true)
    /// </returns>
    [PublicAPI]
    public static bool SpinUntil(Lifetime lifetime, long timeoutMs, [InstantHandle] Func<bool> condition)
    {
      return SpinUntil(lifetime, timeoutMs, state: condition, static condition => condition());
    }

    /// <summary>
    /// Spins while <paramref name="lifetime"/> is alive, <paramref name="timeoutMs"/> is not elapsed and <paramref name="condition"/> is false.
    /// </summary>
    /// <param name="lifetime">Stops spinning and return <c>false</c> when lifetime is no more alive</param>
    /// <param name="timeoutMs">Stops spinning and return <c>false</c> when timeout is alive</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <param name="state">State to pass into delegate to avoid closures</param>
    /// <returns>
    /// <c>false</c> if <paramref name="lifetime"/> is not alive or canceled during spinning, <paramref name="timeoutMs"/> is zero
    /// or elapsed during spinning. Otherwise, <c>true</c> (when <paramref name="condition"/> returns true)
    /// </returns>
    [PublicAPI]
    public static bool SpinUntil<TState>(
      Lifetime lifetime, long timeoutMs, TState state, [InstantHandle, RequireStaticDelegate] Func<TState, bool> condition)
    {
      var s = new SpinWait();
      var start = Environment.TickCount;

      while (true)
      {
        if (!lifetime.IsAlive)
          return false;

        if (condition(state))
          return true;

        if (Environment.TickCount - start > timeoutMs)
          return false;

        s.SpinOnce();
      }
    }

    /// <summary>
    /// Spins in ASYNC manner (not consuming thread or CPU resources) while <paramref name="lifetime"/> is alive,
    /// <paramref name="timeoutMs"/> is not elapsed and <paramref name="condition"/> is false.
    /// Sleeps in async fashion (using <see cref="System.Threading.Tasks.Task.Delay(System.TimeSpan, CancellationToken)"/>
    /// for <paramref name="delayBetweenChecksMs"/> each time between <paramref name="condition"/> check.
    /// Only <paramref name="lifetime"/> cancellation could immediately return execution from delay.
    /// </summary>
    /// <param name="lifetime">Stops spinning and return <c>false</c> when lifetime is no more alive</param>
    /// <param name="timeoutMs">Stops spinning and return <c>false</c> when timeout is alive</param>
    /// <param name="delayBetweenChecksMs">Interval to delay</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns>
    /// <c>false</c> if <paramref name="lifetime"/> is not alive or canceled during spinning, <paramref name="timeoutMs"/> is zero
    /// or elapsed during spinning. Otherwise, <c>true</c> (when <paramref name="condition"/> returns true)
    /// </returns>
    [PublicAPI]
    public static async System.Threading.Tasks.Task<bool> SpinUntilAsync(
      Lifetime lifetime, long timeoutMs, int delayBetweenChecksMs, [InstantHandle(RequireAwait = true)] Func<bool> condition)
    {
      var start = Environment.TickCount;

      while (true)
      {
        if (!lifetime.IsAlive || Environment.TickCount - start > timeoutMs)
          return false;

        try
        {
          await System.Threading.Tasks.Task.Delay(delayBetweenChecksMs, lifetime);
        }
        catch (Exception)
        {
          return false;
        }

        if (condition())
          return true;
      }
    }

    /// <summary>
    /// Performs a single spin iteration, guaranteeing that the current thread is never blocked via
    /// <see cref="Thread.Sleep(int)"/>.
    /// <para>
    /// On .NET 5 and later, delegates to <see cref="SpinWait.SpinOnce(int)"/> with
    /// <c>sleepThreshold = -1</c>, which disables the sleep phase entirely while still allowing
    /// CPU-level spinning and <see cref="Thread.Yield()"/>.
    /// </para>
    /// <para>
    /// On earlier targets, falls back to <see cref="Thread.Yield()"/> when
    /// <see cref="SpinWait.NextSpinWillYield"/> is <c>true</c> (i.e., when the next ordinary
    /// <see cref="SpinWait.SpinOnce()"/> call would otherwise invoke <c>Thread.Sleep</c>),
    /// so the CPU is yielded to other ready threads without blocking.
    /// </para>
    /// </summary>
    /// <remarks>
    /// Prefer this method over <see cref="SpinWait.SpinOnce()"/> in contexts where sleeping the
    /// thread is unacceptable — for example, inside a lock-free loop
    /// that must remain responsive. <c>Thread.Sleep(1)</c>, which <see cref="SpinWait.SpinOnce()"/>
    /// eventually calls, can block for up to ~16 ms on Windows due to the default system timer
    /// resolution.
    /// </remarks>
    /// <param name="spinWait">The <see cref="SpinWait"/> instance to advance.</param>
    [PublicAPI]
    public static void SpinOnceWithoutSleep(this SpinWait spinWait)
    {
#if NET5_0_OR_GREATER
      spinWait.SpinOnce(-1);
#else
      if (spinWait.NextSpinWillYield)
      {
        Thread.Yield();
      }
      else
      {
        spinWait.SpinOnce();
      }
#endif
    }
  }
}
