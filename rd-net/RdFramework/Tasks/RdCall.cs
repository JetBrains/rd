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

    protected override void PreInit(Lifetime lifetime, IProtocol proto)
    {
      base.PreInit(lifetime, proto);
      
      Assertion.Assert(myBindLifetime.Status == LifetimeStatus.Terminated);
      myBindLifetime = lifetime;

      proto.Wire.Advise(lifetime, this);
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
    public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper)
    {
      var taskId = RdId.Read(reader);

      var wiredTask = new WiredRdTask<TReq, TRes>.Endpoint(dispatchHelper.Lifetime, this, taskId, myCancellationScheduler ?? SynchronousScheduler.Instance);
      try
      {
        OnWireReceived(ctx, reader, wiredTask, dispatchHelper);
      }
      catch (Exception e)
      {
        wiredTask.Set(e);
      }
    }

    private void OnWireReceived(SerializationCtx ctx, UnsafeReader reader, WiredRdTask<TReq, TRes>.Endpoint wiredTask, IRdWireableDispatchHelper dispatchHelper)
    {
      var externalCancellation = wiredTask.Lifetime;
      var value = ReadRequestDelegate(ctx, reader);
      ReceiveTrace?.Log($"OnWireReceived:: {wiredTask} :: received request: {value.PrintToString()}");

      dispatchHelper.Dispatch(myHandlerScheduler, () =>
      {
        ReceiveTrace?.Log($"Dispatched:: {wiredTask} :: received request: {value.PrintToString()}");
        var rdTask = RunHandler(value, externalCancellation, wiredTask);

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
      });
    }

    private RdTask<TRes> RunHandler(TReq value, Lifetime externalCancellation, object? moniker)
    {
      RdTask<TRes> rdTask;
      try
      {
        var handler = Handler;
        if (handler == null)
        {
          var message = $"Handler is not set for {moniker} :: received request: {value.PrintToString()}";
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
        rdTask = RdTask.Faulted<TRes>(new Exception($"Unexpected exception in {moniker}", e));
      }

      return rdTask;
    }


    public TRes Sync(TReq request, RpcTimeouts? timeouts = null)
    {
      AssertBound();
      if (!Async) AssertThreading();

      var task = StartInternal(Lifetime.Eternal, request, SynchronousScheduler.Instance);

      var stopwatch = new Stopwatch();
      stopwatch.Start();

      var timeoutsToUse = RpcTimeouts.GetRpcTimeouts(timeouts);
      if (!task.Wait(timeoutsToUse.WarnAwaitTime))
      {
        var deltaAwaitTime = timeoutsToUse.ErrorAwaitTime - timeoutsToUse.WarnAwaitTime;
        var res = deltaAwaitTime > TimeSpan.Zero && task.Wait(deltaAwaitTime);
        stopwatch.Stop();
        
        if (!res)
          throw new TimeoutException($"Sync execution of rpc `{Location}` is timed out in {timeoutsToUse.ErrorAwaitTime.TotalMilliseconds} ms, the freeze time is {stopwatch.ElapsedMilliseconds} ms");
        Log.Root.Error("Sync execution of rpc `{0}` executed too long: {1} ms, the freeze time: {2} ms", Location, timeoutsToUse.WarnAwaitTime.TotalMilliseconds, stopwatch.ElapsedMilliseconds);
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

      var task = CreateCallSite(requestLifetime, (lifetime) => new WiredRdTask<TReq, TRes>.CallSite(lifetime, this, taskId, scheduler ?? proto.Scheduler));

      if (proto.Wire.IsStub)
      {
        var taskResult = RunHandler(request, task.Lifetime, moniker: this).Result;
        taskResult.AdviseOnce(requestLifetime, result =>
        {
          if (result.Result.IsBindable())
          {
            // we mock the endpoint side, since we are on stub wire, so identify bindable result here
            result.Result.IdentifyPolymorphic(proto.Identities, proto.Identities.Mix(RdId, taskId.ToString()));
          }
          
          task.OnResultReceived(result, new SynchronousDispatchHelper(taskId, requestLifetime));
        });
        return task;
      }
      
      using var cookie = task.Lifetime.UsingExecuteIfAlive();
      if (cookie.Succeed)
      {
        proto.Wire.Send(RdId, (writer) =>
        {
          SendTrace?.Log($"{task} :: send request: {request.PrintToString()}");

          taskId.Write(writer);
          WriteRequestDelegate(serializationContext, writer, request);
        });
      }

      return task;
    }

    private WiredRdTask<TReq, TRes>.CallSite CreateCallSite(Lifetime requestLifetime, Func<Lifetime, WiredRdTask<TReq, TRes>.CallSite> createTask)
    {
      if (requestLifetime.IsEternal)
        return createTask(myBindLifetime);
      
      var intersectedDef = Lifetime.DefineIntersection(requestLifetime, myBindLifetime);
      var task = createTask(intersectedDef.Lifetime);
      task.Result.Advise(intersectedDef.Lifetime, result =>
      {
        if (result.Status != RdTaskStatus.Success || !result.Result.IsBindable())
        {
          intersectedDef.AllowTerminationUnderExecution = true;
          intersectedDef.Terminate();
        }
      });

      return task;
    }
    
    private class SynchronousDispatchHelper : IRdWireableDispatchHelper
    {
      public SynchronousDispatchHelper(RdId rdId, Lifetime lifetime)
      {
        RdId = rdId;
        Lifetime = lifetime;
      }

      public RdId RdId { get; }
      public Lifetime Lifetime { get; }
      public void Dispatch(IScheduler? scheduler, Action action) => action();
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