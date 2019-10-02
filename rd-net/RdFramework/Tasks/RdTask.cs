using System;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

#if !NET35

#endif


namespace JetBrains.Rd.Tasks
{
  

  [PublicAPI] public interface IRdTask<T>
  {
    IReadonlyProperty<RdTaskResult<T>> Result { get; }
  }


  public enum RdTaskStatus
  {
    Success,
    Canceled,
    Faulted
  }


  public class RdTask<T> : IRdTask<T>
  {
    internal readonly WriteOnceProperty<RdTaskResult<T>> ResultInternal = new WriteOnceProperty<RdTaskResult<T>>();

    public IReadonlyProperty<RdTaskResult<T>> Result => ResultInternal;

    public void Set(T value)
    {
      ResultInternal.Value = RdTaskResult<T>.Success(value);
    }

    public void SetCancelled()
    {
      ResultInternal.Value = RdTaskResult<T>.Cancelled();
    }

    public void Set(Exception e)
    {
      ResultInternal.Value = RdTaskResult<T>.Faulted(e);
    }

    private static RdTask<T> FromResult(RdTaskResult<T> result)
    {
      var res = new RdTask<T>();
      res.ResultInternal.Value = result;
      return res;
    }

    public static RdTask<T> Successful(T result)
    {
      return FromResult(RdTaskResult<T>.Success(result));
    }


    public static RdTask<T> Faulted(Exception exception)
    {
      return FromResult(RdTaskResult<T>.Faulted(exception));      
    }
  }


  class WiredRdTask<TReq, TRes> : RdTask<TRes>, IRdWireable
  {
    private readonly LifetimeDefinition myLifetimeDef;
    private readonly RdCall<TReq, TRes> myCall;
    public RdId RdId { get; }
    public IScheduler Scheduler { get; }
    public IScheduler WireScheduler { get; } = SynchronousScheduler.Instance; 
    
    public WiredRdTask(LifetimeDefinition lifetimeDef, RdCall<TReq, TRes> call, RdId rdId, IScheduler scheduler)
    {
      myLifetimeDef = lifetimeDef;
      myCall = call;
      RdId = rdId;
      Scheduler = scheduler;
      
      call.Wire.Advise(lifetimeDef.Lifetime, this);
      lifetimeDef.Lifetime.TryOnTermination(() => { ResultInternal.SetIfEmpty(RdTaskResult<TRes>.Cancelled()); });
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
        
        myLifetimeDef.Terminate();
      });
    }
  }






  public interface IRdEndpoint<TReq, TRes>
  {
    void Set(Func<Lifetime, TReq, RdTask<TRes>> handler);
  }

  public interface IRdCall<in TReq, TRes>
  {
    TRes Sync(TReq request, RpcTimeouts timeouts = null);
    IRdTask<TRes> Start(TReq request, IScheduler responseScheduler = null);
  }
  
  
#if !NET35
  public static class RdCallEx
  {
    [NotNull]
    public static Task<TRes> StartAsTask_SyncResponse<TReq, TRes>([NotNull] this IRdCall<TReq, TRes> call, TReq arg)
    {
      return StartAsTask(call, arg, true, x => x);
    }
    
    [NotNull]
    public static Task<TResOut> StartAsTask_SyncResponse<TReq, TResIn, TResOut>([NotNull] this IRdCall<TReq, TResIn> call, TReq arg, [NotNull] Func<TResIn, TResOut> func)
    {
      return StartAsTask(call, arg, true, func);
    }

    [NotNull]
    public static Task<TRes> StartAsTask<TReq, TRes>([NotNull] this IRdCall<TReq, TRes> call, TReq arg)
    {
      return StartAsTask(call, arg, false, x => x);
    }

    [NotNull]
    public static Task<TResOut> StartAsTask<TReq, TResIn, TResOut>([NotNull] this IRdCall<TReq, TResIn> call, TReq arg, bool useSyncResponse, [NotNull] Func<TResIn, TResOut> func)
    {
      if (call == null)
        throw new ArgumentNullException(nameof(call));
      var rdTask = call.Start(arg, useSyncResponse ? SynchronousScheduler.Instance : null);
      var tcs = new TaskCompletionSource<TResOut>();
      rdTask.Result.AdviseOnce(Lifetime.Eternal, result =>
      {
        switch (result.Status)
        {
          case RdTaskStatus.Success:
            tcs.SetResult(func(result.Result));
            break;
          case RdTaskStatus.Canceled:
            tcs.SetCanceled();
            break;
          case RdTaskStatus.Faulted:
            tcs.SetException(result.Error);
            break;
          default:
            Assertion.Fail($"Unexpected task status {result.Status}");
            break;
        }
      });
      return tcs.Task;
    }
  }
