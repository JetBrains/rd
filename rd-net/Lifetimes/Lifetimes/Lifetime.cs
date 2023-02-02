using System;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Threading;

#if !NET35

#endif

namespace JetBrains.Lifetimes
{
  /// <summary>
  /// Lifetime's lifecycle statuses. Lifetime is created in <see cref="Alive"/> status and eventually becomes <see cref="Terminated"/>.
  /// Status change is one way road: from lower ordinal to higher (<c>Alive -> Canceling -> Terminating -> Terminated </c>).
  /// </summary>
  public enum LifetimeStatus
  {
    /// <summary>
    /// Lifetime is ready to use. Every lifetime's method will work. 
    /// </summary>
    Alive,
    
    /// <summary>
    /// This status propagates instantly through all lifetime's children graph when <see cref="LifetimeDefinition.Terminate"/> is invoked.
    /// Lifetime is in consistent state (no resources are terminated) but termination process is already began. All background activities that block
    /// termination (e.g. started with <see cref="Lifetime.Execute{T}"/>, <see cref="Lifetime.ExecuteAsync"/>) should be interrupted
    /// as fast as possible. That's why background activities must check <see cref="Lifetime.IsAlive"/> or <see cref="Lifetime.ThrowIfNotAlive"/>
    /// quite ofter (200 ms is a good reference value).
    ///
    /// Some methods in this status still works, e.g. <see cref="Lifetime.OnTermination(System.Action)"/> others do nothing (<see cref="Lifetime.TryExecute{T}"/>)
    /// or throw <see cref="LifetimeCanceledException"/> (<see cref="Lifetime.Execute{T}"/>, <see cref="Lifetime.Bracket"/>)
    ///
    /// Associated <see cref="Lifetime.ToCancellationToken"/> is canceled.
    /// </summary>
    Canceling,
    
    /// <summary>
    /// Lifetime is in inconsistent state. Destruction begins: some resources are terminated, other not. All method throw exception or do nothing
    /// (e.g. <see cref="Lifetime.TryOnTermination(System.Action)"/>). 
    /// </summary>
    Terminating,
    
    /// <summary>
    /// Lifetime is fully terminated, all resources are disposed and method's behavior is the same as in <see cref="Terminating"/> state.
    /// </summary>
    Terminated
  }

  /// <summary>
  /// Lifetime's termination timeout kind. The actual milliseconds value can be assigned via <see cref="LifetimeDefinition.SetTerminationTimeoutMs"/>. 
  /// </summary>
  public enum LifetimeTerminationTimeoutKind
  {
    /// <summary>
    /// Default timeout (500ms).<br/>
    /// The actual value defined by <see cref="LifetimeDefinition.WaitForExecutingInTerminationTimeoutMs"/> (compatibility mode).
    /// </summary>
    Default,
    
    /// <summary>
    /// Short timeout (250ms).<br/>
    /// The actual value can be overridden via <see cref="LifetimeDefinition.SetTerminationTimeoutMs"/>.
    /// </summary>
    Short,
    
    /// <summary>
    /// Long timeout (5s).<br/>
    /// The actual value can be overriden via <see cref="LifetimeDefinition.SetTerminationTimeoutMs"/>.
    /// </summary>
    Long,
    
