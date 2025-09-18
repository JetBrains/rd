using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Lifetimes;

using System.Threading.Tasks;
using JetBrains.Diagnostics;
using JetBrains.Threading;
using System.Runtime.ExceptionServices;
#nullable disable

namespace JetBrains.Core
{

  
  /// <summary>
  /// Helper methods for <see cref="Result{T}" /> and <see cref="Result{T,T}"/> building
  /// </summary>
  [PublicAPI]
  public static class Result
  {
    /// <summary>
    /// Message that is being applied to Result.Fail when no message provided 
    /// </summary>
    public const string EmptyFailMessage = "<<empty fail message>>";
    
    
    /// <summary>
    /// Creates successful <see cref="Result{T}"/> with value <see cref="value"/>
    /// </summary>
    /// <param name="value"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns>Result with <see cref="Result{T}.Succeed"/> == true</returns>
    public static Result<T> Success<T>(T value)
    {
      return new Result<T>(value, null);
    }


    /// <summary>
    /// Creates failed <see cref="Result{T}"/>
    /// </summary>
    /// <param name="exception"></param>
    /// <param name="captureStackTrace">Try to capture exception stack (if any), could be unwind by <see cref="Result{T}.Unwrap"/> </param>
    /// <returns>Result with <see cref="Result{T}.Succeed"/> == false. Returned type could be implicitly casted to any <see cref="Result{T}"/></returns>
    /// <exception cref="ArgumentNullException">if <see cref="exception"/> is null</exception>
    public static Result<Nothing> Fail([NotNull] Exception exception, bool captureStackTrace = false)
    {
      if (exception == null) throw new ArgumentNullException(nameof(exception));           
      
      return new Result<Nothing>(null, captureStackTrace ? ExceptionDispatchInfo.Capture(exception) : (object) exception);
    }

    /// <summary>
    /// Creates failed <see cref="Result{T, T}"/> with corresponding <see cref="failValue"/>
    /// </summary>
    /// <param name="exception"></param>
    /// <param name="failValue">Special user-defined value provided for failed Result</param>
    /// <param name="captureStackTrace">Try to capture exception stack (if any), could be unwind by <see cref="Result{T}.Unwrap"/> </param>
    /// <returns>Result with <see cref="Result{TSuccess,TFailure}.Succeed"/> == false. Returned type could be implicitly casted to any <see cref="Result{T, T}"/></returns>
    /// <exception cref="ArgumentNullException">if <see cref="exception"/> is null</exception>
    public static Result<Nothing, TFailure> Fail<TFailure>([NotNull] Exception exception, TFailure failValue, bool captureStackTrace = false)
    {
      if (exception == null) throw new ArgumentNullException(nameof(exception));           
      
      return new Result<Nothing, TFailure>(null, captureStackTrace ? ExceptionDispatchInfo.Capture(exception) : (object) exception, failValue);
    }

    /// <summary>
    /// Creates failed <see cref="Result{T}"/> with <see cref="ResultException"/> that wraps provided <see cref="message"/>
    /// </summary>
    /// <param name="message">Reason of failure. If not defined, <see cref="EmptyFailMessage"/> is used.</param>
    /// <returns>Result with <see cref="Result{T}.Succeed"/> == false. Returned type could be implicitly casted to any <see cref="Result{T}"/></returns>
    public static Result<Nothing> Fail(string message = null)
    {
      return Fail(new ResultException(message ?? EmptyFailMessage));
    }

    /// <summary>
    /// Creates failed <see cref="Result{T, T}"/> with <see cref="ResultException"/> that wraps provided <see cref="message"/>
    /// </summary>
    /// <param name="message">Reason of failure. If not defined, <see cref="EmptyFailMessage"/> is used.</param>
    /// <param name="failValue">Special user-defined value provided for failed Result</param>
    /// <returns>Result with <see cref="Result{TSuccess,TFailure}.Succeed"/> == false. Returned type could be implicitly casted to any <see cref="Result{T}"/></returns>
    public static Result<Nothing, TFailure> Fail<TFailure>(string message, TFailure failValue)
    {
      return Fail(new ResultException(message ?? EmptyFailMessage), failValue);
    }

    /// <summary>
    /// Creates failed <see cref="Result{T}"/> with message= <see cref="EmptyFailMessage"/> and user-defined failure parameter 
    /// </summary>
    /// <param name="failValue">Special user-defined value provided for failed Result</param>
    /// <returns>Result with <see cref="Result{TSuccess,TFailure}.Succeed"/> == false. Returned type could be implicitly casted to any <see cref="Result{T}"/></returns>

