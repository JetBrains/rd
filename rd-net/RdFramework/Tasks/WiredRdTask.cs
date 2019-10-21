using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

namespace JetBrains.Rd.Tasks
{
  class WiredRdTask<TReq, TRes> : RdTask<TRes>, IRdWireable
  {
    private readonly LifetimeDefinition myLifetimeDef;
    
    private readonly RdCall<TReq, TRes> myCall;
    public RdId RdId { get; }
    public IScheduler Scheduler { get; }
    public IScheduler WireScheduler { get; } = SynchronousScheduler.Instance;

    private IWire myWire;
    
    public WiredRdTask(LifetimeDefinition lifetimeDef, RdCall<TReq, TRes> call, RdId rdId, IScheduler scheduler)
    {
      myLifetimeDef = lifetimeDef;
      myCall = call;
      RdId = rdId;
      Scheduler = scheduler;
      myWire = call.Wire;
      
      call.Wire.Advise(lifetimeDef.Lifetime, this);
      lifetimeDef.Lifetime.TryOnTermination(() =>
      {
        //otherwise it could be successful continuation from Queue
        if (ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled()))
        {
          RdReactiveBase.LogSend.Trace($"call {myCall.Location} ({myCall.RdId}) send cancellation for task '{RdId}'");
          myWire.Send(rdId, writer => { writer.Write(Unit.Instance); }); //send cancellation to the other side
        }
      });
    }


    //received response from wire
    public void OnWireReceived(UnsafeReader reader)
    {
      var resultFromWire = RdTaskResult<TRes>.Read(myCall.ReadResponseDelegate, myCall.SerializationContext, reader);
      
      if (RdReactiveBase.LogReceived.IsTraceEnabled())
        RdReactiveBase.LogReceived.Trace($"call {myCall.Location} ({myCall.RdId}) received response for task '{RdId}' : {resultFromWire.PrintToString()}");
      
      Scheduler.Queue(() =>
      {
        using (myCall.UsingDebugInfo())
        {
          if (ResultInternal.SetIfEmpty(resultFromWire)) return;
        }

        //trace
        if (RdReactiveBase.LogReceived.IsTraceEnabled())
          RdReactiveBase.LogReceived.Trace($"call {myCall.Location} ({myCall.RdId}) response for task '{RdId}' was dropped, because task already has result: {Result.Value}");
        
        myLifetimeDef.Terminate(); //todo not true in case of bindable entities
      });
    }
  }
}