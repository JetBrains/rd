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
    private readonly LifetimeDefinition myWireAdviseLifetime;

    private readonly RdCall<TReq, TRes> myCall;
    private readonly bool myIsEndpoint;
    public RdId RdId { get; }
    public IScheduler WireScheduler { get; }

    private IWire myWire;

    public WiredRdTask(LifetimeDefinition wireAdviseLifetime, RdCall<TReq, TRes> call, RdId rdId,
      IScheduler wireScheduler, bool isEndpoint)
    {
      myWireAdviseLifetime = wireAdviseLifetime;
      myCall = call;
      myIsEndpoint = isEndpoint;
      RdId = rdId;
      WireScheduler = wireScheduler;
      myWire = call.Wire;
    }


    public void Subscribe(Lifetime lifetime)
    {
      var taskWireSubscriptionDefinition = lifetime.CreateNested();
      myCall.Wire.Advise(taskWireSubscriptionDefinition.Lifetime, this); //this lifetimeDef listen only one value

      Result.AdviseOnce(lifetime, taskResult =>
      {
        taskWireSubscriptionDefinition.Terminate(); //no need to listen result or cancellation from wire

        var potentiallyBindable = taskResult.Result;
        if (potentiallyBindable.IsBindable())
        {
          if (myIsEndpoint)
            potentiallyBindable.IdentifyPolymorphic(myCall.Proto.Identities, myCall.RdId.Mix(RdId.ToString()));

          potentiallyBindable.BindPolymorphic(myWireAdviseLifetime.Lifetime, myCall, RdId.ToString());
        }

        if (myIsEndpoint)
        {
          Trace(RdReactiveBase.LogSend, "send response", taskResult);
          myWire.Send(RdId,
            writer =>
            {
              RdTaskResult<TRes>.Write(myCall.WriteResponseDelegate, myCall.SerializationContext, writer, taskResult);
            });
        }
        else if (taskResult.Status == RdTaskStatus.Canceled) //we need to transfer cancellation to the other side
        {
          Trace(RdReactiveBase.LogSend, "send cancellation");
          myWire.Send(RdId, writer => { writer.Write(Unit.Instance); }); //send cancellation to the other side
        }
      });

      lifetime.TryOnTermination(() => ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled()));
    }


    //received response from wire
    public void OnWireReceived(UnsafeReader reader)
    {
      using (myCall.UsingDebugInfo())
      {
        if (myIsEndpoint) //we are on endpoint side, so listening for cancellation
        {
          Trace(RdReactiveBase.LogReceived, "received cancellation");
          reader.ReadVoid(); //nothing just a void value
          ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled());
        }

        else // we are at call side, so listening no response and bind it if it's bindable
        {
          var resultFromWire = RdTaskResult<TRes>.Read(myCall.ReadResponseDelegate, myCall.SerializationContext, reader);
          Trace(RdReactiveBase.LogReceived, "received response", resultFromWire);
          
          if (!ResultInternal.SetIfEmpty(resultFromWire))
            Trace(RdReactiveBase.LogReceived, "response from wire was rejected because task already has result");
        }
      }
    }

    private void Trace(ILog log, string message, object additional = null)
    {
      if (!log.IsTraceEnabled())
        return;
      
      log.Trace((myIsEndpoint ? "endpoint": "call") + $" `{myCall}` :: taskId={RdId} :: {message} "+additional?.PrintToString() );
    }
  }
}