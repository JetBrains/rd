using System;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Lifetimes;
using JetBrains.Util;


namespace JetBrains.Threading
{  
  /// <summary>
  /// Extensions for <see cref="SpinWait"/>.
  /// </summary>
  public struct SpinWaitEx
  {
    private SpinWait mySpinWait = new();
    
    [PublicAPI]
    public bool NextSpinWillYield => mySpinWait.NextSpinWillYield;
    
    public SpinWaitEx()
    {
    }

    /// <summary>
    /// Performs a single spin.
    /// </summary>
    /// <param name="allowThreadSleep">
    /// A value indicating whether <code>Thread.Sleep(1)</code> may be used.
    /// </param>
    [PublicAPI]
    public void SpinOnce(bool allowThreadSleep)
    {
      if (!allowThreadSleep && mySpinWait.NextSpinWillYield)
      {
#if !NET35
        Thread.Yield();
#else
        Thread.Sleep(0);
#endif
      }
      else
      {
        mySpinWait.SpinOnce();
      }
    }
    
    /// <summary>
    /// Spins while <paramref name="condition"/> is false.     
    /// </summary>
    /// <param name="condition">Stops spinning when condition is true</param>
    [PublicAPI]
    public static void SpinUntil(Func<bool> condition)
    {
      SpinUntil(Lifetime.Eternal, TimeSpan.MaxValue, condition);
    }

    /// <summary>
    /// Spins while <paramref name="lifetime"/> is alive and <paramref name="condition"/> is false.     
    /// </summary>
    /// <param name="lifetime">Stops spinning and return <c>false</c> when lifetime is no more alive</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns><c>false</c> if <paramref name="lifetime"/> is not alive or canceled during spinning.
    /// Otherwise <c>true</c> (when <paramref name="condition"/> returns true)</returns>
    [PublicAPI]
    public static bool SpinUntil(Lifetime lifetime, Func<bool> condition)
    {
      return SpinUntil(lifetime, TimeSpan.MaxValue, condition);
    }

    /// <summary>
    /// Spins while <paramref name="timeout"/> is not elapsed and <paramref name="condition"/> is false.     
    /// </summary>
    /// <param name="timeout">Stops spinning and return <c>false</c> when timeout is alive</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns><c>false</c> if <paramref name="timeout"/> is zero or elapsed during spinning.
    /// Otherwise <c>true</c> (when <paramref name="condition"/> returns true)</returns>
    [PublicAPI]
    public static bool SpinUntil(TimeSpan timeout, Func<bool> condition)
    {
      return SpinUntil(Lifetime.Eternal, (long)timeout.TotalMilliseconds, condition);
    }
    