#endif

  public class RpcTimeouts
  {
    public static readonly RpcTimeouts Default = new RpcTimeouts(TimeSpan.FromMilliseconds(200), TimeSpan.FromMilliseconds(3000));
    public static readonly RpcTimeouts Maximal = new RpcTimeouts(TimeSpan.FromMilliseconds(30000), TimeSpan.FromMilliseconds(30000));

    public static bool RespectRpcTimeouts = true;

    public static RpcTimeouts Max(RpcTimeouts x, RpcTimeouts y)
    {
      return new RpcTimeouts(
        x.WarnAwaitTime > y.WarnAwaitTime ? x.WarnAwaitTime : y.WarnAwaitTime,
        x.ErrorAwaitTime > y.ErrorAwaitTime ? x.ErrorAwaitTime : y.ErrorAwaitTime);
    }

    public readonly TimeSpan WarnAwaitTime;
    public readonly TimeSpan ErrorAwaitTime;

    public RpcTimeouts(TimeSpan warnAwaitTime, TimeSpan errorAwaitTime)
    {
      WarnAwaitTime = warnAwaitTime;
      ErrorAwaitTime = errorAwaitTime;
    }
  }


  public class RdCall<TReq, TRes> : RdReactiveBase, IRdCall<TReq, TRes>, IRdEndpoint<TReq, TRes>
  {
    [PublicAPI] public CtxReadDelegate<TReq> ReadRequestDelegate { get; }
    [PublicAPI] public CtxWriteDelegate<TReq> WriteRequestDelegate { get; }

    [PublicAPI] public CtxReadDelegate<TRes> ReadResponseDelegate { get; }
    [PublicAPI] public CtxWriteDelegate<TRes> WriteResponseDelegate { get; }

    //set in init
    internal new SerializationCtx SerializationContext;
    
    //set via Set method
    public Func<Lifetime, TReq, RdTask<TRes>> Handler { get; private set; }

    
    
    
    
    private Lifetime myBindLifetime;
    
    public override IScheduler WireScheduler => SynchronousScheduler.Instance;

    public RdCall(CtxReadDelegate<TReq> readRequest, CtxWriteDelegate<TReq> writeRequest, CtxReadDelegate<TRes> readResponse, CtxWriteDelegate<TRes> writeResponse)
    {
      ReadRequestDelegate = readRequest;
      WriteRequestDelegate = writeRequest;
      ReadResponseDelegate = readResponse;
      WriteResponseDelegate = writeResponse;
    }
    
    public RdCall() : this(Polymorphic<TReq>.Read, Polymorphic<TReq>.Write, Polymorphic<TRes>.Read, Polymorphic<TRes>.Write) {}
    
    
    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      myBindLifetime = lifetime;
      
      //because we advise synchronous scheduler
      SerializationContext = base.SerializationContext;

      Wire.Advise(lifetime, this);
    }

    
    [PublicAPI] public void Set(Func<Lifetime, TReq, RdTask<TRes>> handler)
    {
      Handler = handler;
    }

    [PublicAPI] public override void OnWireReceived(UnsafeReader reader)
    {
      var taskId = RdId.Read(reader);
      var value = ReadRequestDelegate(SerializationContext, reader);
      if (LogReceived.IsTraceEnabled()) LogReceived.Trace("endpoint `{0}`::({1}), taskId={2}, request = {3}", Location, RdId, taskId, value.PrintToString());


      RdTask<TRes> rdTask;
      using (UsingDebugInfo()) //now supports only sync handlers
      {
        try
        {
          rdTask = Handler(myBindLifetime, value);
        }
        catch (Exception e)
        {
          rdTask = RdTask<TRes>.Faulted(e);
        }
      }
                
      rdTask.Result.Advise(myBindLifetime, result =>
      {
        if (LogSend.IsTraceEnabled()) LogSend.Trace("endpoint `{0}`::({1}), taskId={2}, response = {3}", Location, RdId, taskId, result.PrintToString());

        RdTaskResult<TRes> validatedResult;
        try
        {
          if (result.Status == RdTaskStatus.Success) AssertNullability(result.Result);
          validatedResult = result;
        }
        catch (Exception e)
        {
          LogSend.Error(e);
          validatedResult = RdTaskResult<TRes>.Faulted(e);
        }

        Wire.Send(taskId, (writer) =>
        {
          RdTaskResult<TRes>.Write(WriteResponseDelegate, SerializationContext, writer, validatedResult);
        });
      });
    }

    public TRes Sync(TReq request, RpcTimeouts timeouts = null)
    {
      AssertBound();
      if (!Async) AssertThreading();

      var task = StartInternal(request, true, SynchronousScheduler.Instance);

      var stopwatch = new Stopwatch();
      stopwatch.Start();

      var timeoutsToUse = RpcTimeouts.RespectRpcTimeouts
        ? timeouts ?? RpcTimeouts.Default
        : timeouts == null ? RpcTimeouts.Maximal : RpcTimeouts.Max(timeouts, RpcTimeouts.Maximal);

      if (!task.Wait(timeoutsToUse.ErrorAwaitTime))
      {
        throw new TimeoutException(string.Format("Sync execution of rpc `{0}` is timed out in {1} ms", Location, timeoutsToUse.ErrorAwaitTime.TotalMilliseconds));
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
    {
      AssertBound();
      return StartInternal(request, false, responseScheduler ?? Proto.Scheduler);
    }


    private IRdTask<TRes> StartInternal(TReq request, bool sync, [NotNull]IScheduler scheduler)
    {
      AssertBound();
      if (!Async) AssertThreading();
      AssertNullability(request);

      var taskId = Proto.Identities.Next(RdId.Nil);
      var def = myBindLifetime.CreateNested(); //for arbitrary lifetimes
      var task = new WiredRdTask<TReq,TRes>(def, this, taskId, scheduler);
      Wire.Send(RdId, (writer) =>
      {
        if (LogSend.IsTraceEnabled()) 
          LogSend.Trace("call `{0}`::({1}) send{2} request '{3}' : {4}", Location, RdId, sync ? " SYNC":"", taskId, request.PrintToString());
        
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





//  public class RdEndpoint<TReq, TRes> : RdReactiveBase, IRdRpc
//  {
//
//    private Lifetime myBindLifetime;
//
//    public CtxReadDelegate<TReq> ReadRequestDelegate { get; private set; }
//    public CtxWriteDelegate<TReq> WriteRequestDelegate { get; private set; }
//
//    public CtxReadDelegate<TRes> ReadResponseDelegate { get; private set; }
//    public CtxWriteDelegate<TRes> WriteResponseDelegate { get; private set; }
//
//    public Func<Lifetime, TReq, RdTask<TRes>> Handler { get; private set; }
//
//    public RdEndpoint() : this(Polymorphic<TReq>.Read, Polymorphic<TReq>.Write, Polymorphic<TRes>.Read, Polymorphic<TRes>.Write) {}
//    public RdEndpoint(CtxReadDelegate<TReq> readRequest, CtxWriteDelegate<TReq> writeRequest, CtxReadDelegate<TRes> readResponse, CtxWriteDelegate<TRes> writeResponse)
//    {
//      ReadRequestDelegate = readRequest;
//      WriteRequestDelegate = writeRequest;
//      ReadResponseDelegate = readResponse;
//      WriteResponseDelegate = writeResponse;
//    }
//
//    public RdEndpoint(Func<Lifetime, TReq, RdTask<TRes>> handler) : this()
//    {
//      Set(handler);
//    }
//
//    public RdEndpoint(Func<TReq, TRes> handler) : this((lf, req) => RdTask<TRes>.Successful(handler(req)))
//    {}
//
//    public void Set(Func<Lifetime, TReq, RdTask<TRes>> handler)
//    {
//      Assertion.Require(Handler == null, "Handler is set already");
//      Handler = handler;
//    }
//
//    public void Reset(Func<Lifetime, TReq, RdTask<TRes>> handler)
//    {
//      Handler = handler;
//    }
//
//    public void Set(Func<TReq, TRes> handler)
//    {
//      Set((lf, req) => RdTask<TRes>.Successful(handler(req)));
//    }
//
//    protected override void Init(Lifetime lifetime)
//    {
//      base.Init(lifetime);
//      myBindLifetime = lifetime;
//
//      Wire.Advise(lifetime, this);
//    }
//
//    public override void OnWireReceived(UnsafeReader reader)
//    {
//      var taskId = RdId.Read(reader);
//      var value = ReadRequestDelegate(SerializationContext, reader);
//      if (LogReceived.IsTraceEnabled()) LogReceived.Trace("endpoint `{0}`::({1}), taskId={2}, request = {3}", Location, RdId, taskId, value.PrintToString());
//
//
//      RdTask<TRes> rdTask;
//      using (UsingDebugInfo()) //now supports only sync handlers
//      {
//        try
//        {
//          rdTask = Handler(myBindLifetime, value);
//        }
//        catch (Exception e)
//        {
//          rdTask = RdTask<TRes>.Faulted(e);
//        }
//      }
//                
//      rdTask.Result.Advise(myBindLifetime, result =>
//      {
//        if (LogSend.IsTraceEnabled()) LogSend.Trace("endpoint `{0}`::({1}), taskId={2}, response = {3}", Location, RdId, taskId, result.PrintToString());
//
//        RdTaskResult<TRes> validatedResult;
//        try
//        {
//          if (result.Status == RdTaskStatus.Success) AssertNullability(result.Result);
//          validatedResult = result;
//        }
//        catch (Exception e)
//        {
//          LogSend.Error(e);
//          validatedResult = RdTaskResult<TRes>.Faulted(e);
//        }
//
//        Wire.Send(RdId, (writer) =>
//        {
//          taskId.Write(writer);
//          RdTaskResult<TRes>.Write(WriteResponseDelegate, SerializationContext, writer, validatedResult);
//        });
//      });
//    }
//
//
//    public static RdEndpoint<TReq, TRes> Read(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<TReq> readRequest, CtxWriteDelegate<TReq> writeRequest, CtxReadDelegate<TRes> readResponse, CtxWriteDelegate<TRes> writeResponse)
//    {
//      var id = reader.ReadRdId();
//      return new RdEndpoint<TReq, TRes>(readRequest, writeRequest, readResponse, writeResponse).WithId(id);
////      throw new InvalidOperationException("Deserialization of RdEndpoint is not allowed, the only valid option is to create RdEndpoint with constructor.");
//    }
//
//    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdEndpoint<TReq, TRes> value)
//    {
//      RdId.Write(writer, value.RdId);
//    }
//  }

}
