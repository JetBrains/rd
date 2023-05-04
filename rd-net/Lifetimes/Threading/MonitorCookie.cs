using System;
using System.Threading;
using JetBrains.Lifetimes;
using JetBrains.Util;

namespace JetBrains.Threading;

public readonly ref struct MonitorCookie
{
  private static readonly TimeSpan ourDefaultTimeoutBetweenChecks = TimeSpan.FromMilliseconds(50); 
  
  private readonly object myLocker;
  public bool Success => myLocker != null;

  private MonitorCookie(object locker)
  {
    myLocker = locker;
  }

  public static MonitorCookie TryEnter(object locker, Lifetime lifetime, TimeSpan timeoutBetweenChecks, TimeSpan timeout)
  {
    if (lifetime.IsAlive && Monitor.TryEnter(locker))
      return new MonitorCookie(locker);
    
    var stopwatch = LocalStopwatch.StartNew();
    while (lifetime.IsAlive && stopwatch.Elapsed < timeout)
    {
      if (Monitor.TryEnter(locker, timeoutBetweenChecks))
      {
        if (lifetime.IsNotAlive)
        {
          Monitor.Exit(locker);
          return default;
        }

        return new MonitorCookie(locker);
      }
    }

    return default;
  }
  
  public static MonitorCookie TryEnter(object locker, Lifetime lifetime, TimeSpan timeoutBetweenChecks)
  {
    return TryEnter(locker, lifetime, timeoutBetweenChecks, TimeSpan.MaxValue);
  }
  
  public static MonitorCookie TryEnter(object locker, Lifetime lifetime)
  {
    return TryEnter(locker, lifetime, ourDefaultTimeoutBetweenChecks);
  }
  
  public static MonitorCookie TryEnter(object locker, TimeSpan timeout)
  {
    return TryEnter(locker, Lifetime.Eternal, ourDefaultTimeoutBetweenChecks, timeout);
  }
  
  public static MonitorCookie EnterOrThrow(object locker, Lifetime lifetime, TimeSpan timeoutBetweenChecks, TimeSpan timeout)
  {
    var cookie = TryEnter(locker, lifetime, timeoutBetweenChecks, timeout);
    if (cookie.Success)
      return cookie;
    
    lifetime.ThrowIfNotAlive();
    throw new TimeoutException($"Unable to take lock with timeout {timeout}");
  }
  
  public static MonitorCookie EnterOrThrow(object locker, Lifetime lifetime, TimeSpan timeoutBetweenChecks)
  {
    var cookie = TryEnter(locker, lifetime, timeoutBetweenChecks);
    if (cookie.Success)
      return cookie;

    throw new LifetimeCanceledException(lifetime);
  }
  
  public static MonitorCookie EnterOrThrow(object locker, Lifetime lifetime)
  {
    return EnterOrThrow(locker, lifetime, ourDefaultTimeoutBetweenChecks);
  }
  
  public static MonitorCookie EnterOrThrow(object locker, TimeSpan timeout)
  {
    return EnterOrThrow(locker, Lifetime.Eternal, ourDefaultTimeoutBetweenChecks, timeout);
  }
  
  public void Dispose()
  {
    if (myLocker is { } locker)
      Monitor.Exit(locker);
  }
}