    /// <summary>
    /// Extra long timeout (30s).<br/>
    /// The actual value can be overriden via <see cref="LifetimeDefinition.SetTerminationTimeoutMs"/>.
    /// </summary>
    ExtraLong
  }
  
  
  /// <summary>
  /// Central class in <see cref="JetBrains.Lifetimes"/> package. Has two main functions:<br/>
  /// 1. High performance analogue of <see cref="CancellationToken"/>. <see cref="LifetimeDefinition"/> is analogue of <see cref="CancellationTokenSource"/> <br/>
  /// 2. Inversion of <see cref="IDisposable"/> pattern (with thread-safety):
  /// user can add termination resources into Lifetime with bunch of <c>OnTermination</c> (e.g. <see cref="OnTermination(Action)"/>) methods.
  /// When lifetime is being terminated (i.e. it's <see cref="LifetimeDefinition"/> was called <see cref="LifetimeDefinition.Terminate"/>) all
  /// previously added termination resources are being terminated in stack-way LIFO order. Lifetimes forms a hierarchy with parent-child relations so in single-threaded world child always
  /// becomes <see cref="LifetimeStatus.Terminated"/> <b>BEFORE</b> parent. Usually this hierarchy is a tree but in some cases (like <see cref="Intersect(JetBrains.Lifetimes.Lifetime[])"/> it can be
  /// a directed acyclic graph. 
  ///
  /// <para>
  /// Kinds of termination resources:
  /// <list type="number">
  /// <item><see cref="Action"/> - merely invoked on termination</item>
  /// <item><see cref="IDisposable"/> - <see cref="IDisposable.Dispose"/> is called on termination</item>
  /// <item><see cref="ITerminationHandler"/> - <see cref="ITerminationHandler.OnTermination"/> is called on termination</item>
  /// <item><see cref="LifetimeDefinition"/> - for nested(child) lifetimes created by <see cref="LifetimeDefinition(Lifetime)"/>.
  /// Child lifetime definition's <see cref="LifetimeDefinition.Terminate"/> method is called.</item>
  /// </list>
  ///
  /// If some resource throws an exception during termination, it will be logged by <see cref="ILog"/> with level <see cref="LoggingLevel.ERROR"/> so termination of other resources
  /// won't be affected.
  ///
  /// 
  /// 
  /// <c>Lifetime</c> could be converted to <see cref="CancellationToken"/> by implicit cast or by explicit <see cref="ToCancellationToken"/> to use in task based API. You can start
  /// </para>
  /// </summary>
  public readonly struct Lifetime : IEquatable<Lifetime>
  {        
    
    private readonly LifetimeDefinition? myDefinition;   
    internal LifetimeDefinition Definition => myDefinition ?? LifetimeDefinition.Eternal;   
    
    //ctor
    internal Lifetime(LifetimeDefinition definition)
    {
      myDefinition = definition;
    }


    #if !NET35
    /// <summary>
    /// Special "async-static" lifetime that is captured when you execute <see cref="ExecuteAsync"/> or <see cref="Start(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// and can be used by cooperative cancellation.
    /// </summary>
    public static readonly AsyncLocal<Lifetime> AsyncLocal = new AsyncLocal<Lifetime>();
    #endif

    /// <summary>
    /// <para>A lifetime that never ends. Scheduling actions on such a lifetime has no effect.</para>
    /// <para>Do not call <see cref="Lifetime.AddRef"/> on such a lifetime, because it will not hold your object forever.</para>  
    /// </summary>
    [PublicAPI] public static Lifetime Eternal      => LifetimeDefinition.Eternal.Lifetime;
    
    /// <summary>
    /// Singleton lifetime that is in <see cref="LifetimeStatus.Terminated"/> state by default.  
    /// </summary>
    [PublicAPI] public static Lifetime Terminated   => LifetimeDefinition.Terminated.Lifetime;

    /// <summary>
    /// Lifecycle phase of current lifetime. 
    /// </summary>
    [PublicAPI] public LifetimeStatus Status        => Definition.Status;
    
    /// <summary>
    /// Whether current lifetime is equal to <see cref="Eternal"/> and never be terminated
    /// </summary>
    [PublicAPI] public bool IsEternal               => Definition.IsEternal;
    
    /// <summary>
    /// Is <see cref="Status"/> of this lifetime equal to <see cref="LifetimeStatus.Alive"/>
    /// </summary>
    [PublicAPI] public bool IsAlive                 => Definition.Status == LifetimeStatus.Alive;
    
    /// <summary>
    /// Is <see cref="Status"/> of this lifetime not equal to <see cref="LifetimeStatus.Alive"/>: Termination started already (or even finished).
    /// </summary>
    [PublicAPI] public bool IsNotAlive              => !IsAlive;

    /// <summary>
    /// Gets termination timeout kind for the lifetime.<br/>
    /// The sub-definitions inherit this value at the moment of creation.
    /// </summary>
    [PublicAPI] public LifetimeTerminationTimeoutKind TerminationTimeoutKind => Definition.TerminationTimeoutKind;



    #region OnTermination
    
    //OnTerminations that fail in case of bad status
    
    /// <summary>
    /// Add termination resource with <c>kind == Action</c> that will be invoked when lifetime termination start
    /// (i.e. <see cref="LifetimeDefinition.Terminate"/> is called, <see cref="ExecutingCount"/> became zero, so status is set to <see cref="LifetimeStatus.Terminating"/>)..
    /// Resources invocation order: LIFO
    /// All errors are logger by <see cref="ILog"/>  so termination of each resource is isolated.
    ///
    /// Method throws <see cref="InvalidOperationException"/> if <see cref="Status"/> &ge; <see cref="LifetimeStatus.Terminating"/>.
    /// The reason is that lifetime is in inconsistent partly terminated or fully terminated state.
    /// </summary>
    /// <param name="action">Action to invoke on termination</param>
    /// <exception cref="InvalidOperationException">if lifetime already started destructuring, i.e. <see cref="Status"/> &ge; <see cref="LifetimeStatus.Terminating"/>  </exception>
    /// <returns>this lifetime</returns>
    [PublicAPI] public Lifetime OnTermination(Action action) { Definition.OnTermination(action); return this; }
    
    
    /// <summary>
    /// Add termination resource <c>kind == IDisposable</c> that will be invoked by calling <see cref="IDisposable.Dispose"/> when lifetime termination start
    /// (i.e. <see cref="LifetimeDefinition.Terminate"/> is called, <see cref="ExecutingCount"/> became zero, so status is set to <see cref="LifetimeStatus.Terminating"/>)..
    /// Resources invocation order: LIFO
    /// All errors are logged by <see cref="ILog"/> so termination of each resource is isolated.
    ///
    /// Method throws <see cref="InvalidOperationException"/> if <see cref="Status"/> &ge; <see cref="LifetimeStatus.Terminating"/>.
    /// The reason is that lifetime is in inconsistent partly terminated state.
    /// </summary>
    /// <param name="disposable">Disposable whose <see cref="IDisposable.Dispose"/> method is invoked on termination</param>
    /// <returns>this lifetime</returns>
    [PublicAPI] public Lifetime OnTermination(IDisposable disposable) { Definition.OnTermination(disposable); return this; }
    
    
    /// <summary>
    /// Add termination resource <c>kind == ITerminationHandler</c> that will be invoked by calling <see cref="ITerminationHandler.OnTermination"/> when lifetime termination start
    /// (i.e. <see cref="LifetimeDefinition.Terminate"/> is called, <see cref="ExecutingCount"/> became zero, so status is set to <see cref="LifetimeStatus.Terminating"/>).
    /// Resources invocation order: LIFO
    /// All errors are logged by <see cref="ILog"/> so termination of each resource is isolated.
    ///
    /// Method throws <see cref="InvalidOperationException"/> if <see cref="Status"/> &ge; <see cref="LifetimeStatus.Terminating"/>.
    /// The reason is that lifetime is in inconsistent partly terminated state.
    /// </summary>
    /// <param name="terminationHandler">termination resources whose <see cref="ITerminationHandler.OnTermination"/> method is invoked on termination</param>
    /// <returns>this lifetime</returns>
    [PublicAPI] public Lifetime OnTermination(ITerminationHandler terminationHandler) { Definition.OnTermination(terminationHandler); return this; }
    
    
    //TryOnTermination that do nothing and returns false in case of bad status
    
    
    /// <summary>
    /// Add termination resource with <c>kind == Action</c> that will be invoked when lifetime termination starts 
    /// (i.e. <see cref="LifetimeDefinition.Terminate"/> is called, <see cref="ExecutingCount"/> became zero, so status is set to <see cref="LifetimeStatus.Terminating"/>).
    /// Resources invocation order: LIFO
    /// All errors are logger by <see cref="ILog"/>  so termination of each resource is isolated.
    ///
    /// Method returns do nothing and return `false` if <see cref="Status"/> &ge; <see cref="LifetimeStatus.Terminating"/>. 
    /// </summary>
    /// <param name="action">Action to invoke on termination</param>
    /// <returns><c>true</c> if resource added - only status &le; <see cref="LifetimeStatus.Canceling"/>. <c>false</c> if resource's not added - status &ge; <see cref="LifetimeStatus.Terminating"/> </returns>
    [PublicAPI]          public bool TryOnTermination(Action action)      => Definition.TryAdd(action);
    
    /// <summary>
    /// Add termination resource with <c>kind == Dispose</c> that will be invoked when lifetime termination starts 
    /// (i.e. <see cref="LifetimeDefinition.Terminate"/> is called, <see cref="ExecutingCount"/> became zero, so status is set to <see cref="LifetimeStatus.Terminating"/>).
    /// Resources invocation order: LIFO
    /// All errors are logger by <see cref="ILog"/>  so termination of each resource is isolated.
    ///
    /// Method returns do nothing and return `false` if <see cref="Status"/> &ge; <see cref="LifetimeStatus.Terminating"/>. 
    /// </summary>
    /// <param name="disposable">Disposable to invoke on termination</param>
    /// <returns><c>true</c> if resource added - only status &le; <see cref="LifetimeStatus.Canceling"/>. <c>false</c> if resource's not added - status &ge; <see cref="LifetimeStatus.Terminating"/> </returns>
    [PublicAPI]          public bool TryOnTermination(IDisposable disposable) => Definition.TryAdd(disposable);
    
    
    /// <summary>
    /// Add termination resource with <c>kind == ITerminationHandler</c> that will be invoked when lifetime termination starts 
    /// (i.e. <see cref="LifetimeDefinition.Terminate"/> is called, <see cref="ExecutingCount"/> became zero, so status is set to <see cref="LifetimeStatus.Terminating"/>).
    /// Resources invocation order: LIFO
    /// All errors are logger by <see cref="ILog"/>  so termination of each resource is isolated.
    ///
    /// Method returns do nothing and return `false` if <see cref="Status"/> &ge; <see cref="LifetimeStatus.Terminating"/>. 
    /// </summary>
    /// <param name="disposable">Action to invoke on termination</param>
    /// <returns><c>true</c> if resource added - only status &le; <see cref="LifetimeStatus.Canceling"/>. <c>false</c> if resource's not added - status &ge; <see cref="LifetimeStatus.Terminating"/> </returns>

    [PublicAPI]          public bool TryOnTermination(ITerminationHandler disposable) => Definition.TryAdd(disposable); 

    #endregion

    
    #region Execute If Alive
    
    /// <summary>
    /// Number of background activities started by <see cref="Execute{T}"/>, <see cref="ExecuteAsync"/> or <see cref="StartAttached(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// When lifetime became <see cref="LifetimeStatus.Canceling"/> (it happens right after user ask for this lifetime's or ancestor lifetime's definition <see cref="LifetimeDefinition.Terminate"/>) this
    /// number could only reduce, no new activities can be started.
    /// When it reach zero, lifetime begins to terminate its resources by changing <see cref="Status"/> to <see cref="LifetimeStatus.Terminating"/>  
    /// </summary>
    [PublicAPI] public int ExecutingCount          => Definition.ExecutingCount; 
    

    /// <summary>
    /// If you terminate lifetime definition <see cref="LifetimeDefinition.Terminate"/> under <see cref="Execute{T}"/> you'll get exception because 
    /// it's impossible to keep guarantee that code under <see cref="Execute{T}"/> works with non-terminating lifetime.
    /// However if you know what you are doing (e.g. <c>Terminate</c> is the last call in code under <c>Execute</c>), you can take this cookie and allow this behavior.
    /// </summary>
    /// <returns>Disposable that allow to call <see cref="LifetimeDefinition.Terminate"/> under <see cref="Execute{T}"/> </returns>
    [PublicAPI] public LifetimeDefinition.AllowTerminationUnderExecutionCookie UsingAllowTerminationUnderExecution() => new LifetimeDefinition.AllowTerminationUnderExecutionCookie(Thread.CurrentThread);
    
    /// <summary>
    /// This method could be used as non-allocation version of <see cref="Execute{T}"/> or <see cref="TryExecute{T}"/>.
    /// Typical usage pattern is the following:
    /// <code>
    ///   using (val cookie = lifetime.ExecuteIfAliveCookie()) {
    ///     if (cookie.Succeed) {
    ///        // you can rely lifetime's resources are alive here (the same as body of Lifetime.Execute, Lifetime.TryExecute).
    ///     } else {
    ///       //  lifetime is not alive, return (as TryExecute) or throw OCE (as Execute)
    ///     }
    ///   }
    /// </code>
    /// </summary>
    /// <param name="allowTerminationUnderExecution">Automatically takes <see cref="UsingAllowTerminationUnderExecution"/> under cookie</param>
    /// <returns>Special disposable object. If <see cref="LifetimeDefinition.ExecuteIfAliveCookie.Succeed"/> than until <c>Dispose</c> current lifetime can't
    /// become <see cref="LifetimeStatus.Terminating"/> and can't start resources destruction. If <c>!Succeed</c> than lifetime is already
    /// <see cref="LifetimeStatus.Terminating"/> or <see cref="LifetimeStatus.Terminated"/> </returns>
    [PublicAPI] public LifetimeDefinition.ExecuteIfAliveCookie UsingExecuteIfAlive(bool allowTerminationUnderExecution = false) => Definition.UsingExecuteIfAlive(allowTerminationUnderExecution);
    
    /// <summary>
    /// Executes <paramref name="action"/> in atomic manner, preventing lifetime to terminate:
    /// <list type="number">
    /// <item>If <see cref="IsAlive"/> is false then <paramref name="action"/> isn't executed and method returns <see cref="Result.Canceled()"/></item>
    /// <item>If <see cref="IsAlive"/> is true then <paramref name="action"/> starts to execute and lifetime can't
    /// become <see cref="LifetimeStatus.Terminating"/> until this execution finished. If someone terminates lifetime during action invocation,
    /// it immediately becomes <see cref="LifetimeStatus.Canceling"/> (that forbids all following <see cref="Execute"/> to start), wait until
    /// <paramref name="action"/> finishes its execution and only then status is set to <see cref="LifetimeStatus.Terminating"/> i.e. resource termination begins. </item>
    /// </list>
    /// 
    ///   
    /// </summary>
    /// <remarks>
    /// <paramref name="action"/> execution can last long and lifetime termination can hang because of it. If you want responsiveness you must check lifetime from time to time inside execution body
    /// (e.g. by <see cref="ThrowIfNotAlive"/> method).
    /// </remarks>
    /// <param name="action">action to execute if this lifetime <see cref="IsAlive"/></param>
    /// <param name="wrapExceptions">if some exception (even <see cref="OperationCanceledException"/>) is thrown during execution should we wrap it
    /// into <see cref="Result{T}"/> or throw (default)</param>
    /// <typeparam name="T"></typeparam>
    /// <returns><see cref="Result"/> that encapsulates execution: successful, canceled ot fail</returns>
    [PublicAPI] public Result<T> TryExecute<T>([InstantHandle] Func<T> action, bool wrapExceptions = false) => Definition.TryExecute(action, wrapExceptions);
    
    /// <summary>
    /// See <see cref="TryExecute{T}"/>
    /// </summary>
    /// <param name="action"></param>
    /// <param name="wrapExceptions"></param>
    /// <returns></returns>
    [PublicAPI] public Result<Unit> TryExecute([InstantHandle] Action action, bool wrapExceptions = false) => Definition.TryExecute(action, wrapExceptions);
    
    /// <summary>
    /// Same as <see cref="TryExecute{T}"/> but any if lifetime <see cref="IsNotAlive"/> than throws <see cref="LifetimeCanceledException"/>
    /// </summary>
    /// <param name="action"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public T Execute<T>([InstantHandle] Func<T> action) => Definition.Execute(action);
    
    /// <summary>
    /// Same as <see cref="TryExecute{T}"/> but any if lifetime <see cref="IsNotAlive"/> than throws <see cref="LifetimeCanceledException"/> 
    /// </summary>
    /// <param name="action"></param>
    [PublicAPI] public void Execute([InstantHandle] Action action) => Definition.Execute(action);
    
    #endregion
    
    
    
    
    #region Bracket
    
    /// <summary>
    /// Semantic equivalent of <see cref="TryExecute{T}"/>(<paramref name="opening"/>) and <see cref="OnTermination(System.Action)"/>(<paramref name="closing"/>)
    /// in atomic manner. Both actions will be executed only if <see cref="IsAlive"/>: <paramref name="opening"/> immediately and <paramref name="closing"/> on termination. 
    ///
    /// If exception happens in <paramref name="opening"/> it will be thrown (or wrapped into <see cref="Result{T}"/> as in <see cref="TryExecute{T}"/>), but <paramref name="closing"/>
    /// will be executed on termination anyway.
    /// </summary>
    /// <param name="opening"></param>
    /// <param name="closing"></param>
    /// <param name="wrapExceptions"></param>
    /// <returns></returns>
    [PublicAPI] public Result<Unit> TryBracket([InstantHandle] Action opening, Action closing, bool wrapExceptions = false) => Definition.TryBracket(opening, closing, wrapExceptions);
    
    /// <summary>
    /// <inheritdoc cref="TryBracket(Action, Action, bool)"/>
    /// </summary>
    /// <param name="opening"></param>
    /// <param name="closing"></param>
    /// <param name="wrapExceptions"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Result<T> TryBracket<T>([InstantHandle] Func<T> opening, Action closing, bool wrapExceptions = false) => Definition.TryBracket(opening, closing, wrapExceptions);
    
    /// <summary>
    /// <inheritdoc cref="TryBracket(Action, Action, bool)"/>
    /// </summary>
    [PublicAPI] public Result<T> TryBracket<T>([InstantHandle] Func<T> opening, Action<T> closing, bool wrapExceptions = false) => Definition.TryBracket(opening, closing, wrapExceptions);
    
    
    
    /// <summary>
    /// Semantic equivalent of <see cref="Execute{T}"/>(<paramref name="opening"/>) and <see cref="OnTermination(System.Action)"/>(<paramref name="closing"/>)
    /// in atomic manner. Both actions will be executed only if <see cref="IsAlive"/>: <paramref name="opening"/> immediately and <paramref name="closing"/> on termination.
    /// Otherwise (see <see cref="IsNotAlive"/>) <see cref="LifetimeCanceledException"/> will be thrown. 
    ///
    /// If exception happens in <paramref name="opening"/> it will be thrown but <paramref name="closing"/>
    /// will be executed on termination anyway.
    /// </summary>
    [PublicAPI] public void Bracket([InstantHandle] Action opening, Action closing) => Definition.Bracket(opening, closing);
    
    /// <summary>
    /// <inheritdoc cref="Bracket(Action, Action)"/>
    /// </summary>
    [PublicAPI] public T Bracket<T>([InstantHandle] Func<T> opening, Action closing) => Definition.Bracket(opening, closing);
    
    /// <summary>
    /// <inheritdoc cref="Bracket(Action, Action)"/>
    /// </summary>
    [PublicAPI] public T Bracket<T>([InstantHandle] Func<T> opening, Action<T> closing) => Definition.Bracket(opening, closing);
    
    
    #endregion
    
    
    
    #region Cancellation

    /// <summary>
    /// Transforms this lifetime into cancellation token
    /// </summary>
    /// <returns></returns>
    [PublicAPI] public CancellationToken ToCancellationToken() => Definition.ToCancellationToken();
    [PublicAPI] public static implicit operator CancellationToken(Lifetime lifetime) => lifetime.Definition.ToCancellationToken();
    
    /// <summary>
    /// Throws <see cref="LifetimeCanceledException"/> is this lifetimes is <see cref="IsNotAlive"/>
    /// </summary>
    [PublicAPI] public void ThrowIfNotAlive() => Definition.ThrowIfNotAlive();

    #endregion
    
    
    #region Using      
    
    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    [PublicAPI]
    public static void Using([InstantHandle] Action<Lifetime> action)
    {
      if (action == null) throw new ArgumentNullException(nameof(action));
      
      using (var def = new LifetimeDefinition())
        action(def.Lifetime);
    }
    
    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    [PublicAPI]
    public static T Using<T>([InstantHandle] Func<Lifetime, T> action)
    {
      if (action == null) throw new ArgumentNullException(nameof(action));
      
      using (var def = new LifetimeDefinition())
        return action(def.Lifetime);
    }

    
    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    ///   <para>The newly-created lifetime will be nested within the parent  lifetime and thus terminated automatically when the parent lifetime ends (unless the nested lifetime is terminated first).</para>
    ///   <para>Nested lifetimes are listed within the parent lifetime, but as they're terminated, the records are removed. There will be no memory leak on the parent lifetime if the nested lifetimes are terminated.</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    [PublicAPI]
    public void UsingNested([InstantHandle] Action<Lifetime> action)
    {
      if (action == null) throw new ArgumentNullException(nameof(action));
      
      using (var def = CreateNested())
        action(def.Lifetime);
    }
    
    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    ///   <para>The newly-created lifetime will be nested within the parent  lifetime and thus terminated automatically when the parent lifetime ends (unless the nested lifetime is terminated first).</para>
    ///   <para>Nested lifetimes are listed within the parent lifetime, but as they're terminated, the records are removed. There will be no memory leak on the parent lifetime if the nested lifetimes are terminated.</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    [PublicAPI]
    public T UsingNested<T>([InstantHandle] Func<Lifetime, T> action)
    {
      if (action == null) throw new ArgumentNullException(nameof(action));
      
      using (var def = CreateNested())
        return action(def.Lifetime);
    }
    
    
    
    //#if !NET35

    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    [PublicAPI] public static async Task UsingAsync([InstantHandle] Func<Lifetime, Task> action)
    {
      if(action == null)
        throw new ArgumentNullException(nameof(action));

      using(var def = new LifetimeDefinition())
        await action(def.Lifetime);
    }

    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    [PublicAPI] public static async Task<T> UsingAsync<T>([InstantHandle] Func<Lifetime, Task<T>> action)
    {
      if(action == null)
        throw new ArgumentNullException(nameof(action));

      using(var def = new LifetimeDefinition())
        return await action(def.Lifetime);
    }

    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    /// </summary>
    /// <param name="parent">A parent lifetime which limits the lifetime given to your action, and might terminate it before the action ends.</param>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    [PublicAPI] public static async Task UsingAsync(OuterLifetime parent, [InstantHandle] Func<Lifetime, Task> action)
    {
      if(action == null)
        throw new ArgumentNullException(nameof(action));

      using var def = new LifetimeDefinition(parent.Def.Lifetime);
      await action(def.Lifetime);
    }

    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    /// </summary>
    /// <param name="parent">A parent lifetime which limits the lifetime given to your action, and might terminate it before the action ends.</param>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    [PublicAPI] public static async Task<T> UsingAsync<T>(OuterLifetime parent, [InstantHandle] Func<Lifetime, Task<T>> action)
    {
      if(action == null)
        throw new ArgumentNullException(nameof(action));

      using var def = new LifetimeDefinition(parent.Def.Lifetime);
      return await action(def.Lifetime);
    }
    
    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    ///   <para>The parent lifetime which might cause premature termination of our lifetime (and, supposedly, the chain of tasks executed under the lifetime, if started correctly).</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    [PublicAPI] public async Task<TRetVal> UsingNestedAsync<TRetVal>([InstantHandle] Func<Lifetime, Task<TRetVal>> action)
    {
      if(action == null) throw new ArgumentNullException(nameof(action));

      using (var def = CreateNested())
        return await action(def.Lifetime);
    }

    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    ///   <para>The parent lifetime which might cause premature termination of our lifetime (and, supposedly, the chain of tasks executed under the lifetime, if started correctly).</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    public async Task UsingNestedAsync([InstantHandle] Func<Lifetime, Task> action)
    {
      if(action == null) throw new ArgumentNullException(nameof(action));

      using (var def = CreateNested())
        await action(def.Lifetime);
    }

   // #endif
    
    #endregion
    
    
    #region Candidates for extension methods
    
    /// <summary>
    /// Same as <see cref="LifetimeDefinition(Lifetime)"/>
    /// </summary>
    /// <returns></returns>
    [PublicAPI]
    public LifetimeDefinition CreateNested() => new LifetimeDefinition(this);
        
    /// <summary>
    /// Same as <see cref="LifetimeDefinition(Lifetime, Action{LifetimeDefinition})"/>
    /// </summary>
    /// <returns></returns>
    [PublicAPI]
    public LifetimeDefinition CreateNested([InstantHandle] Action<LifetimeDefinition> atomicAction) => new LifetimeDefinition(this, atomicAction);
    
    /// <summary>
    /// Keeps object from being garbage collected until this lifetime is terminated.
    /// <seealso cref="Lifetimed{T}"/>
    /// </summary>
    /// <param name="object"></param>
    /// <returns></returns>
    /// <exception cref="ArgumentNullException"></exception>
    [PublicAPI]
    public Lifetime KeepAlive(object @object)
    {
      if (@object == null) throw new ArgumentNullException(nameof(@object));      

      return OnTermination(() => GC.KeepAlive(@object));
    }

    /// <summary>
    /// <inheritdoc cref="OnTermination(IDisposable)"/>
    /// <remarks>It's more clear for code review purposes when you are adding IDisposable by <c>AddDispose</c> rather than by <c>OnTermination</c></remarks>
    /// </summary>
    /// <param name="disposable"></param>
    /// <returns>The same lifetime (fluent API)</returns>
    [PublicAPI] public Lifetime AddDispose(IDisposable disposable) => OnTermination(disposable);
    
    #endregion
               
    
    #region Obsolete
    
    // ReSharper disable InconsistentNaming
    [Obsolete("Use `Bracket` method instead")]
    public Lifetime AddBracket([InstantHandle] Action FOpening, Action FClosing) { Bracket(FOpening, FClosing); return this; }
    // ReSharper restore InconsistentNaming

    [Obsolete("Use `OnTermination()` instead")]
    public Lifetime AddAction(Action action) => OnTermination(action);

    [Obsolete("For most cases you need `IsNotAlive` which means lifetime is terminated or soon will be terminated (somebody called Terminate() on this lifetime or its parent)." +
              " If your operation makes sense in Canceling status (but must be stopped when resources termination already began) use Status < Terminating ")]
    public bool IsTerminated => Status >= LifetimeStatus.Terminating;

    [Obsolete("Use `KeepAlive() instead`")]
    public Lifetime AddRef(object @object) => KeepAlive(@object);

    
    /// <summary>
    /// Synchronizes termination of two lifetime definitions.
    /// Whenever any one is terminated, the other will be terminated also.
    /// </summary>
    [Obsolete("Reconsider your architecture and use Intersect")]
    public static void Synchronize(params LifetimeDefinition[] definitions)
    {
      if(definitions == null)
        throw new ArgumentNullException(nameof(definitions));

      // All pairs
      foreach(LifetimeDefinition alpha in definitions)
      {
        if(alpha == null)
          throw new ArgumentNullException(nameof(definitions), "All definitions must be non-Null.");
        foreach(LifetimeDefinition betta in definitions)
        {          
          if(alpha != betta)
            alpha.Attach(betta, false);            
        }
      }
    }
    
    #endregion
    
    
    
    #region Intersection


    /// <summary>
    /// Creates an intersection with other lifetime: new lifetime that terminate when either one terminates.
    /// </summary>
    [PublicAPI] 
    public Lifetime Intersect(Lifetime other) => Intersect(this, other);
    
    /// <summary>
    /// Creates an intersection of some lifetimes: new lifetime that terminate when either one terminates.
    /// Created lifetime inherits the smallest <see cref="TerminationTimeoutKind"/>  
    /// </summary>
    [PublicAPI]
    public static Lifetime Intersect(Lifetime lifetime1, Lifetime lifetime2) //reduces memory traffic
    {
      if (lifetime1.IsEternal || lifetime1 == lifetime2)
        return lifetime2;
      if (lifetime2.IsEternal)
        return lifetime1;
      
      return DefineIntersection(lifetime1, lifetime2).Lifetime;
    }
    
    /// <summary>
    /// <inheritdoc cref="Intersect(JetBrains.Lifetimes.Lifetime, Lifetime)"/>
    /// </summary>
    [PublicAPI] public static Lifetime Intersect(params Lifetime[] lifetimes)
    {
      return DefineIntersection(lifetimes).Lifetime;
    }

    /// <summary>
    /// <inheritdoc cref="Intersect(JetBrains.Lifetimes.Lifetime, Lifetime)"/>
    ///
    /// This method returns definition rather than lifetime, so in addition to any parent's termination you can
    /// <see cref="LifetimeDefinition.Terminate"/> it manually.  
    /// </summary>
    public static LifetimeDefinition DefineIntersection(Lifetime lifetime1, Lifetime lifetime2) //reduces memory traffic
    {
      if (lifetime1.Status >= LifetimeStatus.Terminating || lifetime2.Status >= LifetimeStatus.Terminating)
        return LifetimeDefinition.Terminated;

      if (lifetime1 == lifetime2)
        return lifetime1.CreateNested();

      var timeoutKind1 = lifetime1.TerminationTimeoutKind;
      var timeoutKind2 = lifetime2.TerminationTimeoutKind;
      
      var res = new LifetimeDefinition
      {
        TerminationTimeoutKind = timeoutKind1 > timeoutKind2 ? timeoutKind2 : timeoutKind1 
      };
      
      lifetime1.Definition.Attach(res, false);
      lifetime2.Definition.Attach(res, false);
      return res;
    }
    
    /// <summary>
    /// <inheritdoc cref="DefineIntersection(JetBrains.Lifetimes.Lifetime, Lifetime)"/>
    /// </summary>
    [PublicAPI]
    public static LifetimeDefinition DefineIntersection(params Lifetime[] lifetimes)
    {
      if (lifetimes == null) throw new ArgumentNullException(nameof(lifetimes));
      if (Mode.IsAssertion) Assertion.Assert(lifetimes.Length > 0, "One or more parameters must be passed");

      var res = new LifetimeDefinition();
      var minTimeoutKind = (LifetimeTerminationTimeoutKind)int.MaxValue;
      foreach (var lf in lifetimes)
      {
        lf.Definition.Attach(res, false);
        
        var timeoutKind = lf.TerminationTimeoutKind;
        if (timeoutKind < minTimeoutKind)
          minTimeoutKind = timeoutKind;
      }

      res.TerminationTimeoutKind = minTimeoutKind;

      return res;
    }
        
    #endregion

    
    
    
    #region Diagnostics

    /// <summary>
    /// <inheritdoc cref="LifetimeDefinition.Id"/>
    /// </summary>
    [PublicAPI] public object? Id
    {
      get => Definition.Id;
      set => Definition.Id = value;
    }
    
    /// <summary>
    /// <inheritdoc cref="LifetimeDefinition.EnableTerminationLogging"/>
    /// </summary>
    [PublicAPI] public void EnableTerminationLogging() => Definition.EnableTerminationLogging();
    public override string ToString() => Definition.ToString();
    
    #endregion
    
    
    
    #region Old define
    
    /// <summary>
    ///   <para>Defines a new lifetime nested within the <paramref name="lifetime" /> you pass in.</para>
    /// </summary>
    /// <remarks>
    ///   <para>In most cases, you should have some lifetime to use as a parent, such as the lifetime of your component. If this is not the case, and you just need a function-scoped lifetime, call <see cref="Using(System.Action{Lifetime})" /> instead. It is terminated automatically when your action ends and thus does not need a parent.</para>
    ///   <para>If the lifetime you're created is really not parented by any other lifetime, use the <see cref="Eternal" /> as a parent.</para>
    /// </remarks>
    /// <param name="lifetime">
    ///   <para>The parent lifetime.</para>
    ///   <para>The newly-created lifetime will be nested within the parent lifetime and thus terminated automatically when the parent lifetime ends (unless the nested lifetime is terminated first).</para>
    ///   <para>Nested lifetimes are listed within the parent lifetime, but as they're terminated, the records are removed. There will be no memory leak on the parent lifetime if the nested lifetimes are terminated.</para>
    /// </param>
    /// <param name="id">
    ///   <para>Optional. The ID of the lifetime.</para>
    ///   <para>Used for tracking and debugging. If the call stack annotations feature is ON, this ID will appear on the call stack when the lifetime object starts executing scheduled actions upon termination.</para>
    ///   <para>In case of nested lifetimes and if scheduled actions are anonymous in their nature, it might be hard to tell what's happening from exception stack traces without this annotation. You're encouraged to specify IDs wherever such situations are suspected, but the IDs should better be statically defined (to avoid memory leaks on part of the call stacks annotation engine).</para>
    ///   <para>If omitted, the default <see cref="LifetimeDefinition.AnonymousLifetimeId" /> or the <see cref="Lifetime" /> should type name is used, depending on the context.</para>
    /// </param>
    /// <param name="atomicAction">
    ///   <para>Optional. The code to be executed atomically on the newly-created lifetime.</para>
    ///   <para>If this code succeeds (or is not specified), the definition of the new lifetime is returned from the method.</para>
    ///   <para>If this code fails with an exception, the newly-created lifetime is terminated, all of the scheduled actions are executed (rolling back any activities already bound to the lifetime), the nested lifetime is not registered no the parent, and the exception is let out of this method.</para>
    /// </param>    
    /// <returns>
    ///   <para>The definition to the new lifetime.</para>
    ///   <para>As you own the lifetime, you can terminate it through this definition at any time.</para>
    ///   <para>To pass the lifetime to objects&amp;functions or schedule termination actions on it, get it from the <see cref="LifetimeDefinition.Lifetime" /> property. Do not pass the definition itself to child objects, unless this is the intended scenario to allow them to terminate the lifetime upon their discretion (e. g. a user-cancelable non-modal dialog).</para>
    /// </returns>
    /// <seealso cref="Using(System.Action{Lifetime})" />
    ///
    /// <remarks>For compatibility reason logic is different than <see cref="LifetimeDefinition(Lifetime, Action{LifetimeDefinition})"/>: in this method
    /// <paramref name="atomicAction"/> is always executed</remarks>
    public static LifetimeDefinition Define(Lifetime lifetime, string? id = null, [InstantHandle] Action<LifetimeDefinition>? atomicAction = null)
    {
      var res = new LifetimeDefinition {Id = id};
      try
      {
        atomicAction?.Invoke(res);
        lifetime.Definition.Attach(res, true);
        return res;
      }
      catch (Exception)
      {
        res.Terminate();
        throw;
      }
      
      //todo differs from current logic: does not execute `atomicAction` if lifetime is terminated
      //return new LifetimeDefinition(lifetime, atomicAction) {Id = id}; 
    }

    /// <summary>
    /// Same as <see cref="Define(Lifetime, string, Action{LifetimeDefinition})"/>
    /// </summary>
    /// <param name="lifetime"></param>
    /// <param name="atomicAction"></param>
    /// <returns></returns>
    public static LifetimeDefinition Define(Lifetime lifetime, [InstantHandle] Action<Lifetime>? atomicAction) => Define(lifetime, null, atomicAction);

    /// <summary>
    /// Same as <see cref="Define(Lifetime, string, Action{LifetimeDefinition})"/>
    /// </summary>
    /// <param name="lifetime"></param>
    /// <param name="id"></param>
    /// <param name="atomicAction"></param>
    /// <returns></returns>
    public static LifetimeDefinition Define(Lifetime lifetime, string? id, [InstantHandle] Action<Lifetime>? atomicAction)
    {
      var res = new LifetimeDefinition {Id = id};
      try
      {
        atomicAction?.Invoke(res.Lifetime);
        lifetime.Definition.Attach(res, true);
        return res;
      }
      catch (Exception)
      {
        res.Terminate();
        throw;
      }
      
      //todo differs from current logic: does not execute `atomicAction` if lifetime is terminated
      //return new LifetimeDefinition(lifetime, atomicAction) {Id = id};
    }
    
    /// <summary>
    /// Same as <see cref="LifetimeDefinition()"/> with <see cref="Id"/> = <paramref name="id"/>
    /// </summary>
    /// <param name="id"></param>
    /// <returns></returns>
    [Pure]
    public static LifetimeDefinition Define(string? id = null) => new LifetimeDefinition {Id = id};
    
    #endregion
    

    #if !NET35
    #region Task API
    
    
    [MethodImpl(MethodImplOptions.AggressiveInlining)] private ScopedAsyncLocal<Lifetime> UsingAsyncLocal() => new ScopedAsyncLocal<Lifetime>(AsyncLocal, this);


    /// <summary>
    /// Do the same as <see cref="Execute{T}"/> but also (if <see cref="IsAlive"/>) suppress lifetime termination until task returned by <paramref name="closure"/>
    /// is finished. Task will see this lifetime in <see cref="AsyncLocal"/>.
    /// </summary>
    /// <param name="closure"></param>
    /// <returns></returns>
    [PublicAPI] public Task ExecuteAsync(Func<Task> closure) { using (UsingAsyncLocal()) return Definition.ExecuteAsync(closure); }
    
    /// <summary>
    /// <inheritdoc cref="ExecuteAsync"/>
    /// </summary>
    /// <param name="closure"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Task<T> ExecuteAsync<T>(Func<Task<T>> closure)  { using (UsingAsyncLocal()) return Definition.ExecuteAsync(closure); }

    /// <summary>
    /// Do the same as <see cref="TryExecute{T}"/> but also (if <see cref="IsAlive"/>) suppress lifetime termination until task returned by <paramref name="closure"/>
    /// is finished. Task will see this lifetime in <see cref="AsyncLocal"/>.
    /// </summary>
    /// <param name="closure"></param>
    /// <param name="wrapExceptions"></param>
    /// <returns></returns>
    [PublicAPI] public Task TryExecuteAsync(Func<Task> closure, bool wrapExceptions = false) => Definition.TryExecuteAsync(closure, wrapExceptions);
    
    /// <summary>
    /// <inheritdoc cref="ExecuteAsync"/>
    /// </summary>
    /// <param name="closure"></param>
    /// <param name="wrapExceptions"></param>
    /// <returns></returns>
    [PublicAPI] public Task<T> TryExecuteAsync<T>(Func<Task<T>> closure, bool wrapExceptions = false) => Definition.TryExecuteAsync(closure, wrapExceptions);
    
    
    /// <summary>
    /// Starts task with this lifetime as <see cref="CancellationToken"/>. Task will see this lifetime in <see cref="AsyncLocal"/>.
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="options"></param>
    /// <returns></returns>
    [PublicAPI] public Task Start(TaskScheduler scheduler, Action action, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, this, options, scheduler); }

