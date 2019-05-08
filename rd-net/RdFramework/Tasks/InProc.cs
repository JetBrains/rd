using System;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Serialization;

namespace JetBrains.Rd.Tasks
{
  /// <summary>
  /// Fake IRdCall implementation for use in single process.
  /// </summary>
  public class InprocRpc<TReq, TRes> : RdBindableBase, IRdCall<TReq, TRes>
  {
    public Func<Lifetime, TReq, RdTask<TRes>> myHandler;
    private Lifetime myBindLifetime;

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdEndpoint<TReq, TRes> value)
    {
      RdId.Write(writer, value.RdId);
    }

    public void SetHandler([NotNull] Func<Lifetime, TReq, RdTask<TRes>> handler)
    {
      Assertion.Assert(myHandler == null, "Handler already initialized");
      myHandler = handler ?? throw new ArgumentNullException(nameof(handler));
    }

    protected override void Init(Lifetime lifetime)
    {
      myBindLifetime = lifetime;
      base.Init(lifetime);
    }

    public void SetHandler([NotNull] Func<TReq, TRes> handler)
    {
      Assertion.Assert(myHandler == null, "Handler already initialized");
      myHandler = (lt, req) => RdTask<TRes>.Successful(handler(req));
    }

    public TRes Sync(TReq request, RpcTimeouts timeouts = null)
    {
      Assertion.AssertNotNull(myHandler, "myHandler != null");
      return myHandler(myBindLifetime, request).Result.Value.Unwrap();
    }

    public IRdTask<TRes> Start(TReq request, IScheduler responseScheduler = null)
    {
      return myHandler(myBindLifetime, request);
    }
  }
}