using System;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Threading;
using JetBrains.Util;
using JetBrains.Util.Internal;
using JetBrains.Util.Util;

namespace JetBrains.Lifetimes
{
  public class LifetimeDefinition : IDisposable
  {    
    
    #region Statics

    internal static readonly ILog Log = JetBrains.Diagnostics.Log.GetLog<Lifetime>();
    
    [PublicAPI] internal static readonly LifetimeDefinition Eternal = new LifetimeDefinition { Id = nameof(Eternal) };
    [PublicAPI] internal static readonly LifetimeDefinition Terminated = new LifetimeDefinition { Id = nameof(Terminated) };

    static LifetimeDefinition()
    {
      Terminated.ToCancellationToken(); //to create cts
      Terminated.Terminate();
    }

    //Dictionary is used only for thread-local termination check. Maybe it's not worth it an we should remove map.
    [ThreadStatic] private static Dictionary<LifetimeDefinition, int> ourThreadLocalExecuting;
    [ThreadStatic] private static int ourAllowTerminationUnderExecutionThreadStatic; 
      
    
    public struct AllowTerminationUnderExecutionCookie : IDisposable
    {
      private Thread myDisposeThread;

      internal AllowTerminationUnderExecutionCookie(Thread disposeThread)
      {
        myDisposeThread = disposeThread;
        ourAllowTerminationUnderExecutionThreadStatic++;
      }

      public void Dispose()
      {
        if (myDisposeThread != null)
        {
          if (myDisposeThread != Thread.CurrentThread)
            throw new InvalidOperationException($"{nameof(AllowTerminationUnderExecutionCookie)} must be disposed under thread `{myDisposeThread.ToThreadString()}`, but disposing under {Thread.CurrentThread} ");
          myDisposeThread = null;
          ourAllowTerminationUnderExecutionThreadStatic--;
        }
      }
    }
    
    
    private int ThreadLocalExecuting(int increment = 0)
    {
      var map = ourThreadLocalExecuting;
      if (map == null)
      {
        if (increment == 0)
          return 0; //shortcut 
        
        map = new Dictionary<LifetimeDefinition, int>();
        ourThreadLocalExecuting = map;
      }

      var hasValue  = map.TryGetValue(this, out var old);
      Assertion.Assert(hasValue == (old > 0), "Illegal state, hasValue={0}, _old={1}", hasValue, old);
      
      var _new = old + increment;
      if (_new == 0)
        map.Remove(this);
      else
        map[this] = _new;

      return _new;
    }
    
    private const int WaitForExecutingInTerminationTimeoutMsDefault = 500;
    [PublicAPI] public static int WaitForExecutingInTerminationTimeoutMs = WaitForExecutingInTerminationTimeoutMsDefault;  

    private static readonly BitSlice<int> ourExecutingSlice = BitSlice.Int(20);
    private static readonly BitSlice<LifetimeStatus> ourStatusSlice = BitSlice.Enum<LifetimeStatus>(ourExecutingSlice);
    private static readonly BitSlice<bool> ourMutexSlice = BitSlice.Bool(ourStatusSlice);
    private static readonly BitSlice<bool> ourVerboseDiagnosticsSlice = BitSlice.Bool(ourMutexSlice);
    private static readonly BitSlice<bool> ourAllowTerminationUnderExecutionSlice = BitSlice.Bool(ourVerboseDiagnosticsSlice);
    private static readonly BitSlice<bool> ourLogErrorAfterExecution = BitSlice.Bool(ourAllowTerminationUnderExecutionSlice);
         
    
    #endregion
    
    
        
    #region State

    private const int ResourcesInitialCapacity = 1; 
    
    private int myResCount;
    //in fact we could optimize footprint even better by changing `object[]` to `object` for single object 
    [CanBeNull] private object[] myResources = new object[ResourcesInitialCapacity];
    
    private int myState;

