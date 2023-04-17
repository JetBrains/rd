using System;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;

namespace JetBrains.Rd.Base
{
  public abstract class RdReactiveBase : RdBindableBase, IRdReactive
  {    
    internal static readonly ILog ourLogReceived = Protocol.Logger.GetSublogger("RECV");
    internal static readonly ILog ourLogSend = Protocol.Logger.GetSublogger("SEND");
    
    internal static LogWithLevel? ReceiveTrace => ourLogReceived.WhenTrace();
    internal static LogWithLevel? SendTrace => ourLogSend.WhenTrace();


    #region Assertion
    
    public bool Async { get; set; }

    [AssertionMethod]
    protected void AssertThreading()
    {
      if (!Async && AllowBindCookie.IsBindNotAllowed && TryGetProto() is {} proto)
        proto.Scheduler.AssertThread(this);
    }

    public bool ValueCanBeNull { get; set; }

    protected void AssertNullability<T>(T value)
    {
      
      if ( //optimization for memory traffic 
        typeof(T).IsValueType || ValueCanBeNull || value != null) return;

      Assertion.Fail("Value is defined as not nullable: {0}", this);
    }

    [AssertionMethod]
    protected void AssertBound()
    {
      Assertion.Require(IsBound, "Not bound: {0}", this);
    }

    #endregion 


    #region Local change
    
    public bool IsLocalChange { get; protected set; }
    
    protected internal struct LocalChangeCookie : IDisposable
    {
      private readonly RdReactiveBase myHost;
      private FirstChanceExceptionInterceptor.ThreadLocalDebugInfo myDebugInfo;

      internal LocalChangeCookie(RdReactiveBase host)
      {
        (myHost = host).IsLocalChange = true;
        myDebugInfo = host.UsingDebugInfo();
      }      

      public void Dispose()
      {
        myHost.IsLocalChange = false;
        myDebugInfo.Dispose();
      }
    }

    protected internal FirstChanceExceptionInterceptor.ThreadLocalDebugInfo UsingDebugInfo()
    {
      return new FirstChanceExceptionInterceptor.ThreadLocalDebugInfo(this);
    }
    
    protected internal LocalChangeCookie UsingLocalChange()
    {
      if (Mode.IsAssertion) Assertion.Assert(!IsLocalChange, "!IsLocalChange: {0}", this);
      if (IsBound && !Async) AssertThreading();
      return new LocalChangeCookie(this);
    }
    #endregion


    #region From interface

    public RdWireableContinuation OnWireReceived(Lifetime lifetime, UnsafeReader reader)
    {
      var proto = TryGetProto();
      if (proto == null || !TryGetSerializationContext(out var serializationCtx) || lifetime.IsNotAlive)
        return RdWireableContinuation.NotBound;
      
      return OnWireReceived(lifetime, proto, serializationCtx, reader);
    }
    
    public abstract RdWireableContinuation OnWireReceived(Lifetime lifetime, IProtocol proto, SerializationCtx ctx, UnsafeReader reader);

    #endregion
    
  }
}