using System;
using System.Diagnostics;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;

namespace JetBrains.Rd.Tasks
{
  public class RdCall<TReq, TRes> : RdReactiveBase, IRdCall<TReq, TRes>, IRdEndpoint<TReq, TRes>
  {
    [PublicAPI]
    public CtxReadDelegate<TReq> ReadRequestDelegate { get; }

    [PublicAPI]
    public CtxWriteDelegate<TReq> WriteRequestDelegate { get; }

    [PublicAPI]
    public CtxReadDelegate<TRes> ReadResponseDelegate { get; }

    [PublicAPI]
    public CtxWriteDelegate<TRes> WriteResponseDelegate { get; }

    //set in init
    internal new SerializationCtx SerializationContext;

    //set via Set method
    [PublicAPI] public Func<Lifetime, TReq, RdTask<TRes>> Handler { get; private set; }





    private Lifetime myBindLifetime;
    private IScheduler myEndpointSchedulerForHandlerAndCancellation;

    public RdCall(CtxReadDelegate<TReq> readRequest, CtxWriteDelegate<TReq> writeRequest, CtxReadDelegate<TRes> readResponse, CtxWriteDelegate<TRes> writeResponse)
    {
      ReadRequestDelegate = readRequest;
      WriteRequestDelegate = writeRequest;
      ReadResponseDelegate = readResponse;
      WriteResponseDelegate = writeResponse;
    }

    public RdCall() : this(Polymorphic<TReq>.Read, Polymorphic<TReq>.Write, Polymorphic<TRes>.Read, Polymorphic<TRes>.Write)
    {
    }


    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      myBindLifetime = lifetime;

      //because we advise synchronous scheduler
      SerializationContext = base.SerializationContext;

      Wire.Advise(lifetime, this);
    }


    [PublicAPI]
    public void Set(Func<Lifetime, TReq, RdTask<TRes>> handler, IScheduler cancellationAndRequestScheduler = null)
    {
      Handler = handler;
      myEndpointSchedulerForHandlerAndCancellation = cancellationAndRequestScheduler;
    }

    [PublicAPI]
    public override void OnWireReceived(UnsafeReader reader) //endpoint's side
    {
      var taskId = RdId.Read(reader);
      
      var wiredTask = new WiredRdTask<TReq, TRes>(this, taskId, myEndpointSchedulerForHandlerAndCancellation ?? WireScheduler, true);
      //subscribe for lifetime cancellation
      var externalCancellation = wiredTask.Subscribe(myBindLifetime);

      using (UsingDebugInfo()) //now supports only sync handlers
      {
        RdTask<TRes> rdTask;
        try
        {
          var value = ReadRequestDelegate(SerializationContext, reader);
          ReceiveTrace?.Log($"{this} taskId={taskId}, request = {value.PrintToString()}");
          rdTask = Handler(externalCancellation, value);
        }
        catch (Exception e)
        {
          rdTask = RdTask<TRes>.Faulted(e);
        }
        
        rdTask.Result.Advise(Lifetime.Eternal, result =>
          {
            try
            {
              if (result.Status == RdTaskStatus.Success)
                AssertNullability(result.Result);

              wiredTask.ResultInternal.SetIfEmpty(result);
            }
            catch (Exception ee)
            {
              ourLogSend.Error($"Problem when responding to {this}, taskId={taskId}", ee);
              wiredTask.Set(new RdFault(ee));
            }
          });
      }
    }
    

    public TRes Sync(TReq request, RpcTimeouts timeouts = null)
    {
      AssertBound();
      if (!Async) AssertThreading();

      var task = StartInternal(Lifetime.Eternal, request, SynchronousScheduler.Instance);

      var stopwatch = new Stopwatch();
      stopwatch.Start();

      RpcTimeouts timeoutsToUse;
      if (RpcTimeouts.RespectRpcTimeouts)
        timeoutsToUse = timeouts ?? RpcTimeouts.Default;
      else
        timeoutsToUse = timeouts == null
          ? RpcTimeouts.Maximal
          : RpcTimeouts.Max(timeouts, RpcTimeouts.Maximal);

      if (!task.Wait(timeoutsToUse.ErrorAwaitTime))
      {
        throw new TimeoutException($"Sync execution of rpc `{Location}` is timed out in {timeoutsToUse.ErrorAwaitTime.TotalMilliseconds} ms");
      }

      stopwatch.Stop();

      var freezeTime = stopwatch.ElapsedMilliseconds;
      if (freezeTime > timeoutsToUse.WarnAwaitTime.TotalMilliseconds)
      {
        Log.Root.Error("Sync execution of rpc `{0}` executed too long: {1} ms", Location, freezeTime);
      }

      return task.Result.Value.Unwrap();
    }


    public IRdTask<TRes> Start(TReq request, IScheduler responseScheduler = null)
      => StartInternal(Lifetime.Eternal, request, responseScheduler ?? Proto.Scheduler);

    public IRdTask<TRes> Start(Lifetime lifetime, TReq request, IScheduler responseScheduler = null)
      => StartInternal(lifetime, request, responseScheduler ?? Proto.Scheduler);


    private IRdTask<TRes> StartInternal(Lifetime requestLifetime, TReq request, [NotNull] IScheduler scheduler)
    {
      AssertBound();
      if (!Async) AssertThreading();
      AssertNullability(request);

      var taskId = Proto.Identities.Next(RdId.Nil);
      var task = new WiredRdTask<TReq, TRes>(this, taskId, scheduler, false);
      
      //no need for cancellationLifetime on call site
      var _ = task.Subscribe(Lifetime.Intersect(requestLifetime, myBindLifetime));
      Wire.Send(RdId, (writer) =>
      {
        SendTrace?.Log($"{this} send request '{taskId}' : {request.PrintToString()}");

        taskId.Write(writer);
        WriteRequestDelegate(SerializationContext, writer, request);
      });

      return task;
    }

    public static RdCall<TReq, TRes> Read(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<TReq> readRequest, CtxWriteDelegate<TReq> writeRequest, CtxReadDelegate<TRes> readResponse, CtxWriteDelegate<TRes> writeResponse)
    {
      return new RdCall<TReq, TRes>(readRequest, writeRequest, readResponse, writeResponse).WithId(reader.ReadRdId());
    }

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdCall<TReq, TRes> value)
    {
      value.RdId.Write(writer);
    }
  }
}