    /// <summary>
    /// Spins while <paramref name="lifetime"/> is alive, <paramref name="timeout"/> is not elapsed and <paramref name="condition"/> is false.     
    /// </summary>
    /// <param name="lifetime">Stops spinning and return <c>false</c> when lifetime is no more alive</param>
    /// <param name="timeout">Stops spinning and return <c>false</c> when timeout is alive</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns><c>false</c> if <paramref name="lifetime"/> is not alive or canceled during spinning, <paramref name="timeout"/> is zero or elapsed during spinning.
    /// Otherwise <c>true</c> (when <paramref name="condition"/> returns true)</returns>
    [PublicAPI]
    public static bool SpinUntil(Lifetime lifetime, TimeSpan timeout, Func<bool> condition)
    {
      var spinner = new SpinWaitEx();
      var stopwatch = LocalStopwatch.StartNew();
    
      var allowThreadSleep = false;
      var noThreadSleepTimeout = TimeSpan.FromMilliseconds(16);
    
      while (!condition())
      {
        if (lifetime.IsNotAlive || timeout == TimeSpan.Zero)
          return false;

        spinner.SpinOnce(allowThreadSleep);

        if (lifetime.IsNotAlive)
          return false;

        var elapsed = stopwatch.Elapsed;
        if (elapsed > timeout)
          return false;

        if (elapsed > noThreadSleepTimeout && !allowThreadSleep)
          allowThreadSleep = true;
      }
    
      return true;
    }
    
    
    /// <summary>
    /// Spins while <paramref name="lifetime"/> is alive, <paramref name="timeoutMs"/> is not elapsed and <paramref name="condition"/> is false.     
    /// </summary>
    /// <param name="lifetime">Stops spinning and return <c>false</c> when lifetime is no more alive</param>
    /// <param name="timeoutMs">Stops spinning and return <c>false</c> when timeout is alive</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns><c>false</c> if <paramref name="lifetime"/> is not alive or canceled during spinning, <paramref name="timeoutMs"/> is zero or elapsed during spinning.
    /// Otherwise <c>true</c> (when <paramref name="condition"/> returns true)</returns>    
    [PublicAPI]
    public static bool SpinUntil(Lifetime lifetime, long timeoutMs, Func<bool> condition)
    {
      return SpinUntil(lifetime, TimeSpan.FromMilliseconds(timeoutMs), condition);
    }

#if !NET35
    /// <summary>
    /// Spins in ASYNC manner (not consuming thread or CPU resources) while <paramref name="lifetime"/> is alive, <paramref name="timeoutMs"/> is not elapsed and <paramref name="condition"/> is false.
    /// Sleeps in async fashion (using <see cref="System.Threading.Tasks.Task.Delay(System.TimeSpan, CancellationToken)"/> for <paramref name="delayBetweenChecksMs"/> each time between <paramref name="condition"/> check.
    /// Only <paramref name="lifetime"/> cancellation could immediately return execution from delay. 
    /// </summary>
    /// <param name="lifetime">Stops spinning and return <c>false</c> when lifetime is no more alive</param>
    /// <param name="timeoutMs">Stops spinning and return <c>false</c> when timeout is alive</param>
    /// <param name="delayBetweenChecksMs">Interval to delay</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns><c>false</c> if <paramref name="lifetime"/> is not alive or canceled during spinning, <paramref name="timeoutMs"/> is zero or elapsed during spinning.
    /// Otherwise <c>true</c> (when <paramref name="condition"/> returns true)</returns> 
    [PublicAPI]
    public static System.Threading.Tasks.Task<bool> SpinUntilAsync(Lifetime lifetime, long timeoutMs, int delayBetweenChecksMs, Func<bool> condition)
    {
      return SpinUntilAsync(lifetime, TimeSpan.FromMilliseconds(timeoutMs), delayBetweenChecksMs, condition);
    }
    
    /// <summary>
    /// Spins in ASYNC manner (not consuming thread or CPU resources) while <paramref name="lifetime"/> is alive, <paramref name="timeout"/> is not elapsed and <paramref name="condition"/> is false.
    /// Sleeps in async fashion (using <see cref="System.Threading.Tasks.Task.Delay(System.TimeSpan, CancellationToken)"/> for <paramref name="delayBetweenChecksMs"/> each time between <paramref name="condition"/> check.
    /// Only <paramref name="lifetime"/> cancellation could immediately return execution from delay. 
    /// </summary>
    /// <param name="lifetime">Stops spinning and return <c>false</c> when lifetime is no more alive</param>
    /// <param name="timeout">Stops spinning and return <c>false</c> when timeout is alive</param>
    /// <param name="delayBetweenChecksMs">Interval to delay</param>
    /// <param name="condition">Stops spinning and return <c>false</c> when condition is true</param>
    /// <returns><c>false</c> if <paramref name="lifetime"/> is not alive or canceled during spinning, <paramref name="timeout"/> is zero or elapsed during spinning.
    /// Otherwise <c>true</c> (when <paramref name="condition"/> returns true)</returns> 
    public static async System.Threading.Tasks.Task<bool> SpinUntilAsync(Lifetime lifetime, TimeSpan timeout, int delayBetweenChecksMs, Func<bool> condition)
    {
      var stopwatch = new LocalStopwatch();

      while (!condition())
      {
        if (lifetime.IsNotAlive || stopwatch.Elapsed > timeout)
          return false;

        try
        {
          await System.Threading.Tasks.Task.Delay(delayBetweenChecksMs, lifetime);
        }
        catch (Exception)
        {
          return false;
        }
      }

      return true;
    }
#endif
  }
}