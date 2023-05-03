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

    //set via Set method
    [PublicAPI] public Func<Lifetime, TReq, RdTask<TRes>>? Handler { get; private set; }





    private Lifetime myBindLifetime;
    private IScheduler? myCancellationScheduler;
    private IScheduler? myHandlerScheduler;

    public RdCall(CtxReadDelegate<TReq> readRequest, CtxWriteDelegate<TReq> writeRequest, CtxReadDelegate<TRes> readResponse, CtxWriteDelegate<TRes> writeResponse)
    {
      ReadRequestDelegate = readRequest;
      WriteRequestDelegate = writeRequest;
      ReadResponseDelegate = readResponse;
      WriteResponseDelegate = writeResponse;
      myBindLifetime = Lifetime.Terminated;
    }

    protected override void PreInit(Lifetime lifetime, IProtocol parentProto)
    {
      base.PreInit(lifetime, parentProto);
      
      Assertion.Assert(myBindLifetime.Status == LifetimeStatus.Terminated);
      myBindLifetime = lifetime;

      parentProto.Wire.Advise(lifetime, this);
    }


    public void SetRdTask(Func<Lifetime, TReq, RdTask<TRes>> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null)
    {
      Handler = handler;
      myCancellationScheduler = cancellationScheduler;
      myHandlerScheduler = handlerScheduler;
    }

    [Obsolete("This is an internal API. It is preferable to use SetSync or SetAsync extension methods")]
    public void Set(Func<Lifetime, TReq, RdTask<TRes>> handler, IScheduler? cancellationScheduler = null, IScheduler? handlerScheduler = null)
    {
      SetRdTask(handler, cancellationScheduler, handlerScheduler);
    }
    
    [PublicAPI]
    public override RdWireableContinuation OnWireReceived(Lifetime lifetime, IProtocol proto, SerializationCtx ctx, UnsafeReader reader)
    {
      var taskId = RdId.Read(reader);

      var wiredTask = new WiredRdTask<TReq, TRes>.Endpoint(lifetime, this, taskId, myCancellationScheduler ?? SynchronousScheduler.Instance);
      try
      {
        return OnWireReceived(lifetime, proto, ctx, reader, wiredTask);
      }
      catch (Exception e)
      {
        wiredTask.Set(e);
        return RdWireableContinuation.Empty;
      }
      //subscribe for lifetime cancellation
    }

    private RdWireableContinuation OnWireReceived(Lifetime lifetime, IProtocol proto, SerializationCtx ctx, UnsafeReader reader, WiredRdTask<TReq, TRes>.Endpoint wiredTask)
    {
      var externalCancellation = wiredTask.Lifetime;
      var value = ReadRequestDelegate(ctx, reader);
      ReceiveTrace?.Log($"{wiredTask} :: received request: {value.PrintToString()}");

      return new RdWireableContinuation(lifetime, myHandlerScheduler ?? proto.Scheduler, () =>
      {
        using (UsingDebugInfo()) //now supports only sync handlers
        {
          RdTask<TRes> rdTask;
          try
          {
            var handler = Handler;
            if (handler == null)
            {
              var message = $"Handler is not set for {wiredTask} :: received request: {value.PrintToString()}";
              ourLogReceived.Error(message);
              rdTask = RdTask.Faulted<TRes>(new Exception(message));
            }
            else
            {
              try
              {
                rdTask = handler(externalCancellation, value);
              }
              catch (Exception ex)
              {
                rdTask = RdTask.Faulted<TRes>(ex);
              }
            }
          }
          catch (Exception e)
          {
            rdTask = RdTask.Faulted<TRes>(new Exception($"Unexpected exception in {wiredTask}", e));
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
              ourLogSend.Error($"Problem when responding to `{wiredTask}`", ee);
              wiredTask.Set(new RdFault(ee));
            }
          });
        }
      });
    }


    public TRes Sync(TReq request, RpcTimeouts? timeouts = null)
    {
      AssertBound();
      if (!Async) AssertThreading();

      var task = StartInternal(Lifetime.Eternal, request, SynchronousScheduler.Instance);

      var stopwatch = new Stopwatch();
      stopwatch.Start();

      var timeoutsToUse = RpcTimeouts.GetRpcTimeouts(timeouts);
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


    public IRdTask<TRes> Start(TReq request, IScheduler? responseScheduler = null)
      => StartInternal(Lifetime.Eternal, request, responseScheduler);

    public IRdTask<TRes> Start(Lifetime lifetime, TReq request, IScheduler? responseScheduler = null)
      => StartInternal(lifetime, request, responseScheduler);


    private IRdTask<TRes> StartInternal(Lifetime requestLifetime, TReq request, IScheduler? scheduler)
    {
      var proto = TryGetProto();
      
      if (!Async)
        AssertBound();
      
      AssertThreading();
      AssertNullability(request);

      if (proto == null || !TryGetSerializationContext(out var serializationContext))
        return new WiredRdTask<TReq, TRes>.CallSite(Lifetime.Terminated, this, RdId.Nil, SynchronousScheduler.Instance);

      var taskId = proto.Identities.Next(RdId.Nil);
      var task = new WiredRdTask<TReq,TRes>.CallSite(Lifetime.Intersect(requestLifetime, myBindLifetime), this, taskId, scheduler ?? proto.Scheduler);
      
      proto.Wire.Send(RdId, (writer) =>
      {
        SendTrace?.Log($"{task} :: send request: {request.PrintToString()}");

        taskId.Write(writer);
        WriteRequestDelegate(serializationContext, writer, request);
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

    protected override string ShortName => Handler == null ? "call" : "endpoint";
  }
}