    public Lifetime Lifetime => new Lifetime(this);
    
    
    [PublicAPI] public LifetimeStatus Status => ourStatusSlice[myState];
    [PublicAPI] public bool IsEternal => ReferenceEquals(this, Eternal);

    public bool AllowTerminationUnderExecution
    {
      [PublicAPI] get => ourAllowTerminationUnderExecutionSlice[myState];
      [PublicAPI] set => ourAllowTerminationUnderExecutionSlice.InterlockedUpdate(ref myState, value);
    }
    
    #endregion

    
    
    #region Init
    
    public LifetimeDefinition() {}    
    
    public LifetimeDefinition(Lifetime parent) : this()
    {
      parent.Definition.Attach(this);
    }   
  
    public LifetimeDefinition(Lifetime parent, [CanBeNull, InstantHandle] Action<LifetimeDefinition> atomicAction) : this(parent)
    {
      ExecuteOrTerminateOnFail(atomicAction);
    }
    
    public LifetimeDefinition(Lifetime parent, [CanBeNull, InstantHandle] Action<Lifetime> atomicAction) : this(parent)
    {
      ExecuteOrTerminateOnFail(atomicAction);
    }
    
#if !NET35
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
#endif  
    private void ExecuteOrTerminateOnFail([CanBeNull] Action<LifetimeDefinition> atomicAction)
    {
      try
      {
        using (var cookie = UsingExecuteIfAlive(true))
        {
          if (cookie.Succeed) 
            atomicAction?.Invoke(this);          
        } 
      }
      catch (Exception)
      {
        Terminate();
        throw;
      }
    }
    
#if !NET35
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
#endif  
    private void ExecuteOrTerminateOnFail([CanBeNull] Action<Lifetime> atomicAction)
    {
      try
      {
        using (var cookie = UsingExecuteIfAlive(true))
        {
          if (cookie.Succeed) 
            atomicAction?.Invoke(Lifetime);          
        } 
      }
      catch (Exception)
      {
        Terminate();
        throw;
      }
    }
    
    #endregion

    
    
    #region Diagnostics
    
    [PublicAPI] public static readonly string AnonymousLifetimeId = "Anonymous";
    
    [PublicAPI, CanBeNull] public object Id { get; set; }    
    
    private bool IsVerboseLoggingEnabled => ourVerboseDiagnosticsSlice[myState];
    
    public void EnableTerminationLogging() => ourVerboseDiagnosticsSlice.InterlockedUpdate(ref myState, true);

    public override string ToString() => $"Lifetime `{Id ?? AnonymousLifetimeId}` [{Status}, executing={ourExecutingSlice[myState]}, resources={myResCount}]";
    
    #endregion
        
    


    #region State change helpers

    //Without allocations
    struct UnderMutexCookie : IDisposable
    {
      private readonly LifetimeDefinition myDef;
      internal readonly bool Success;

      public UnderMutexCookie(LifetimeDefinition def, LifetimeStatus statusUpperBound)
      {
        myDef = def;

        while (true)
        {
          var s = myDef.myState;
          if (ourStatusSlice[s] > statusUpperBound)
          {
            Success = false;
            return;
          }
        
          if (ourMutexSlice[s])
            continue;        
          
          if (Interlocked.CompareExchange(ref def.myState, ourMutexSlice.Updated(s, true), s) == s)
            break;
        }

        Success = true;
      }
      
      
      public void Dispose()
      {
        if (!Success)
          return;
        
        while (true)
        {          
          var s = myDef.myState;
          Assertion.Assert(ourMutexSlice[s], "{0}: Mutex must be owned", myDef);
    
          if (Interlocked.CompareExchange(ref myDef.myState, ourMutexSlice.Updated(s, false), s) == s)
            break;
        }
      }
    }
    


    private bool IncrementStatusIfEqualsTo(LifetimeStatus status)
    {
      Assertion.Assert(!IsEternal, "Trying to change eternal lifetime");

      while (true)
      {
        var s = myState;
        if (ourStatusSlice[s] != status)
          return false;

        var updated = ourStatusSlice.Updated(s, ourStatusSlice[s] + 1);

        if (Interlocked.CompareExchange(ref myState, updated, s) == s)
          return true;
      }
    }
    
