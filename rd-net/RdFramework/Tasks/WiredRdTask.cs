using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;

namespace JetBrains.Rd.Tasks
{
  internal abstract class WiredRdTask<TReq, TRes> : RdTask<TRes>, IRdWireable
  {
    private readonly RdCall<TReq, TRes> myCall;
    public RdId RdId { get; }
    public IScheduler WireScheduler { get; }

    private readonly IWire myWire;

    public IProtocol Proto => myCall.Proto;
    public SerializationCtx SerializationContext => myCall.SerializationContext;
    public RName Location { get; }

    protected WiredRdTask(RdCall<TReq, TRes> call, RdId rdId, IScheduler wireScheduler)
    {
      myCall = call;
      RdId = rdId;
      WireScheduler = wireScheduler;
      myWire = call.Wire;
      Location = call.Location.Sub(rdId);
    }

    //received response from wire
    public abstract void OnWireReceived(UnsafeReader reader);

    protected void Trace(ILog log, string message, object additional = null)
    {
      if (!log.IsTraceEnabled())
        return;
      
      log.Trace($"{this} :: {message}" + (additional != null ? ": " + additional.PrintToString() : ""));
    }

    public override string ToString() => $"{myCall}, taskId={RdId}";
    
    
    
    
    internal class CallSite : WiredRdTask<TReq, TRes>
    {
      public CallSite(Lifetime outerLifetime, RdCall<TReq, TRes> call, RdId rdId, IScheduler wireScheduler) : base(call, rdId, wireScheduler)
      {
        var taskWireSubscriptionDefinition = outerLifetime.CreateNested();
      
        myCall.Wire.Advise(taskWireSubscriptionDefinition.Lifetime, this); //this lifetimeDef listen only one value
        taskWireSubscriptionDefinition.Lifetime.TryOnTermination(() => ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled()));

        Result.AdviseOnce(Lifetime.Eternal, taskResult =>
        {
          taskWireSubscriptionDefinition.Terminate(); //no need to listen result or cancellation from wire

          var potentiallyBindable = taskResult.Result;
          if (potentiallyBindable.IsBindable())
          {
            potentiallyBindable.BindPolymorphic(outerLifetime, myCall, RdId.ToString());
            if (!outerLifetime.TryOnTermination(SendCancellation))
              SendCancellation();
          } 
          else if (taskResult.Status == RdTaskStatus.Canceled)  //we need to transfer cancellation to the other side
            SendCancellation();
        });
      }
    
      private void SendCancellation()
      {
        Trace(RdReactiveBase.ourLogSend, "send cancellation");
        myWire.Send(RdId, writer => writer.Write(Unit.Instance)); //send cancellation to the other side
      }

      public override void OnWireReceived(UnsafeReader reader)
      {
        using (myCall.UsingDebugInfo())
        {
          // we are at call side, so listening no response and bind it if it's bindable
          var resultFromWire = RdTaskResult<TRes>.Read(myCall.ReadResponseDelegate, myCall.SerializationContext, reader);
          Trace(RdReactiveBase.ourLogReceived, "received response", resultFromWire);

          if (!ResultInternal.SetIfEmpty(resultFromWire))
            Trace(RdReactiveBase.ourLogReceived, "response from wire was rejected because task already has result");
        }
      }
    }

    
    
    
    internal class Endpoint : WiredRdTask<TReq, TRes>
    {
      private readonly LifetimeDefinition myDef;
      internal Lifetime Lifetime => myDef.Lifetime;
    
      public Endpoint(Lifetime bindLifetime, RdCall<TReq, TRes> call, RdId rdId, IScheduler wireScheduler) : base(call, rdId, wireScheduler)
      {
        myDef = bindLifetime.CreateNested();
        
        myCall.Wire.Advise(Lifetime, this);
        Lifetime.TryOnTermination(() => ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled()));
        
        Result.AdviseOnce(Lifetime.Eternal, taskResult =>
        {
          var potentiallyBindable = taskResult.Result;
          if (potentiallyBindable.IsBindable())
          {
            potentiallyBindable.IdentifyPolymorphic(myCall.Proto.Identities, myCall.RdId.Mix(RdId.ToString()));
            potentiallyBindable.BindPolymorphic(Lifetime, myCall, RdId.ToString());
          }
          else
          {
            myDef.Terminate();
          }

          Trace(RdReactiveBase.ourLogSend, "send response", taskResult);
          myWire.Send(RdId,
            writer =>
            {
              RdTaskResult<TRes>.Write(myCall.WriteResponseDelegate, myCall.SerializationContext, writer, taskResult);
            });
        });
      }

      public override void OnWireReceived(UnsafeReader reader)
      {
        using (myCall.UsingDebugInfo())
        {
          //we are on endpoint side, so listening for cancellation
          Trace(RdReactiveBase.ourLogReceived, "received cancellation");
          reader.ReadVoid(); //nothing just a void value
          ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled());
          myDef.Terminate();
        }
      }
    }
  }
}