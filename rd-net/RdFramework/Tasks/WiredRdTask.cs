using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;

namespace JetBrains.Rd.Tasks
{
  class WiredRdTask<TReq, TRes> : RdTask<TRes>, IRdWireable
  {
    private readonly RdCall<TReq, TRes> myCall;
    private readonly bool myIsEndpoint;
    public RdId RdId { get; }
    public IScheduler WireScheduler { get; }

    private IWire myWire;

    public IProtocol Proto => myCall.Proto;
    public SerializationCtx SerializationContext => myCall.SerializationContext;
    public RName Location { get; }

    public WiredRdTask(RdCall<TReq, TRes> call, RdId rdId,
      IScheduler wireScheduler, bool isEndpoint)
    {
      myCall = call;
      myIsEndpoint = isEndpoint;
      RdId = rdId;
      WireScheduler = wireScheduler;
      myWire = call.Wire;
      Location = call.Location.Sub(rdId);
    }

    
    
    internal Lifetime Subscribe(Lifetime outerLifetime)
    {
      var taskWireSubscriptionDefinition = outerLifetime.CreateNested();
      var externalCancellation = outerLifetime.CreateNested();
      
      myCall.Wire.Advise(taskWireSubscriptionDefinition.Lifetime, this); //this lifetimeDef listen only one value
      taskWireSubscriptionDefinition.Lifetime.TryOnTermination(() => ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled()));
      
      Result.AdviseOnce(Lifetime.Eternal, taskResult =>
      {
        try
        {
          var potentiallyBindable = taskResult.Result;
          if (potentiallyBindable.IsBindable())
          {
            if (myIsEndpoint)
              potentiallyBindable.IdentifyPolymorphic(myCall.Proto.Identities, myCall.RdId.Mix(RdId.ToString()));

            potentiallyBindable.BindPolymorphic(externalCancellation.Lifetime, myCall, RdId.ToString());
          }

          if (myIsEndpoint)
          {
            if (taskResult.Status == RdTaskStatus.Canceled)
            {
              externalCancellation.Terminate();
            }

            Trace(RdReactiveBase.ourLogSend, "send response", taskResult);
            myWire.Send(RdId,
              writer =>
              {
                RdTaskResult<TRes>.Write(myCall.WriteResponseDelegate, myCall.SerializationContext, writer, taskResult);
              });
          }
          else if (taskResult.Status == RdTaskStatus.Canceled) //we need to transfer cancellation to the other side
          {
            Trace(RdReactiveBase.ourLogSend, "send cancellation");
            myWire.Send(RdId, writer => { writer.Write(Unit.Instance); }); //send cancellation to the other side
          }
        }
        finally
        {
          taskWireSubscriptionDefinition.Terminate(); //no need to listen result or cancellation from wire
        }
      });

      return externalCancellation.Lifetime;
    }


    //received response from wire
    public void OnWireReceived(UnsafeReader reader)
    {
      using (myCall.UsingDebugInfo())
      {
        if (myIsEndpoint) //we are on endpoint side, so listening for cancellation
        {
          Trace(RdReactiveBase.ourLogReceived, "received cancellation");
          reader.ReadVoid(); //nothing just a void value
          ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled());
        }

        else // we are at call side, so listening no response and bind it if it's bindable
        {
          var resultFromWire = RdTaskResult<TRes>.Read(myCall.ReadResponseDelegate, myCall.SerializationContext, reader);
          Trace(RdReactiveBase.ourLogReceived, "received response", resultFromWire);
          
          if (!ResultInternal.SetIfEmpty(resultFromWire))
            Trace(RdReactiveBase.ourLogReceived, "response from wire was rejected because task already has result");
        }
      }
    }

    private void Trace(ILog log, string message, object additional = null)
    {
      if (!log.IsTraceEnabled())
        return;
      
      log.Trace($"{this} :: {message}" + (additional != null ? ": " + additional.PrintToString() : ""));
    }

    public override string ToString() => $"{myCall}, taskId={RdId}";

  }
}