    #endregion
     
    
    
    
    #region Termination

    public void Dispose() => Terminate();

    [Obsolete("Use `Lifetime.IsAlive` or `Status` field instead")]
    public bool IsTerminated => Status >= LifetimeStatus.Terminating;
      
    
    
    private void Diagnostics(string msg)
    {
      if (IsVerboseLoggingEnabled)
        Log.Verbose($"{msg} {this}");
      else if (Log.IsTraceEnabled())
        Log.Trace($"{msg} {this}");
    }
    
    
    [PublicAPI]
    public void Terminate()
    {
      if (IsEternal || Status > LifetimeStatus.Canceling)
        return ;

      Diagnostics(nameof(LifetimeStatus.Canceling));

      var supportsTerminationUnderExecuting = AllowTerminationUnderExecution || ourAllowTerminationUnderExecutionThreadStatic > 0;
      if (!supportsTerminationUnderExecuting && ThreadLocalExecuting() > 0)
        throw new InvalidOperationException($"{this}: can't terminate under `ExecuteIfAlive` because termination doesn't support this. Use `{nameof(AllowTerminationUnderExecution)}` or `{nameof(Lifetime.UsingAllowTerminationUnderExecution)}`.");
      
      
      //parent could ask for canceled already
      MarkCanceledRecursively();      
      
      if (ourExecutingSlice[myState] > 0 /*optimization*/ && !SpinWait.SpinUntil(() => ourExecutingSlice[myState] <= ThreadLocalExecuting(), WaitForExecutingInTerminationTimeoutMs))
      {
        Log.Error($"{this}: can't wait for `ExecuteIfAlive` completed on other thread in {WaitForExecutingInTerminationTimeoutMs} ms. Keep termination. You'll find stacktrace of ExecuteIfAlive in following error messages.");
        ourLogErrorAfterExecution.InterlockedUpdate(ref myState, true);
      }

      
      if (!IncrementStatusIfEqualsTo(LifetimeStatus.Canceling))
        return;
      
      Diagnostics(nameof(LifetimeStatus.Terminating));
      //Now status is 'Terminating' and we have to wait for all resource modifications to complete. No mutex acquire is possible beyond this point.
      if (ourMutexSlice[myState]) //optimization
        SpinWaitEx.SpinUntil(() => !ourMutexSlice[myState]);
      
      Destruct();      
      Assertion.Assert(Status == LifetimeStatus.Terminated, "{0}: bad status for termination finish", this);
      Diagnostics(nameof(LifetimeStatus.Terminated));
    }
    
    
    private void MarkCanceledRecursively()
    {
      Assertion.Assert(!IsEternal, "Trying to terminate eternal lifetime");

      if (!IncrementStatusIfEqualsTo(LifetimeStatus.Alive))
        return;
      
      myCts?.Cancel();
      
      // Some other thread can already begin destructuring
      // Then children lifetimes become canceled in their termination 
      
      // In fact here access to resources could be done without mutex because setting cancellation status of children is rather optimization than necessity
      var resources = myResources;
      if (resources == null) return;
      
      //Math.min is to ensure that even if some other thread increased myResCount, we don't get IndexOutOfBoundsException
      for (var i = Math.Min(myResCount, resources.Length) - 1; i >= 0; i--)  
      {
        (resources[i] as LifetimeDefinition)?.MarkCanceledRecursively();
      }
    }

#if !NET35
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
#endif  
    private void Destruct()
    {
      var status = Status;
      Assertion.Assert(status == LifetimeStatus.Terminating, "{0}: bad status for destructuring start", this);
      Assertion.Assert(ourMutexSlice[myState] == false, "{0}: mutex must be released in this point", this);
      //no one can take mutex after this point

      var resources = myResources;
      Assertion.AssertNotNull(resources, "{0}: `resources` can't be null on destructuring stage", this);
      
      for (var i = myResCount - 1; i >= 0; i--)
      {
        try
        {
          switch (resources[i])
          {
            case Action a:
              a();
              break;

            case LifetimeDefinition ld:
              ld.Terminate();
              break;

            case IDisposable d:
              d.Dispose();
              break;
            
            case ITerminationHandler th:
              th.OnTermination(Lifetime);
              break;

            default:
              Log.Error("{0}: unknown type of termination resource: {1}", this, resources[i]);
              break;
          }
        }
        catch (Exception e)
        {
          Log.Error(e, $"{this}: exception on termination of resource[{i}]: ${resources[i]}");
        }
      }

      myResources = null;
      myResCount = 0;
      
      //In fact we shouldn't make cts null, because it should provide stable CancellationToken to finish enclosing tasks in Canceled state (not Faulted)
      //But to avoid memory leaks we must do it. So if you 1) run task with alive lifetime 2) terminate lifetime 3) in task invoke ThrowIfNotAlive() you can obtain `Faulted` state rather than `Canceled`. But it doesn't matter in `async-await` programming.     
      if (!ReferenceEquals(this, Terminated))
        myCts = null;
      
      var statusIncrementedSuccessfully = IncrementStatusIfEqualsTo(LifetimeStatus.Terminating);
      Assertion.Assert(statusIncrementedSuccessfully, "{0}: bad status for destructuring finish", this);
    }

    

    
    #endregion

    
    
    
    #region Add termination actions

