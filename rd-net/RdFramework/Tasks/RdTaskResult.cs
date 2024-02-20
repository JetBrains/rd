using System;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

#nullable disable

namespace JetBrains.Rd.Tasks
{
  //todo Union with Result
  public sealed class RdTaskResult<T> : IPrintable
  {
    public RdTaskStatus Status { get; }
    public T Result { get; }
    public RdFault Error { get; }

    private RdTaskResult(RdTaskStatus status, T result, RdFault error)
    {
      Status = status;
      Result = result;
      Error = error;
    }


    internal static RdTaskResult<T> Success(T result) => new RdTaskResult<T>(RdTaskStatus.Success, result, null);

    internal static RdTaskResult<T> Cancelled() => new RdTaskResult<T>(RdTaskStatus.Canceled, default(T), null);

    internal static RdTaskResult<T> Faulted(Exception exception) => new RdTaskResult<T>(RdTaskStatus.Faulted, default(T), exception as RdFault ?? new RdFault(exception));

    public T Unwrap()
    {
      switch (Status)
      {
        case RdTaskStatus.Success: return Result;
        case RdTaskStatus.Canceled: throw new OperationCanceledException();
        case RdTaskStatus.Faulted: throw Error;
        default:
          throw new ArgumentOutOfRangeException(Status + "");
      }
    }


    public static RdTaskResult<T> Read(CtxReadDelegate<T> readDelegate, SerializationCtx ctx, UnsafeReader reader)
    {
      var status = (RdTaskStatus) reader.ReadInt();

      switch (status)
      {
        case RdTaskStatus.Success: return Success(readDelegate(ctx, reader));
        case RdTaskStatus.Canceled: return Cancelled();
        case RdTaskStatus.Faulted: return Faulted(RdFault.Read(ctx, reader));
        default:
          throw new ArgumentOutOfRangeException($"Unknown status of {nameof(RdTaskStatus)}: {status}");
      }
    }

    public static void Write(CtxWriteDelegate<T> writeDelegate, SerializationCtx ctx, UnsafeWriter writer, RdTaskResult<T> value)
    {
      writer.WriteInt32((int) value.Status);

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
      printer.Print(Status.ToString());
      if (Status != RdTaskStatus.Canceled)
      {
        printer.Print(" :: ");
        if (Status == RdTaskStatus.Success)
        {
          Result.PrintEx(printer);
        }
        else if (Status == RdTaskStatus.Faulted)
        {
          Error.PrintEx(printer);
        }
      }
    }

    public override string ToString() => this.PrintToString();
  }
}