    public static Result<Nothing, TFailure> FailWithValue<TFailure>(TFailure failValue)
    {
      return Fail((string) null, failValue);
    }

    
    /// <summary>
    /// Creates special failed <see cref="Result{T}"/> that wraps <see cref="OperationCanceledException"/>
    /// </summary>
    /// <returns>Result with <see cref="Result{T}.Succeed"/> == false and <see cref="Result{T}.Canceled"/> == true. Returned type could be implicitly casted to any <see cref="Result{T}"/></returns>
    public static Result<Nothing> Canceled()
    {
      return Canceled(new OperationCanceledException(
        Lifetime.Terminated
        ));
    }

    /// <summary>
    /// Creates special failed <see cref="Result{T}"/> that wraps <see cref="OperationCanceledException"/>
    /// </summary>
    /// <param name="exception">Captured OCE that lead to this cancellation</param>
    /// <param name="captureStackTrace">Try to capture exception stack (if any), could be unwind by <see cref="Result{T}.Unwrap"/>. </param>
    /// <returns>Result with <see cref="Result{T}.Succeed"/> == false and <see cref="Result{T}.Canceled"/> == true. Returned type could be implicitly casted to any <see cref="Result{T}"/></returns>
    /// <exception cref="ArgumentNullException">if <see cref="exception"/> is null</exception>
    public static Result<Nothing> Canceled([NotNull] OperationCanceledException exception, bool captureStackTrace = false)
    {
      return Fail(exception, captureStackTrace);
    }
    
    /// <summary>
    /// Void succeed result for <see cref="Unit"/> type
    /// </summary>
    public static Result<Unit> Unit = Success(Core.Unit.Instance);
    

    /// <summary>
    /// Wrap execution of <see cref="f"/>() into <see cref="Result{TRes}"/>. 
    /// </summary>
    /// <param name="f">Function  to execute</param>
    /// <typeparam name="TRes">type argument of returned Result</typeparam>
    /// <returns>Succeed result with <see cref="Result{T}.Value"/> == f() if no exception happened during <see cref="f"/> execution. Failed result with corresponding exception otherwise </returns>
    [HandleProcessCorruptedStateExceptions]
    public static Result<TRes> Wrap<TRes>([NotNull] Func<TRes> f)
    {
      try
      {
        return Success(f());
      }
      catch (Exception e)
      {
        return Fail(e, true);
      }
    }
    
    
    /// <summary>
    /// Wrap execution of <see cref="f"/>() into <see cref="Result{Unit}"/>. 
    /// </summary>
    /// <param name="f">Action to execute</param>
    /// <returns>Succeed result with <see cref="Result.Unit"/> if no exception happened during <see cref="f"/> execution. Failed result with corresponding exception otherwise </returns>
    [HandleProcessCorruptedStateExceptions]
    public static Result<Unit> Wrap([NotNull] Action f)
    {
      try
      {
        f();
        return Unit;
      }
      catch (Exception e)
      {
        return Fail(e, true);
      }
    }
    
    
    /// <summary>
    /// Wrap execution of <see cref="f"/>(<see cref="param"/>) into <see cref="Result{Unit}"/>. 
    /// </summary>
    /// <param name="f">Action with parameter to execute</param>
    /// <param name="param">function argument</param>
    /// <typeparam name="T"><see cref="param"/> type</typeparam>
    /// <returns>Succeed result with <see cref="Result.Unit"/> if no exception happened during <see cref="f"/> execution. Failed result with corresponding exception otherwise </returns>    
      [HandleProcessCorruptedStateExceptions]
    public static Result<Unit> Wrap<T>([NotNull] Action<T> f, T param)
    {
      try
      {
        f(param);
        return Unit;
      }
      catch (Exception e)
      {
        return Fail(e, true);
      }
    }
    