    internal bool TryAdd([NotNull] object action)
    {
      CheckNotNull(action);
      
      //will never be terminated; need to be revised for debugging
      if (IsEternal)
        return true;

      using (var mutex = new UnderMutexCookie(this, LifetimeStatus.Canceling))
      {
        if (!mutex.Success)
          return false;

        var resources = myResources;
        Assertion.AssertNotNull(resources, "{0}: `resources` can't be null under mutex while status < Terminating", this);
        
        if (myResCount == resources.Length)
        {
          var countAfterCleaning = 0;
          for (var i = 0; i < myResCount; i++)
          {
            //can't clear Canceling because TryAdd works in Canceling state 
            if (resources[i] is LifetimeDefinition ld && ld.Status >= LifetimeStatus.Terminating)
              resources[i] = null;
            else
              resources[countAfterCleaning++] = resources[i];
          }

          myResCount = countAfterCleaning;
          if (countAfterCleaning * 2 > resources.Length)
            Array.Resize(ref myResources, countAfterCleaning * 2); //must be more than 1, so it always should be room for one more resource
        }

        // ReSharper disable once PossibleNullReferenceException
        myResources[myResCount++] = action;
        return true;
      }
    }


    
    internal Task Attached(Task task)
    {
      if (IsEternal || task.IsCompleted) 
        return task;

      var cookie = UsingExecuteIfAlive(false, true);
      if (!cookie.Succeed) return task;

      return task.ContinueWith(t =>
      {
        cookie.Dispose();
        return t;
      }, TaskContinuationOptions.ExecuteSynchronously).Unwrap();
      
    }
    
