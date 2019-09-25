using System;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

namespace JetBrains.Rd.Tasks
{
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

        public RdTaskStatus Status => myStatus;
        public T Result => myResult;
        public RdFault Error => myError;


        internal static RdTaskResult<T> Success(T result) => new RdTaskResult<T>(RdTaskStatus.Success, result, null);

        internal static RdTaskResult<T> Cancelled() => new RdTaskResult<T>(RdTaskStatus.Canceled, default(T), null);

        internal static RdTaskResult<T> Faulted(Exception exception) => new RdTaskResult<T>(RdTaskStatus.Faulted, default(T), exception as RdFault ?? new RdFault(exception));

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
}