using System;
using JetBrains.Annotations;
using JetBrains.Diagnostics;

namespace JetBrains.Rd.Tasks
{
  /// <summary>
  /// Timeouts for synchronous request by <see cref="RdCall{TReq,TRes}.Sync"/>. 
  /// </summary>
  public class RpcTimeouts
  {
    /// <summary>
    /// This timeout is used by <see cref="RdCall{TReq,TRes}.Sync"/> if no timeouts specified and <see cref="RespectRpcTimeouts"/> is <c>true</c>.
    /// If request lasts more than 200 ms from UI Thread it could lead to UI freeze, so <see cref="ILog.Warn"/> will be reported after <c>Sync</c> finished.
    /// If request lasts more than 3000 ms it's considered as hang. <c>Sync</c> request interrupts and <see cref="ILog.Error"/> is reported.
    /// </summary>
    public static readonly RpcTimeouts Default = new RpcTimeouts(TimeSpan.FromMilliseconds(200), TimeSpan.FromMilliseconds(3000));
    
    /// <summary>
    /// This timeout is used by <see cref="RdCall{TReq,TRes}.Sync"/> if no timeouts specified and <see cref="RespectRpcTimeouts"/> is <c>false</c>.
    /// </summary>
    public static readonly RpcTimeouts Maximal = new RpcTimeouts(TimeSpan.FromMilliseconds(30000), TimeSpan.FromMilliseconds(30000));

    /// <summary>
    /// Static property used mainly for tests. <c>true</c> by default. If <c>false</c> <see cref="Maximal"/> timeouts are used by
    /// <see cref="RdCall{TReq,TRes}.Sync"/> by default.
    /// </summary>
    public static bool RespectRpcTimeouts = true;

    /// <summary>
    /// Choose maximal of two timeouts maximizing Warn and Error await time among two.
    /// </summary>
    /// <param name="x">First timeouts</param>
    /// <param name="y">Second timeouts</param>
    /// <returns></returns>
    public static RpcTimeouts Max(RpcTimeouts x, RpcTimeouts y)
    {
      return new RpcTimeouts(
        x.WarnAwaitTime > y.WarnAwaitTime ? x.WarnAwaitTime : y.WarnAwaitTime,
        x.ErrorAwaitTime > y.ErrorAwaitTime ? x.ErrorAwaitTime : y.ErrorAwaitTime);
    }

    /// <summary>
    /// Time after which <see cref="ILog.Warn"/> is reported.
    /// </summary>
    public readonly TimeSpan WarnAwaitTime;
    
    /// <summary>
    /// Time after which <see cref="ILog.Error"/> is reported.
    /// </summary>
    public readonly TimeSpan ErrorAwaitTime;

    /// <summary>
    /// Creates new timeouts
    /// </summary>
    /// <param name="warnAwaitTime">Must be more than 0 ms and less or equal than <paramref name="errorAwaitTime"/>.</param>
    /// <param name="errorAwaitTime">Must be more or equal than <paramref name="warnAwaitTime"/></param>.
    public RpcTimeouts(TimeSpan warnAwaitTime, TimeSpan errorAwaitTime)
    {
      Assertion.Require(warnAwaitTime.TotalMilliseconds > 0, "Warn timeout should be more 0ms but was: {0}", warnAwaitTime.TotalMilliseconds);
      Assertion.Require(warnAwaitTime <= errorAwaitTime, "Warn timeout should ({0} ms) <= Error Timeout ({1} ms)", warnAwaitTime.TotalMilliseconds, ErrorAwaitTime.TotalMilliseconds);
      
      
      WarnAwaitTime = warnAwaitTime;
      ErrorAwaitTime = errorAwaitTime;
    }

    /// <summary>
    /// Returns a mix of optionally provided timeouts and default one.
    /// </summary>
    public static RpcTimeouts GetRpcTimeouts(RpcTimeouts? timeouts)
    {
      RpcTimeouts timeoutsToUse;
      if (RespectRpcTimeouts)
        timeoutsToUse = timeouts ?? Default;
      else
        timeoutsToUse = timeouts == null
          ? Maximal
          : Max(timeouts, Maximal);
      return timeoutsToUse;
    }
  }
}