    internal Task<T> Attached<T>(Task<T> task)
    {
      if (IsEternal || task.IsCompleted) 
        return task;

      var cookie = UsingExecuteIfAlive(false, true);
      if (!cookie.Succeed) return task;

      return task.ContinueWith(t =>
      {
        cookie.Dispose();
        return t;
      }, TaskContinuationOptions.ExecuteSynchronously).Unwrap();
      
    }

    
    internal void Attach([NotNull] LifetimeDefinition child)
    {
      if (child == null) throw new ArgumentNullException(nameof(child));
      Assertion.Require(!child.IsEternal, "{0}: can't attach eternal lifetime", this);

      if (child.Status >= LifetimeStatus.Canceling) //should not normally happen
        return;
      
      if (!TryAdd(child))
        child.Terminate();
    }
      

    
    internal void OnTermination([NotNull] object action)
    {
      if (TryAdd(action)) return;
      
      //Trying to add action to terminated lifetime. We should throw an exception but also need to terminate this resource right now.
      try
      {
        switch (action)
        {
          case Action a:
            a();
            break;
            
          case IDisposable d:
            d.Dispose();
            break;
          
          case ITerminationHandler th:
            th.OnTermination(Lifetime);
            break;
            
          default:
            Assertion.Fail($"{this}: Unknown resource for synchronous termination: {action}");
            break;
        }
      }
      catch (Exception e)
      {
        Log.Error(e, $"{this}: exception on synchronous execute of action on terminated lifetime: {action}");
      }
        
      //Doesn't return "proper" OCE that transfer task into Canceled state (opposed to Faulted) because we null cts in `Destruct`. But it's for memory sake.
      throw new InvalidOperationException($"{this}: can't add termination action if lifetime terminating or terminated (Status > Canceled); you can consider usage of `TryOnTermination` ");
    }
    
    #endregion
        
    
    
    
    #region Execute

