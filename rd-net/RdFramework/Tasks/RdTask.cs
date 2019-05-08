using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
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


  public interface IRdTask
  {
    
  }

  public interface IRdTask<T> : IRdTask
  {
    IReadonlyProperty<RdTaskResult<T>> Result { get; }
  }

  public static class RdTasksEx
  {
    public static bool IsSuceedeed<T>(this IRdTask<T> task)
    {
      return task.Result.HasValue() && task.Result.Value.Status == RdTaskStatus.Success;
    }

    public static bool Wait<T>(this IRdTask<T> task, TimeSpan timeout)
    {
      var evt = new ManualResetEvent(false);
      task.Result.Advise(Lifetime.Eternal, _ => evt.Set());
      return evt.WaitOne((int)timeout.TotalMilliseconds);
    }
#if !NET35
    public static RdTask<T> ToRdTask<T>(this Task<T> task, CancellationToken token, TaskContinuationOptions continuationOptions = TaskContinuationOptions.None, TaskScheduler scheduler = null)
    {
      var rdTask = new RdTask<T>();
      task.ContinueWith((tsk, rdTaskObject) =>
      {
        var rdTask1 = (RdTask<T>) rdTaskObject;
        if (tsk.IsCanceled)
          rdTask1.SetCancelled();
        else if (tsk.IsFaulted)
          rdTask1.Set(tsk.Exception?.Flatten().GetBaseException());
        else
          rdTask1.Set(tsk.Result);
      }, rdTask, token, continuationOptions, scheduler ?? TaskScheduler.Current);
      return rdTask;
    }

    public static void Set<TReq, TRes>(this RdEndpoint<TReq, TRes> endpoint, Func<Lifetime, TReq, Task<TRes>> handler)
    {
      endpoint.Set((lt, req) => handler(lt, req).ToRdTask(lt));
    }
#endif

    public static void SetAndLogErrors<TReq, TRes>(this RdEndpoint<TReq, TRes> endpoint, Func<TReq, TRes> handler, ILog logger)
    {
      endpoint.Set(req =>
      {
        try
        {
          return handler(req);
        }
        catch (Exception ex)
        {
          logger.Error(ex);
          throw;
        }
      });
    }
    
    public static void SetAndLogErrors<TReq, TRes>(this RdEndpoint<TReq, TRes> endpoint, Action<TReq, RdTask<TRes>> handler, ILog logger)
    {
      endpoint.Set((lifetime, req) =>
      {
        try
        {
          var task = new RdTask<TRes>();
          handler(req, task);
          return task;
        }
        catch (Exception ex)
        {
          logger.Error(ex);
          throw;
        }
      });
    }

    public static void SetVoid<TReq>(this RdEndpoint<TReq, Unit> endpoint, Action<TReq> handler)
    {
      endpoint.Set(req =>
      {
        handler(req);
        return Unit.Instance;
      });
    }

    public static void SetVoid<TRes>(this RdEndpoint<Unit, TRes> endpoint, Func<TRes> handler)
    {
      endpoint.Set(req => handler());
    }

    public static void SetVoid(this RdEndpoint<Unit, Unit> endpoint, Action handler)
    {
      endpoint.Set(req =>
      {
        handler();
        return Unit.Instance;
      });
    }
  }


  public enum RdTaskStatus
  {
    Success,
    Canceled,
    Faulted
  }

  public sealed class RdTaskResult<T> : IPrintable
  {
    private readonly RdTaskStatus myStatus;
    private readonly T myResult;
    private readonly RdFault myError;

    internal RdTaskResult(RdTaskStatus status, T result, RdFault error)
    {
      myStatus = status;
      myResult = result;
      myError = error;
    }

    public RdTaskStatus Status { get { return myStatus; } }
    public T Result { get { return myResult; } }
    public RdFault Error { get { return myError; } }


    internal static RdTaskResult<T> Success(T result)
    {
      return new RdTaskResult<T>(RdTaskStatus.Success, result, null);
    }
    internal static RdTaskResult<T> Cancelled()
    {
      return new RdTaskResult<T>(RdTaskStatus.Canceled, default(T), null);
    }
    internal static RdTaskResult<T> Faulted(Exception exception)
    {
      return new RdTaskResult<T>(RdTaskStatus.Faulted, default(T), exception as RdFault ?? new RdFault(exception));
    }

    public T Unwrap()
    {
      switch (Status)
      {
        case RdTaskStatus.Success:   return Result;
        case RdTaskStatus.Canceled:  throw new OperationCanceledException();
        case RdTaskStatus.Faulted:   throw Error;
        default:
          throw new ArgumentOutOfRangeException(Status + "");
      }
    }


    public static RdTaskResult<T> Read(CtxReadDelegate<T> readDelegate, SerializationCtx ctx, UnsafeReader reader)
    {
      var status = (RdTaskStatus)reader.ReadInt();

      switch (status)
      {
        case RdTaskStatus.Success:   return Success(readDelegate(ctx, reader));
        case RdTaskStatus.Canceled:  return Cancelled();
        case RdTaskStatus.Faulted:   return Faulted(RdFault.Read(ctx, reader));
        default:
          throw new ArgumentOutOfRangeException(status + "");
      }
    }

    public static void Write(CtxWriteDelegate<T> writeDelegate, SerializationCtx ctx, UnsafeWriter writer, RdTaskResult<T> value)
    {
      writer.Write((int)value.Status);

      switch (value.Status)
      {
        case RdTaskStatus.Success:
          writeDelegate(ctx, writer, value.Result);
          break;
        case RdTaskStatus.Canceled:
          break;
        case RdTaskStatus.Faulted:
          RdFault.Write(ctx, writer, value.Error);
          break;
        default:
          throw new ArgumentOutOfRangeException();
      }
    }


    public void Print(PrettyPrinter printer)
    {
      printer.Print(myStatus.ToString());
      if (myStatus != RdTaskStatus.Canceled)
      {
        printer.Print(" :: ");
        if (myStatus == RdTaskStatus.Success)
        {
          Result.PrintEx(printer);
        } else if (myStatus == RdTaskStatus.Faulted)
        {
          Error.PrintEx(printer);
        }
      }
    }
  }


  public class RdTask<T> : IRdTask<T>
  {
    internal readonly WriteOnceProperty<RdTaskResult<T>> ResultInternal = new WriteOnceProperty<RdTaskResult<T>>();

    public IReadonlyProperty<RdTaskResult<T>> Result
    {
      get { return ResultInternal; }
    }

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



  public interface IRdRpc
  {
    
  }



  public interface IRdCall<in TReq, TRes> : IRdRpc
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


  public class RdCall<TReq, TRes> : RdReactiveBase, IRdCall<TReq, TRes>
  {
//    private readonly IDictionary<RdId, KeyValuePair<IScheduler, RdTask<TRes>>> myRequests = new ConcurrentDictionary<RdId, KeyValuePair<IScheduler, RdTask<TRes>>>();
    private readonly IDictionary<RdId, KeyValuePair<IScheduler, RdTask<TRes>>> myRequests
#if NET35
      = new JetBrains.Collections.Synchronized.SynchronizedDictionary<RdId, KeyValuePair<IScheduler, RdTask<TRes>>>  ();
      #else
      = new System.Collections.Concurrent.ConcurrentDictionary<RdId, KeyValuePair<IScheduler, RdTask<TRes>>>();
      #endif

    public RdCall() :this(Polymorphic<TReq>.Read, Polymorphic<TReq>.Write, Polymorphic<TRes>.Read, Polymorphic<TRes>.Write) {}

    public RdCall(CtxReadDelegate<TReq> readRequest, CtxWriteDelegate<TReq> writeRequest, CtxReadDelegate<TRes> readResponse, CtxWriteDelegate<TRes> writeResponse)
    {
      ReadRequestDelegate = readRequest;
      WriteRequestDelegate = writeRequest;
      ReadResponseDelegate = readResponse;
      WriteResponseDelegate = writeResponse;
    }

    public CtxReadDelegate<TReq> ReadRequestDelegate { get; private set; }
    public CtxWriteDelegate<TReq> WriteRequestDelegate { get; private set; }

    public CtxReadDelegate<TRes> ReadResponseDelegate { get; private set; }
    public CtxWriteDelegate<TRes> WriteResponseDelegate { get; private set; }

    public new SerializationCtx SerializationContext { get; private set; }
    

    public override IScheduler WireScheduler => SynchronousScheduler.Instance;


    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      SerializationContext = base.SerializationContext; //caching context because of we could listen on 
      

      Wire.Advise(lifetime, this);

      lifetime.OnTermination(() =>
      {
        foreach (var request in myRequests)
        {
          var task = request.Value.Value;

          //todo race condition
          if (!task.ResultInternal.HasValue())
          {
            task.ResultInternal.SetValue(RdTaskResult<TRes>.Cancelled());
          }
        }

        myRequests.Clear();
      });
    }

    public override void OnWireReceived(UnsafeReader stream)
    {
      var taskId = stream.ReadRdId();
      KeyValuePair<IScheduler, RdTask<TRes>> request;

      if (!myRequests.TryGetValue(taskId, out request))
      {
        if (LogReceived.IsTraceEnabled()) LogReceived.Trace("call `{0}` ({1}) received response '{2}' but it was dropped", Location, RdId, taskId);

      }
      else
      {
        var result = RdTaskResult<TRes>.Read(ReadResponseDelegate, SerializationContext, stream);
        if (LogReceived.IsTraceEnabled()) LogReceived.Trace("call `{0}` ({1}) received response '{2}' : {3}", Location, RdId, taskId, result.PrintToString());

          request.Key.Queue(() =>
          {
            var task = request.Value;
            if (task.Result.HasValue())
            {
              if (LogReceived.IsTraceEnabled()) LogReceived.Trace("call `{0}` ({1}) response was dropped, task result is {2} ", Location, RdId, task.Result.Value.Status);
              if (IsBound && DefaultScheduler.IsActive && myRequests.ContainsKey(taskId))
                Log.Root.Error($"Request for task `{taskId}` should be already removed");
            }
            else
            {
              //todo race condition
              
            using (UsingDebugInfo())
              task.ResultInternal.SetValue(result);

            myRequests.Remove(taskId);
          }
        });
      }
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
      var task = new RdTask<TRes>();
      myRequests[taskId] = JetKeyValuePair.Of(scheduler, task);

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
      //throw new InvalidOperationException("Serialization of RdCall ("+value.RdId+") is not allowed. The only valid option is to deserialize RdEndpoint.");
    }
  }





  public class RdEndpoint<TReq, TRes> : RdReactiveBase, IRdRpc
  {

    private Lifetime myBindLifetime;

    public CtxReadDelegate<TReq> ReadRequestDelegate { get; private set; }
    public CtxWriteDelegate<TReq> WriteRequestDelegate { get; private set; }

    public CtxReadDelegate<TRes> ReadResponseDelegate { get; private set; }
    public CtxWriteDelegate<TRes> WriteResponseDelegate { get; private set; }

    public Func<Lifetime, TReq, RdTask<TRes>> Handler { get; private set; }

    public RdEndpoint() : this(Polymorphic<TReq>.Read, Polymorphic<TReq>.Write, Polymorphic<TRes>.Read, Polymorphic<TRes>.Write) {}
    public RdEndpoint(CtxReadDelegate<TReq> readRequest, CtxWriteDelegate<TReq> writeRequest, CtxReadDelegate<TRes> readResponse, CtxWriteDelegate<TRes> writeResponse)
    {
      ReadRequestDelegate = readRequest;
      WriteRequestDelegate = writeRequest;
      ReadResponseDelegate = readResponse;
      WriteResponseDelegate = writeResponse;
    }

    public RdEndpoint(Func<Lifetime, TReq, RdTask<TRes>> handler) : this()
    {
      Set(handler);
    }

    public RdEndpoint(Func<TReq, TRes> handler) : this((lf, req) => RdTask<TRes>.Successful(handler(req)))
    {}

    public void Set(Func<Lifetime, TReq, RdTask<TRes>> handler)
    {
      Assertion.Require(Handler == null, "Handler is set already");
      Handler = handler;
    }

    public void Reset(Func<Lifetime, TReq, RdTask<TRes>> handler)
    {
      Handler = handler;
    }

    public void Set(Func<TReq, TRes> handler)
    {
      Set((lf, req) => RdTask<TRes>.Successful(handler(req)));
    }

    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);
      myBindLifetime = lifetime;

      Wire.Advise(lifetime, this);
    }

    public override void OnWireReceived(UnsafeReader reader)
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

        Wire.Send(RdId, (writer) =>
        {
          taskId.Write(writer);
          RdTaskResult<TRes>.Write(WriteResponseDelegate, SerializationContext, writer, validatedResult);
        });
      });
    }


    public static RdEndpoint<TReq, TRes> Read(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<TReq> readRequest, CtxWriteDelegate<TReq> writeRequest, CtxReadDelegate<TRes> readResponse, CtxWriteDelegate<TRes> writeResponse)
    {
      var id = reader.ReadRdId();
      return new RdEndpoint<TReq, TRes>(readRequest, writeRequest, readResponse, writeResponse).WithId(id);
//      throw new InvalidOperationException("Deserialization of RdEndpoint is not allowed, the only valid option is to create RdEndpoint with constructor.");
    }

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdEndpoint<TReq, TRes> value)
    {
      RdId.Write(writer, value.RdId);
    }
  }

}
