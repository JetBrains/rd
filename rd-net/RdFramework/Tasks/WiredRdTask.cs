using System;
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
    public bool IsBound => myCall.IsBound;
    public IScheduler WireScheduler { get; }

    public IWire? Wire;
    public RName Location { get; }


    protected WiredRdTask(RdCall<TReq, TRes> call, RdId rdId, IScheduler wireScheduler)
    {
      myCall = call;
      RdId = rdId;
      Wire = call.TryGetProto()?.Wire;
      WireScheduler = wireScheduler;
      Location = call.Location.Sub(rdId.ToString());
    }

    public IProtocol? TryGetProto() => myCall.TryGetProto();
    public bool TryGetSerializationContext(out SerializationCtx ctx) => myCall.TryGetSerializationContext(out ctx);

    //received response from wire
    public void OnWireReceived(UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper)
    {
      var proto = TryGetProto();
      if (proto == null || !TryGetSerializationContext(out var ctx) || dispatchHelper.Lifetime.IsNotAlive)
      {
        RdReactiveBase.ourLogReceived.Trace($"{this} is not bound. Message for ({dispatchHelper.RdId} will not be processed");
        return;
      }

      OnWireReceived(proto, ctx, reader, dispatchHelper);
    }
    
    public abstract void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper);

    protected void Trace(ILog log, string message, object? additional = null)
    {
      if (!log.IsTraceEnabled())
        return;
      
      log.Trace($"{this} :: {message}" + (additional != null ? ": " + additional.PrintToString() : ""));
    }

    public override string ToString() => $"{myCall}, taskId={RdId}";
    
    
    
    
    internal class CallSite : WiredRdTask<TReq, TRes>
    {
      private readonly Lifetime myOuterLifetime;

      public Lifetime Lifetime => myOuterLifetime;

      public CallSite(Lifetime outerLifetime, RdCall<TReq, TRes> call, RdId rdId, IScheduler wireScheduler) : base(call, rdId, wireScheduler)
      {
        myOuterLifetime = outerLifetime;
        var taskWireSubscriptionDefinition = outerLifetime.CreateNested();
      
        Wire?.Advise(taskWireSubscriptionDefinition.Lifetime, this); //this lifetimeDef listen only one value
        if (!taskWireSubscriptionDefinition.Lifetime.TryOnTermination(() => ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled())))
          ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled());

        Result.AdviseOnce(Lifetime.Eternal, taskResult =>
        {
          taskWireSubscriptionDefinition.Terminate(); //no need to listen result or cancellation from wire
          if (taskResult.Status == RdTaskStatus.Canceled)  //we need to transfer cancellation to the other side
            SendCancellation();
        });
      }
    
      private void SendCancellation()
      {
        Trace(RdReactiveBase.ourLogSend, "send cancellation");
        Wire?.Send(RdId, writer => writer.Write(Unit.Instance)); //send cancellation to the other side
      }

      public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper)
      {
        // we are at call side, so listening no response and bind it if it's bindable
        var taskResult = RdTaskResult<TRes>.Read(myCall.ReadResponseDelegate, ctx, reader);
        OnResultReceived(taskResult, dispatchHelper);
      }

      internal void OnResultReceived(RdTaskResult<TRes> taskResult, IRdWireableDispatchHelper dispatchHelper)
      {
        Trace(RdReactiveBase.ourLogReceived, "received response", taskResult);

        var maybe = ResultInternal.Maybe;
        if (maybe.HasValue)
        {
          RdReactiveBase.ourLogReceived.Trace($"{nameof(ResultInternal)} already has a value: {maybe.Value}");
          return;
        }
        
        var potentiallyBindable = taskResult.Result;
        if (potentiallyBindable.IsBindable())
        {
          var definition = new LifetimeDefinition { Id = potentiallyBindable };

          try
          {
            var valueBindLifetime = definition.Lifetime;
            valueBindLifetime.OnTermination(SendCancellation);
            potentiallyBindable.PreBindPolymorphic(valueBindLifetime, myCall, RdId.ToString());
            potentiallyBindable.BindPolymorphic();

            myOuterLifetime.Definition.Attach(definition, true);
          }
          catch
          {
            definition.Terminate();
            throw;
          }
        } 
        else if (taskResult.Status == RdTaskStatus.Canceled)  //we need to transfer cancellation to the other side
          SendCancellation();


        dispatchHelper.Dispatch(WireScheduler, () =>
        {
          if (!ResultInternal.SetIfEmpty(taskResult))
            Trace(RdReactiveBase.ourLogReceived, "response from wire was rejected because task already has result");
        });
      }
    }

    
    
    
    internal class Endpoint : WiredRdTask<TReq, TRes>
    {
      private readonly LifetimeDefinition myDef;
      internal Lifetime Lifetime => myDef.Lifetime;
    
      public Endpoint(Lifetime bindLifetime, RdCall<TReq, TRes> call, RdId rdId, IScheduler wireScheduler) : base(call, rdId, wireScheduler)
      {
        myDef = bindLifetime.CreateNested();
        myDef.Id = this;
        
        Wire?.Advise(Lifetime, this);
        Lifetime.TryOnTermination(() => ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled()));
        
        if (!TryGetSerializationContext(out var serializationCtx))
          return;

        Result.AdviseOnce(Lifetime.Eternal, taskResult =>
        {
          var proto = myCall.TryGetProto();
          var potentiallyBindable = taskResult.Result;
          if (potentiallyBindable.IsBindable())
          {
            if (proto == null)
            {
              Wire?.Send(RdId, writer => { RdTaskResult<TRes>.Write(myCall.WriteResponseDelegate, serializationCtx, writer, RdTaskResult<TRes>.Cancelled()); });
              return;
            }

            var bindableRdId = proto.Identities.Next(RdId);
            potentiallyBindable.IdentifyPolymorphic(proto.Identities, bindableRdId);

            using var cookie = Lifetime.UsingExecuteIfAlive();
            if (cookie.Succeed) // lifetime can be terminated from background thread
            {
              potentiallyBindable.PreBindPolymorphic(Lifetime, myCall, bindableRdId.ToString());
              if (Lifetime.IsNotAlive)
                return;

              Wire?.Send(RdId, writer =>
              {
                RdTaskResult<TRes>.Write(myCall.WriteResponseDelegate, serializationCtx, writer, taskResult);
              });
              
              if (Lifetime.IsNotAlive)
                return;
              
              potentiallyBindable.BindPolymorphic();
            }
          }
          else
          {
            myDef.Terminate();
            Wire?.Send(RdId,
              writer =>
              {
                RdTaskResult<TRes>.Write(myCall.WriteResponseDelegate, serializationCtx, writer, taskResult);
              });
          }

          Trace(RdReactiveBase.ourLogSend, "send response", taskResult);
        });
      }

      public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper)
      {
        //we are on endpoint side, so listening for cancellation
        Trace(RdReactiveBase.ourLogReceived, "received cancellation");
        reader.ReadVoid(); //nothing just a void value
        
        dispatchHelper.Dispatch(WireScheduler, () =>
        {
          var success = ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled());
          var protocolScheduler = myCall.TryGetProto()?.Scheduler;
          if (success || protocolScheduler == null)
            myDef.Terminate();
          else if (Lifetime.IsAlive)
            protocolScheduler.Queue(() =>
              myDef.Terminate()); // if the value is already set, it is not a cancellation scenario, but a termination 
        });
      }
    }
  }
}