    /// <summary>
    /// Wrap execution of <see cref="f"/>(<see cref="param"/>) into <see cref="Result{TRes}"/>. 
    /// </summary>
    /// <param name="f">Function with parameter to execute</param>
    /// <param name="param">function argument</param>
    /// <typeparam name="T"><see cref="param"/> type</typeparam>
    /// <typeparam name="TRes">type argument of returned Result</typeparam>
    /// <returns>Succeed result with <see cref="Result{T}.Value"/> == f(param) if no exception happened during <see cref="f"/> execution. Failed result with corresponding exception otherwise </returns>    
    [HandleProcessCorruptedStateExceptions]
    public static Result<TRes> Wrap<T, TRes>([NotNull] Func<T, TRes> f, T param)
    {
      try
      {
        return Success(f(param));
      }
      catch (Exception e)
      {
        return Fail(e, true);
      }
    }
    
    
    /// <summary>
    /// Transforms this <see cref="Result"/> into <see cref="Task"/>.
    /// <see cref="Result{T}.Succeed"/> corresponds to <see cref="Task"/> in <see cref="Result{Task}.Value"/>.
    /// <see cref="Result{Task}.Canceled"/> corresponds to completed task with <see cref="Task.IsCanceled"/>
    /// <see cref="Result{Task}.FailedNotCanceled"/> corresponds to completed task with <see cref="Task.IsFaulted"/> and <see cref="Exception"/>
    /// </summary>
    /// <param name="result">this</param>
    /// <returns><see cref="Task"/> that corresponds <see cref="result"/></returns>
    public static Task UnwrapTask(this Result<Task> result)
    {
      if (result.Succeed) 
        return result.Value;
      
      if (!result.Canceled) 
        return Task.FromException(result.Exception);
      
      if (result.Exception is OperationCanceledException oce)
        return Task.FromCanceled(oce.CancellationToken);
      else
        return Task.FromCanceled(Lifetime.Terminated);
    }

    /// <summary>
    /// Transforms this <see cref="Result"/> into <see cref="Task"/>.
    /// <see cref="Result{T}.Succeed"/> corresponds to <see cref="Task"/> in <see cref="Result{Task}.Value"/>.
    /// <see cref="Result{Task}.Canceled"/> corresponds to completed task with <see cref="Task.IsCanceled"/>
    /// <see cref="Result{Task}.FailedNotCanceled"/> corresponds to completed task with <see cref="Task.IsFaulted"/> and <see cref="Exception"/>
    /// </summary>
    /// <param name="result">this</param>
    /// <typeparam name="T">type parameter of returning task</typeparam>
    /// <returns><see cref="Task"/> that corresponds <see cref="result"/></returns>
    public static Task<T> UnwrapTask<T>(this Result<Task<T>> result)
    {
      if (result.Succeed) 
        return result.Value;
      
      if (!result.Canceled) 
        return Task.FromException<T>(result.Exception);
      
      if (result.Exception is OperationCanceledException oce)
        return Task.FromCanceled<T>(oce.CancellationToken);
      else
        return Task.FromCanceled<T>(Lifetime.Terminated);
    }


    /// <summary>
    /// Wrap completed task's result into <see cref="Result{T}"/> or throw <see cref="InvalidOperationException"/> is task is <c>!</c><see cref="Task.IsCompleted"/> 
    /// </summary>
    /// <param name="task">Must be finished (<see cref="Task.IsCompleted"/><c>==true</c>) or <see cref="InvalidOperationException"/> will be throws</param>
    /// <typeparam name="T"></typeparam>
    /// <returns><see cref="Success{T}"/>(task.<see cref="Task{T}.Result"/>) or <see cref="Fail(System.Exception, bool)"/>(task.<see cref="Task.Exception"/>)</returns>
    /// <exception cref="InvalidOperationException">in case of <c>!task.</c><see cref="Task.IsCompleted"/></exception>
    public static Result<T> FromCompletedTask<T>(Task<T> task)
    {
      if (!task.IsCompleted)
        throw new InvalidOperationException($"Task must be completed to convert into result but was in state: {task.Status}");

      return task.Status == TaskStatus.RanToCompletion ? 
        Success(task.Result) 
        : Fail(task.Exception.NotNull($"Exception must always exist for task with status: {task.Status}"));
    }
  }
  
  
  
  
  
