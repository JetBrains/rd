using System;
using JetBrains.Diagnostics;

namespace JetBrains.Rd.Tasks
{
  /// <summary>
  /// Information about a synchronous RPC call, provided by <see cref="SyncCallMonitor"/>.
  /// </summary>
  public readonly struct SyncCallInfo
  {
    /// <summary>The location (endpoint path) of the RPC call.</summary>
    public RName Location { get; }

    /// <summary>How long the synchronous call took.</summary>
    public TimeSpan ElapsedTime { get; }

    /// <summary>The timeouts configured for this call.</summary>
    public RpcTimeouts Timeouts { get; }

    public SyncCallInfo(RName location, TimeSpan elapsedTime, RpcTimeouts timeouts)
    {
      Location = location;
      ElapsedTime = elapsedTime;
      Timeouts = timeouts;
    }
  }

  /// <summary>
  /// Provides observability for synchronous RPC calls.
  /// </summary>
  public static class SyncCallMonitor
  {
    /// The default <see cref="SyncCallFinished"/> handler: logs when a call exceeds WarnAwaitTime.
    /// Opt-out.
    public static readonly Action<SyncCallInfo> DefaultWarnHandler = static info =>
    {
      if (info.ElapsedTime > info.Timeouts.WarnAwaitTime)
        Log.Root.Error("Sync execution of rpc `{0}` executed too long: {1} ms, the freeze time: {2} ms", info.Location, info.Timeouts.WarnAwaitTime.TotalMilliseconds, info.ElapsedTime.TotalMilliseconds);
    };

    static SyncCallMonitor()
    {
      SyncCallFinished += DefaultWarnHandler;
    }

    /// <summary>
    /// Raised after every synchronous RPC call that completed within its timeout.
    /// Use this to track sync call volume and identify candidates for async migration.
    /// </summary>
    public static event Action<SyncCallInfo>? SyncCallFinished;

    /// <summary>
    /// Raised when a synchronous RPC call exceeds its error timeout, before the
    /// <see cref="TimeoutException"/> is thrown to the caller.
    /// Use this to aggregate timeout-affected endpoints.
    /// </summary>
    public static event Action<SyncCallInfo>? SyncCallTimedOut;

    public static void RaiseSyncCallFinished(SyncCallInfo info) => SyncCallFinished?.Invoke(info);
    public static void RaiseSyncCallTimedOut(SyncCallInfo info) => SyncCallTimedOut?.Invoke(info);
  }
}