    public int ExecutingCount => ourExecutingSlice[myState]; 
    
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private void CheckNotNull([NotNull] object action)
    {
      if (action == null) throw new ArgumentNullException(nameof(action));      
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private static Result<Unit> WrapOrThrow([NotNull] Action action, bool wrap)
    {
      if (wrap)
        return Result.Wrap(action);

      action();
      return Result.Unit;
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private static Result<T> WrapOrThrow<T>([NotNull] Func<T> action, bool wrap)
    {
      return wrap ? Result.Wrap(action) : Result.Success(action());
    }
    
    
    public struct ExecuteIfAliveCookie : IDisposable
    {
      [NotNull] 
      private readonly LifetimeDefinition myDef;

      private readonly bool myAllowTerminationUnderExecuting;
      private readonly bool myDisableIncrementThreadLocalExecuting;
      public readonly bool Succeed;

      internal ExecuteIfAliveCookie([NotNull] LifetimeDefinition def, bool allowTerminationUnderExecuting, bool disableIncrementThreadLocalExecuting)
      {
        
        myDef = def;
        myAllowTerminationUnderExecuting = allowTerminationUnderExecuting;
        myDisableIncrementThreadLocalExecuting = disableIncrementThreadLocalExecuting;

        while (true)
        {
          var s = myDef.myState;
          if (ourStatusSlice[s] != LifetimeStatus.Alive)
          {
            Succeed = false;
            return;
          }
        
          if (Interlocked.CompareExchange(ref myDef.myState, s+1, s) == s)
            break;
        }

        Succeed = true;
        
        if (!myDisableIncrementThreadLocalExecuting)
          myDef.ThreadLocalExecuting(+1);
        
        if (myAllowTerminationUnderExecuting)
          ourAllowTerminationUnderExecutionThreadStatic++;
      }

      public void Dispose()
      {
        if (!Succeed)
          return;
        
        Interlocked.Decrement(ref myDef.myState);
        
        if (!myDisableIncrementThreadLocalExecuting)
          myDef.ThreadLocalExecuting(-1);
        
        if (myAllowTerminationUnderExecuting)
          ourAllowTerminationUnderExecutionThreadStatic--;

        if (ourLogErrorAfterExecution[myDef.myState])
        {
          Log.Error($"ExecuteIfAlive after termination of {myDef} took too much time (>{WaitForExecutingInTerminationTimeoutMs}ms)");
        } 
      }
    }

        
    internal ExecuteIfAliveCookie UsingExecuteIfAlive(bool allowTerminationUnderExecution = false, bool disableIncrementThreadLocalExecuting = false)
    {
      return new ExecuteIfAliveCookie(this, allowTerminationUnderExecution, disableIncrementThreadLocalExecuting);
    }
               
    
    
    internal Result<T> TryExecute<T>([NotNull] Func<T> action, bool wrapExceptions = false)
    {
      CheckNotNull(action);
      
      using (var cookie = UsingExecuteIfAlive())
      {
        return cookie.Succeed ? WrapOrThrow(action, wrapExceptions) : CanceledResult();
      }
    }

    internal Result<Unit> TryExecute([NotNull] Action action, bool wrapExceptions = false)
    {
      CheckNotNull(action);
      
      using (var cookie = UsingExecuteIfAlive())
      {
        return cookie.Succeed ? WrapOrThrow(action, wrapExceptions) : CanceledResult();
      }
    }

    #if !NET35
    internal Task TryExecuteAsync([NotNull] Func<Task> closure, bool wrapExceptions = false) => Attached(TryExecute(closure, wrapExceptions).UnwrapTask());
    internal Task<T> TryExecuteAsync<T>([NotNull] Func<Task<T>> closure, bool wrapExceptions = false) => Attached(TryExecute(closure, wrapExceptions).UnwrapTask());
    #endif
    
    internal T Execute<T>([NotNull] Func<T> action)
    {      
      CheckNotNull(action);
      
      using (var cookie = UsingExecuteIfAlive())
      {
        return cookie.Succeed ? action() : throw CanceledException();
      }
    }

    internal void Execute([NotNull] Action action)
    {       
      CheckNotNull(action);
      
      using (var cookie = UsingExecuteIfAlive())
      {
        if (cookie.Succeed)
          action();
        else
          throw CanceledException();
      }
    }

    internal Task ExecuteAsync([NotNull] Func<Task> closure) => Attached(Execute(closure));
    internal Task<T> ExecuteAsync<T>([NotNull] Func<Task<T>> closure) => Attached(Execute(closure));
    
    #endregion




    #region Bracket

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private void CheckNotNull([NotNull] object opening, [NotNull] object closing)
    {
      if (opening == null) throw new ArgumentNullException(nameof(opening));      
      if (closing == null) throw new ArgumentNullException(nameof(closing));      
    } 
        
    internal Result<Unit> TryBracket([NotNull] Action opening, [NotNull] Action closing, bool wrapExceptions = false)
    {
      CheckNotNull(opening, closing);
      
      using (var cookie = UsingExecuteIfAlive())
      {
        if (!cookie.Succeed) return CanceledResult();

        var result = WrapOrThrow(opening, wrapExceptions);
        return result.Succeed && !TryAdd(closing) ? WrapOrThrow(closing, wrapExceptions) : result;
      }
    }

    internal Result<T> TryBracket<T>([NotNull] Func<T> opening, [NotNull] Action closing, bool wrapExceptions = false)
    {
      CheckNotNull(opening, closing);
      
      using (var cookie = UsingExecuteIfAlive())
      {
        if (!cookie.Succeed) return CanceledResult();
        
        var result = WrapOrThrow(opening, wrapExceptions);
        return result.Succeed && !TryAdd(closing) ? WrapOrThrow(closing, wrapExceptions).Map(result.Value) : result;
      }
    }
    internal Result<T> TryBracket<T>([NotNull] Func<T> opening, [NotNull] Action<T> closing, bool wrapExceptions = false)
    {
      CheckNotNull(opening, closing);
      
      using (var cookie = UsingExecuteIfAlive())
      {
        if (!cookie.Succeed) return CanceledResult();
        
        var result = WrapOrThrow(opening, wrapExceptions);
        var closingAction = new Action(() => closing(result.Value));
        return result.Succeed && !TryAdd(closingAction) ? WrapOrThrow(closingAction, wrapExceptions).Map(result.Value) : result;
      }
    }


    internal void Bracket([NotNull] Action opening, [NotNull] Action closing)
    {
      CheckNotNull(opening, closing);
      
      using (var cookie = UsingExecuteIfAlive())
      {
        if (!cookie.Succeed)
          throw CanceledException();
        
        opening();
        
        if (!TryAdd(closing))
          closing();
                
      }            
    }
    
    internal T Bracket<T>([NotNull] Func<T> opening, [NotNull] Action closing)
    {
      CheckNotNull(opening, closing);
      
      using (var cookie = UsingExecuteIfAlive())
      {
        if (!cookie.Succeed)
          throw CanceledException();
        
        var res = opening();
        
        if (!TryAdd(closing))
          closing();

        return res;
      }            
    }
    
    internal T Bracket<T>([NotNull] Func<T> opening, [NotNull] Action<T> closing)
    {      
      CheckNotNull(opening, closing);
      
      using (var cookie = UsingExecuteIfAlive())
      {
        if (!cookie.Succeed)
          throw CanceledException();
        
        var res = opening();
        
        if (!TryAdd(new Action (() => {closing(res);})))
        {
          closing(res);
        }

        return res;
      }            
    }
    
    #endregion

    
    
    
    #region Cancellation    
    
    private CancellationTokenSource myCts;
    
        
    //Only if state >= Canceled
    private LifetimeCanceledException CanceledException() => new LifetimeCanceledException(Lifetime);
    
    //Only if state >= Canceled
    private Result<Nothing> CanceledResult() => Result.Canceled(CanceledException());
    

    private void CreateCtsLazily()
    {
      if (myCts != null) return;
      
      var cts = new CancellationTokenSource();
      Memory.Barrier();
      //to suppress reordering of init and ctor visible from outside
      myCts = cts;
      
      //But MarkCanceledRecursively may already happen, so we need to help Cancel source
      if (Status != LifetimeStatus.Alive)
        myCts.Cancel();            
    }
    
    
    public void ThrowIfNotAlive()
    {
      if (Status != LifetimeStatus.Alive)
        throw CanceledException();
    }
    
    
    internal CancellationToken ToCancellationToken(bool doNotCreateCts = false)
    {
      if (myCts == null)
      {
        if (doNotCreateCts)
          return Terminated.ToCancellationToken();
        
        using (var mutex = new UnderMutexCookie(this, LifetimeStatus.Alive))
        {
          if (!mutex.Success)
          {
            Assertion.Assert(!ReferenceEquals(this, Terminated), "Mustn't reach this point on lifetime `Terminated`");
            return Terminated.ToCancellationToken(); //to get stable CancellationTokenSource (for tasks to finish in Canceled state, rather than Faulted)
          }
          
          CreateCtsLazily();
        }
      }
      
      return myCts.Token;
    }
    
    

    #endregion


    #region Finalization

    [PublicAPI] public void AssertEverTerminated(string comment = null)
    {
      OnTermination(new FinalizableGuard(this, comment));
    }

    private class FinalizableGuard : IDisposable
    {
      private readonly LifetimeDefinition myDef;
      private readonly string myComment;

      internal FinalizableGuard([NotNull] LifetimeDefinition def, [CanBeNull] string comment)
      {
        myDef = def;
        myComment = comment;
      }

      ~FinalizableGuard()
      {
        Log.Error("{0} has never been terminated. Some resources might have leaked. {1}", myDef, myComment ?? "");
      }
      
      public void Dispose()
      {
        GC.SuppressFinalize(this);
      }
    }
    

    #endregion
    
    
    
    #region Task API

    [PublicAPI] public void SynchronizeWith<T>([NotNull] TaskCompletionSource<T> taskCompletionSource)
    {
      if (taskCompletionSource == null) throw new ArgumentNullException(nameof(taskCompletionSource));

      var task = taskCompletionSource.Task;
      using (var cookie = UsingExecuteIfAlive(true))
      {
        if (!cookie.Succeed)
          taskCompletionSource.TrySetCanceled();
        else
          if (task.IsCompleted)
            Terminate();
          else
          {
            //lifetime is guaranteed alive and task is probably alive (but race could happen and task already completed).
            Lifetime.OnTermination(() => taskCompletionSource.TrySetCanceled(/*ToCancellationToken()*/));
            task.ContinueWith(_ => Terminate(), TaskContinuationOptions.ExecuteSynchronously);
          }
      }
    }
    #endregion
  }
}