  /// <summary>
  /// Monad that can can have two states: <see cref="Succeed"/> and Fail (!<see cref="Succeed"/>). Also we distinct special type of Fail: <see cref="Canceled"/>.  
  /// </summary>
  /// <typeparam name="T"></typeparam>
  [PublicAPI]
  public readonly struct Result<T> : IEquatable<Result<T>>
  {
    /// <summary>
    /// Value in case of <see cref="Succeed"/>, default(T) otherwise
    /// </summary>
    public readonly T Value;
    
    /// <summary>
    /// It this field not null, this Result is !<see cref="Succeed"/> and vise versa.
    /// </summary>
    internal readonly object ExceptionOrExceptionDispatchInfo; 

    /// <summary>
    /// Exception in case of (!<see cref="Succeed"/>), null otherwise
    /// </summary>
    public Exception Exception => ExceptionOrExceptionDispatchInfo is Exception ex
      ? ex
      : (ExceptionOrExceptionDispatchInfo as ExceptionDispatchInfo)?.SourceException; 

    
    /// <summary>
    /// Exception message in case of (!<see cref="Succeed"/>), null otherwise 
    /// </summary>
    public string FailMessage => Exception?.Message;    
   
    /// <summary>
    /// Shouldn't be invoked in user's code
    /// </summary>
    /// <param name="success"></param>
    /// <param name="failure"></param>
    internal Result(T success, object failure)
    {
      Value = success;
      ExceptionOrExceptionDispatchInfo = failure;
    }

    /// <summary>
    /// Is result successful
    /// </summary>
    public bool Succeed => ExceptionOrExceptionDispatchInfo == null;
    
    /// <summary>
    /// (!<see cref="Succeed"/>) and (<see cref="Canceled"/>
    /// </summary>
    public bool FailedNotCanceled => !Succeed && !Canceled;
    
    /// <summary>
    /// Exception has specials type of <see cref="OperationCanceledException"/> or <see cref="AggregateException"/> that has <see cref="OperationCanceledException"/> inside.
    /// </summary>
    public bool Canceled => Exception.IsOperationCanceled();

        
    public static implicit operator Result<T>(Result<Nothing> me)
    {
      return new Result<T>(default(T), me.ExceptionOrExceptionDispatchInfo);
    }
    
    /// <summary>
    /// Transform this result into new one with given function. if !<see cref="Succeed"/>, stays untouched./> 
    /// </summary>
    /// <param name="transform"></param>
    /// <typeparam name="TRes"></typeparam>
    /// <returns></returns>
    public Result<TRes> Map<TRes>(Func<T, TRes> transform)
    {
      return Succeed ? Result.Wrap(transform, Value) : Result.Fail(Exception);
    }
    
    /// <summary>
    /// Map without lambda. Success{Anything} -> Success{<see cref="successValue"/>}. Fail -> Fail
    /// </summary>
    /// <param name="successValue">In case of success we always create successful result with this value</param>
    /// <typeparam name="TRes"></typeparam>
    /// <returns></returns>
    public Result<TRes> Map<TRes>(TRes successValue)
    {
      return Succeed ? Result.Success(successValue) : Result.Fail(Exception);
    }

    /// <summary>
    /// Returns <see cref="Value"/> if <see cref="Succeed"/>, throws <see cref="Exception"/> otherwise
    /// </summary>
    /// <returns> <see cref="Value"/> if <see cref="Succeed"/> </returns>
    /// <exception cref="Exception">if !<see cref="Succeed"/></exception>
    public T Unwrap()
    {
      if (Succeed)
        return Value;
      
      switch (ExceptionOrExceptionDispatchInfo)
      {
        case Exception ex:
          throw ex;
        case ExceptionDispatchInfo edi:
          edi.Throw();
          return Nothing.Unreachable<T>();
        default:
          return Nothing.Unreachable<T>();
      }
    }


    /// <summary>
    /// Transforms this <see cref="Result"/> into <see cref="Task"/> in <see cref="Task.IsCompleted"/> state state.
    /// <see cref="Succeed"/> corresponds to <see cref="Task.IsRanToCompletion"/>.
    /// <see cref="Canceled"/> corresponds to <see cref="Task.IsCanceled"/>
    /// <see cref="FailedNotCanceled"/> corresponds to <see cref="Task.IsFaulted"/> with <see cref="Exception"/>
    /// </summary>
    /// <returns><see cref="Task"/> in <see cref="Task.IsCompleted"/> state</returns>
    public Task<T> AsCompletedTask()
    {
      if (Succeed) 
        return Task.FromResult(Value);
      
      if (!Canceled) return Task.FromException<T>(Exception);
      
      if (Exception is OperationCanceledException oce)
        return Task.FromCanceled<T>(oce.CancellationToken);
      else
        return Task.FromCanceled<T>(Lifetime.Terminated);
    }
    
    public override string ToString()
    {
      var status = Succeed ? "Success(" + Value +")":
        Canceled ? "Canceled" 
        : "Fail(" + FailMessage+")";

      return "Result." + status;
    }

    public bool Equals(Result<T> other)
    {
      return EqualityComparer<T>.Default.Equals(Value, other.Value) && Equals(Exception, other.Exception);
    }

    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      return obj is Result<T> other && Equals(other);
    }

    public override int GetHashCode()
    {
      unchecked
      {
        return (EqualityComparer<T>.Default.GetHashCode(Value) * 397) ^ (Exception != null ? Exception.GetHashCode() : 0);
      }
    }

    public static bool operator ==(Result<T> left, Result<T> right)
    {
      return left.Equals(right);
    }

