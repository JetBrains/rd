using System;

namespace JetBrains.Rd.Tasks
{
  public class RpcTimeouts
  {
    public static readonly RpcTimeouts Default = new RpcTimeouts(TimeSpan.FromMilliseconds(200), TimeSpan.FromMilliseconds(3000));
    public static readonly RpcTimeouts Maximal = new RpcTimeouts(TimeSpan.FromMilliseconds(30000), TimeSpan.FromMilliseconds(30000));

    public static bool RespectRpcTimeouts = true;

    public static RpcTimeouts Max(RpcTimeouts x, RpcTimeouts y)
    {
      return new RpcTimeouts(
        x.WarnAwaitTime > y.WarnAwaitTime ? x.WarnAwaitTime : y.WarnAwaitTime,
        x.ErrorAwaitTime > y.ErrorAwaitTime ? x.ErrorAwaitTime : y.ErrorAwaitTime);
    }

    public readonly TimeSpan WarnAwaitTime;
    public readonly TimeSpan ErrorAwaitTime;

    public RpcTimeouts(TimeSpan warnAwaitTime, TimeSpan errorAwaitTime)
    {
      WarnAwaitTime = warnAwaitTime;
      ErrorAwaitTime = errorAwaitTime;
    }
  }
}