    /// <summary>
    /// <inheritdoc cref="Start(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="state"></param>
    /// <param name="options"></param>
    /// <returns></returns>
    [PublicAPI] public Task Start(TaskScheduler scheduler, Action<object> action, object state, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, state, this, options, scheduler); }
    
    /// <summary>
    /// <inheritdoc cref="Start(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="options"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Task<T> Start<T>(TaskScheduler scheduler, Func<T> action, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, this, options, scheduler); }

    /// <summary>
    /// <inheritdoc cref="Start(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="state"></param>
    /// <param name="options"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Task<T> Start<T>(TaskScheduler scheduler, Func<object, T> action, object state, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, state, this, options, scheduler); }
    
    /// <summary>
    /// <inheritdoc cref="Start(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="options"></param>
    /// <returns></returns>
    [PublicAPI] public Task StartAsync(TaskScheduler scheduler, Func<Task> action, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, this, options, scheduler).Unwrap(); }

    /// <summary>
    /// <inheritdoc cref="Start(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="state"></param>
    /// <param name="options"></param>
    /// <returns></returns>
    [PublicAPI] public Task StartAsync(TaskScheduler scheduler, Func<object, Task> action, object state, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, state, this, options, scheduler).Unwrap(); }
    
    /// <summary>
    /// <inheritdoc cref="Start(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="options"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Task<T> StartAsync<T>(TaskScheduler scheduler, Func<Task<T>> action, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, this, options, scheduler).Unwrap(); }

    /// <summary>
    /// <inheritdoc cref="Start(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="state"></param>
    /// <param name="options"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Task<T> StartAsync<T>(TaskScheduler scheduler, Func<object, Task<T>> action, object state, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, state, this, options, scheduler).Unwrap(); }


    /// <summary>
    /// Starts task with this lifetime as <see cref="CancellationToken"/> but also (if <see cref="IsAlive"/>) suppress
    /// lifetime termination until task is finished. Task will see this lifetime in <see cref="AsyncLocal"/>.
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="options"></param>
    /// <returns></returns>
    [PublicAPI] public Task StartAttached(TaskScheduler scheduler, Action action, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(Start(scheduler, action, options));

    /// <summary>
    /// Starts task with this lifetime as <see cref="CancellationToken"/> but also (if <see cref="IsAlive"/>) suppress
    /// lifetime termination until task is finished. Task will see this lifetime in <see cref="AsyncLocal"/>.
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="state"></param>
    /// <param name="options"></param>
    /// <returns></returns>
    [PublicAPI] public Task StartAttached(TaskScheduler scheduler, Action<object> action, object state, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(Start(scheduler, action, state, options));
    
    /// <summary>
    /// <inheritdoc cref="StartAttached(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="options"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Task<T> StartAttached<T>(TaskScheduler scheduler, Func<T> action, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(Start(scheduler, action, options));

    /// <summary>
    /// <inheritdoc cref="StartAttached(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="state"></param>
    /// <param name="options"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Task<T> StartAttached<T>(TaskScheduler scheduler, Func<object, T> action, object state, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(Start(scheduler, action, state, options));

    /// <summary>
    /// <inheritdoc cref="StartAttached(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="options"></param>
    /// <returns></returns>
    [PublicAPI] public Task StartAttachedAsync(TaskScheduler scheduler, Func<Task> action, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(StartAsync(scheduler, action, options));

    /// <summary>
    /// <inheritdoc cref="StartAttached(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="state"></param>
    /// <param name="options"></param>
    /// <returns></returns>
    [PublicAPI] public Task StartAttachedAsync(TaskScheduler scheduler, Func<object, Task> action, object state, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(StartAsync(scheduler, action, state, options));

    /// <summary>
    /// <inheritdoc cref="StartAttached(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="options"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Task<T> StartAttachedAsync<T>(TaskScheduler scheduler, Func<Task<T>> action, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(StartAsync(scheduler, action, options));

    /// <summary>
    /// <inheritdoc cref="StartAttached(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// </summary>
    /// <param name="scheduler"></param>
    /// <param name="action"></param>
    /// <param name="state"></param>
    /// <param name="options"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Task<T> StartAttachedAsync<T>(TaskScheduler scheduler, Func<object, Task<T>> action, object state, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(StartAsync(scheduler, action, state, options));


    /// <summary>
    /// Creates new <see cref="TaskCompletionSource{TResult}"/> that translate <see cref="Task.Status"/> into canceled if this lifetime becomes terminates (and task wasn't completed before).
    /// </summary>
    /// <param name="options">to pass into <see cref="TaskCompletionSource{TResult}.ctor"/></param>
    /// <typeparam name="T"></typeparam>
    /// <returns>New <see cref="TaskCompletionSource{TResult}"/>. Possibly with already canceled <see cref="TaskCompletionSource{TResult}.Task"/> if this lifetime already terminated</returns>
    [PublicAPI] public TaskCompletionSource<T> CreateTaskCompletionSource<T>(TaskCreationOptions options = TaskCreationOptions.None)
    {
      var res = new TaskCompletionSource<T>(options);
      
      //Nested definition is better that just adding termination action because it prevents memory leak
      CreateNested().SynchronizeWith(res);
      return res;
    }


    /// <summary>
    /// Returns lifetime that is child of this lifetime and will be terminated after specified period of time.
    /// </summary>
    /// <param name="timeSpan">Terminate lifetime after this amount of time</param>
    /// <param name="terminationScheduler">Scheduler where termination will be invoked (if none specified, termination will start in timer's thread)</param>
    [PublicAPI] public Lifetime CreateTerminatedAfter(TimeSpan timeSpan, TaskScheduler? terminationScheduler = null)
    {
      var def = new LifetimeDefinition(this);
      Task.Delay(timeSpan, this).ContinueWith(
        _ => def.Terminate(), 
        this, 
        TaskContinuationOptions.None, 
        terminationScheduler ?? SynchronousScheduler.Instance
        );

      return def.Lifetime;
    }
    
    
    #endregion
    #endif


    #region Equality
    
    public static bool operator ==(Lifetime left, Lifetime right) 
    {
      return ReferenceEquals(left.Definition, right.Definition); 
    }

    public static bool operator !=(Lifetime left, Lifetime right)
    {
      return !(left == right);
    }
    
    public bool Equals(Lifetime other)
    {
      return ReferenceEquals(Definition, other.Definition);
    }

    public override bool Equals(object? obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      return obj is Lifetime other && Equals(other);
    }

    public override int GetHashCode()
    {
      return Definition.GetHashCode();
    }

    #endregion

    
    
    #region Performance critical: Boxing/Unboxing

    /// <summary>
    /// Box lifetime without allocation. Must be use in pair with <see cref="TryUnwrapAsObject"/>.
    /// </summary>
    /// <param name="lifetime">Lifetime that you want to box without allocation</param>
    /// <returns><see cref="LifetimeDefinition"/> for this lifetime. ONLY for zero-allocation purpose.</returns>
    [PublicAPI] public static object WrapAsObject(Lifetime lifetime) => lifetime.Definition;

    /// <summary>
    /// Try to unwrap object into <see cref="Lifetime"/>. Must be used in pair with <see cref="WrapAsObject"/>.
    /// </summary>
    /// <param name="obj">object that potentially is LifetimeDefinition previously wrapped by <see cref="WrapAsObject"/></param>
    /// <param name="lifetime">if <code>obj is LifetimeDefinition</code> returns corresponding <see cref="LifetimeDefinition.Lifetime"/>; else returns <see cref="Eternal"/></param>
    /// <returns>true if <code>obj is LifetimeDefinition</code> previously wrapped by <see cref="WrapAsObject"/>. false otherwise</returns>
    [PublicAPI] public static bool TryUnwrapAsObject(object obj, out Lifetime lifetime)
    {
      var ld = obj as LifetimeDefinition;
      lifetime = ld?.Lifetime ?? Eternal;
      return ld != null;
    }
    

    #endregion


    #region Asserts

    /// <summary>
    /// <para>If this lifetime never gets terminated (and all the references to it get lost), reports an exception to the logger.</para>
    /// <para>The lifetime will not be terminated automatically, because this is not a lifetime definition. See Remarks for options.</para>
    /// <para>Note that this method means certain load on the finalization queue, and can degrade performance if used in large amounts.</para>
    /// <para>Fluent.</para>
    /// </summary>
    /// <remarks>
    /// <para>As this method operates on a <see cref="Lifetime"/> object you do not own, it cannot terminate the lifetime automatically when a missed termination is detected.</para>
    /// </remarks>
    public Lifetime AssertEverTerminated(string? comment = null)
    {
      Definition.AssertEverTerminated(comment);
      return this;
    }

    #if !NET35
    /// <summary>
    /// Log error if this lifetime hasn't been terminated in specified <paramref name="timeout"/> 
    /// </summary>
    /// <param name="timeout">Maximum timeout to wait this lifetime is terminated</param>
    /// <param name="comment">Optional comment to log when assertion failed</param>
    public async void AssertTerminatesIn(TimeSpan timeout, string? comment = null)
    {
      try
      {
        await Task.Delay(timeout, this).ConfigureAwait(false);
      }
      catch (OperationCanceledException)
      {
        return; // Everything is OK
      }
      
      LifetimeDefinition.Log.Error("{0} hasn't been terminated in {1} {2}", this, timeout, string.IsNullOrEmpty(comment) ? "" : comment);
    }
    #endif
      

        

    #endregion

    
    #region Retry

    /// <summary>
    /// Creates and starts new <paramref name="task"/> while it is finished with <see cref="OperationCanceledException"/>.
    /// Returned task can be finished in:
    /// <list type="number">
    /// <item>successfully - if some started <paramref name="task"/> finished successfully</item>
    /// <item>failed - if some started <paramref name="task"/> finished with exception other than <see cref="OperationCanceledException"/></item>
    /// <item>canceled - if this lifetime <see cref="IsNotAlive"/></item>
    /// </list>
    /// </summary>
    /// <param name="task"></param>
    /// <returns></returns>
    /// <exception cref="LifetimeCanceledException"></exception>
    [PublicAPI] public async Task RetryWhileOperationCancellingAsync(Func<Task> task)
    {
      while (IsAlive)
      {  
        try
        {
          await task();
          return;
        }
        catch (OperationCanceledException) 
        {
          //retry
        }
      }
      throw new LifetimeCanceledException(this);
    }
    
    
    /// <summary>
    /// <inheritdoc cref="RetryWhileOperationCancellingAsync(Func{Task})"/>
    /// </summary>
    /// <param name="task"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    /// <exception cref="LifetimeCanceledException"></exception>
    [PublicAPI] public async Task<T> RetryWhileOperationCancellingAsync<T>(Func<Task<T>> task)
    {
      while (IsAlive)
      {  
        try
        {
          return await task();
        }
        catch (Exception e) when (e.IsOperationCanceled())
        {
          //retry
        }
      }
      
      throw new LifetimeCanceledException(this);
    }
    
    
    #endregion
  }
}