    public static bool operator !=(Result<T> left, Result<T> right)
    {
      return !left.Equals(right);
    }
  }
  
  /// <summary>
  /// Special kind of <see cref="Result{T}"/> to store custom <see cref="FailValue"/> in case of !<see cref="Succeed"/>. 
  /// </summary>
  /// <typeparam name="TSuccess"></typeparam>
  /// <typeparam name="TFailure"></typeparam>
  [PublicAPI]
  public readonly struct Result<TSuccess, TFailure> : IEquatable<Result<TSuccess, TFailure>>
  {
    /// <summary>
    /// Value in case of <see cref="Succeed"/>, default(T) otherwise
    /// </summary>
    public readonly TSuccess Value;
    
    /// <summary>
    /// It this field not null, this Result is !<see cref="Succeed"/> and vise versa.
    /// </summary>
    internal readonly object ExceptionOrExceptionDispatchInfo; 

    /// <summary>
    /// Exception in case of (!<see cref="Succeed"/>), null otherwise
    /// </summary>
    public Exception Exception => ExceptionOrExceptionDispatchInfo is Exception ex
      ? ex
      : (ExceptionOrExceptionDispatchInfo as ExceptionDispatchInfo)?.SourceException;  

    public string FailMessage => Exception?.Message; 
    
    public readonly TFailure FailValue;
   
    internal Result(TSuccess success, object failure, TFailure failValue)
    {
      Value = success;
      ExceptionOrExceptionDispatchInfo = failure;
      FailValue = failValue;
    }

    public bool Succeed => ExceptionOrExceptionDispatchInfo == null;
    public bool FailedNotCanceled => !Succeed && !Canceled;
   
    public bool Canceled => 
      Exception is OperationCanceledException 
      ||((Exception as AggregateException)?.Flatten().InnerExceptions.Any(ex => ex is OperationCanceledException) ?? false)
    ;

       
    public static implicit operator Result<TSuccess, TFailure>(Result<Nothing, TFailure> me)
    {
      return new Result<TSuccess, TFailure>(default(TSuccess), me.ExceptionOrExceptionDispatchInfo, me.FailValue);
    }
    
    public static implicit operator Result<TSuccess>(Result<TSuccess, TFailure> me)
    {
      return new Result<TSuccess>(me.Value, me.ExceptionOrExceptionDispatchInfo);
    }
    
    public static implicit operator Result<TSuccess, TFailure>(Result<TSuccess> me)
    {
      return new Result<TSuccess, TFailure>(me.Value, me.ExceptionOrExceptionDispatchInfo, default(TFailure));
    }

    public TSuccess Unwrap()
    {
      if (Succeed)
        return Value;
      
      switch (ExceptionOrExceptionDispatchInfo)
      {
        case Exception ex:
          throw ex;
        case ExceptionDispatchInfo edi:
          edi.Throw();
          return Nothing.Unreachable<TSuccess>();
        default:
          return Nothing.Unreachable<TSuccess>();
      }
    }
    
    public override string ToString()
    {
      var status = Succeed ? "Success(" + Value +")":
        Canceled ? "Canceled" 
        : $"Fail({FailMessage}, {FailValue})";

      return "Result." + status;
    }


    public bool Equals(Result<TSuccess, TFailure> other)
    {
      return EqualityComparer<TSuccess>.Default.Equals(Value, other.Value) && Equals(Exception, other.Exception) && EqualityComparer<TFailure>.Default.Equals(FailValue, other.FailValue);
    }

    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      return obj is Result<TSuccess, TFailure> other && Equals(other);
    }

    public override int GetHashCode()
    {
      unchecked
      {
        var hashCode = EqualityComparer<TSuccess>.Default.GetHashCode(Value);
        hashCode = (hashCode * 397) ^ (Exception != null ? Exception.GetHashCode() : 0);
        hashCode = (hashCode * 397) ^ EqualityComparer<TFailure>.Default.GetHashCode(FailValue);
        return hashCode;
      }
    }

    public static bool operator ==(Result<TSuccess, TFailure> left, Result<TSuccess, TFailure> right)
    {
      return left.Equals(right);
    }

    public static bool operator !=(Result<TSuccess, TFailure> left, Result<TSuccess, TFailure> right)
    {
      return !left.Equals(right);
    }
  }

  /// <summary>
  /// Exception arising in <see cref="Result"/> when do not specify exception explicitly: <see cref="Result.Fail(string)"/> 
  /// </summary>
  public class ResultException : Exception
  {
    public ResultException(string message) : base(message) { }
  }
 
}