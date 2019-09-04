using System;
using System.Runtime.CompilerServices;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
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
  /// Central class in <see cref="JetBrains.Lifetimes"/> package. Has two main function:<br/>
  /// 1. High performance analogue of <see cref="CancellationToken"/>. Analogue of <see cref="CancellationTokenSource"/> <br/>
  /// 2. Inversion of <see cref="IDisposable"/> pattern (with thread-safety):
  /// user can add termination resources into Lifetime with bunch of <c>OnTermination</c> (e.g. <see cref="OnTermination(Action)"/>) methods.
  /// When lifetime is being terminated (i.e. it's <see cref="LifetimeDefinition"/> was called <see cref="LifetimeDefinition.Terminate"/>) all
  /// previously added termination resources are being terminated in stack-way LIFO order. Lifetimes forms a hierarchy with parent-child relations so in single-threaded world child always
  /// becomes <see cref="LifetimeStatus.Terminated"/> <b>BEFORE</b> parent. Usually this hierarchy is a tree but it some cases (like <see cref="Intersect(JetBrains.Lifetimes.Lifetime[])"/> it can be
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
    
    [CanBeNull] private readonly LifetimeDefinition myDefinition;   
    [NotNull] internal LifetimeDefinition Definition => myDefinition ?? LifetimeDefinition.Eternal;   
    
    //ctor
    internal Lifetime([NotNull] LifetimeDefinition definition)
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
    [PublicAPI] public Lifetime OnTermination([NotNull] Action action) { Definition.OnTermination(action); return this; }
    
    
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
    [PublicAPI] public Lifetime OnTermination([NotNull] IDisposable disposable) { Definition.OnTermination(disposable); return this; }
    
    
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
    [PublicAPI] public Lifetime OnTermination([NotNull] ITerminationHandler terminationHandler) { Definition.OnTermination(terminationHandler); return this; }
    
    
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
    [PublicAPI]          public bool TryOnTermination([NotNull] Action action)      => Definition.TryAdd(action);
    
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
    [PublicAPI]          public bool TryOnTermination([NotNull] IDisposable disposable) => Definition.TryAdd(disposable);
    
    
    /// <summary>
    /// Add termination resource with <c>kind == ITerminationHandler</c> that will be invoked when lifetime termination starts 
    /// (i.e. <see cref="LifetimeDefinition.Terminate"/> is called, <see cref="ExecutingCount"/> became zero, so status is set to <see cref="LifetimeStatus.Terminating"/>).
    /// Resources invocation order: LIFO
    /// All errors are logger by <see cref="ILog"/>  so termination of each resource is isolated.
    ///
    /// Method returns do nothing and return `false` if <see cref="Status"/> &ge; <see cref="LifetimeStatus.Terminating"/>. 
    /// </summary>
    /// <param name="action">Action to invoke on termination</param>
    /// <returns><c>true</c> if resource added - only status &le; <see cref="LifetimeStatus.Canceling"/>. <c>false</c> if resource's not added - status &ge; <see cref="LifetimeStatus.Terminating"/> </returns>

    [PublicAPI]          public bool TryOnTermination([NotNull] ITerminationHandler disposable) => Definition.TryAdd(disposable); 

    #endregion

    
    #region Execute If Alive
    
    /// <summary>
    /// Number of background activities started by <see cref="Execute{T}"/>, <see cref="ExecuteAsync"/> or <see cref="StartNested(System.Threading.Tasks.TaskScheduler,System.Action,System.Threading.Tasks.TaskCreationOptions)"/>
    /// When lifetime became <see cref="LifetimeStatus.Canceling"/> (it happens right after user ask for this lifetime's or ancestor lifetime's definition <see cref="LifetimeDefinition.Terminate"/>) this
    /// number could only reduce, no new activities can be started.
    /// When it reach zero, lifetime begins to terminate its resources by changing <see cref="Status"/> to <see cref="LifetimeStatus.Terminating"/>  
    /// </summary>
    [PublicAPI] public int ExecutingCount          => Definition.ExecutingCount; 
    

    /// <summary>
    /// If you terminate lifetime definition <see cref="LifetimeDefinition.Terminate"/> under <see cref="Execute{T}"/> you'll get exception because 
    /// it's impossible to keep guarantee that code under <see cref="Execute{T}"/> works with non-terminating livetime.
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
    /// 
    /// </summary>
    /// <param name="action"></param>
    /// <param name="wrapExceptions"></param>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    [PublicAPI] public Result<T> TryExecute<T>([NotNull, InstantHandle] Func<T> action, bool wrapExceptions = false) => Definition.TryExecute(action, wrapExceptions);
    
    [PublicAPI] public Result<Unit> TryExecute([NotNull, InstantHandle] Action action, bool wrapExceptions = false) => Definition.TryExecute(action, wrapExceptions);
    
    [PublicAPI] public T Execute<T>([NotNull, InstantHandle] Func<T> action) => Definition.Execute(action);
    
    [PublicAPI] public void Execute([NotNull, InstantHandle] Action action) => Definition.Execute(action);
    
    
    #endregion
    
    
    
    
    #region Bracket
    
    [PublicAPI] public Result<Unit> TryBracket([NotNull, InstantHandle] Action opening, [NotNull] Action closing, bool wrapExceptions = false) => Definition.TryBracket(opening, closing, wrapExceptions);
    
    [PublicAPI] public Result<T> TryBracket<T>([NotNull, InstantHandle] Func<T> opening, [NotNull] Action closing, bool wrapExceptions = false) => Definition.TryBracket(opening, closing, wrapExceptions);
    
    [PublicAPI] public Result<T> TryBracket<T>([NotNull, InstantHandle] Func<T> opening, [NotNull] Action<T> closing, bool wrapExceptions = false) => Definition.TryBracket(opening, closing, wrapExceptions);
    
    
    
    [PublicAPI] public void Bracket([NotNull, InstantHandle] Action opening, [NotNull] Action closing) => Definition.Bracket(opening, closing);
    [PublicAPI] public T Bracket<T>([NotNull, InstantHandle] Func<T> opening, [NotNull] Action closing) => Definition.Bracket(opening, closing);    
    [PublicAPI] public T Bracket<T>([NotNull, InstantHandle] Func<T> opening, [NotNull] Action<T> closing) => Definition.Bracket(opening, closing);
    
    
    #endregion
    
    
    
    #region Cancellation

    [PublicAPI] public CancellationToken ToCancellationToken() => Definition.ToCancellationToken();
    [PublicAPI] public static implicit operator CancellationToken(Lifetime lifetime) => lifetime.Definition.ToCancellationToken();
    [PublicAPI] public void ThrowIfNotAlive() => Definition.ThrowIfNotAlive();

    #endregion
    
    
    #region Using      
    
    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    [PublicAPI]
    public static void Using([NotNull, InstantHandle] Action<Lifetime> action)
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
    public static T Using<T>([NotNull, InstantHandle] Func<Lifetime, T> action)
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
    public void UsingNested([NotNull, InstantHandle] Action<Lifetime> action)
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
    public T UsingNested<T>([NotNull, InstantHandle] Func<Lifetime, T> action)
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
    public static async Task UsingAsync([NotNull] [InstantHandle] Func<Lifetime, Task> action)
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
    public static async Task<T> UsingAsync<T>([NotNull] [InstantHandle] Func<Lifetime, Task<T>> action)
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
    /// <param name="parent">A parent lifetime which limits the lifetime given to your action, and migth terminate it before the action ends.</param>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    public static async Task UsingAsync(OuterLifetime parent, [NotNull] [InstantHandle] Func<Lifetime, Task> action)
    {
      if(action == null)
        throw new ArgumentNullException(nameof(action));

      using(var def = new LifetimeDefinition())
      {
        parent.Def.Attach(def);
        await action(def.Lifetime);
      }
    }

    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    /// </summary>
    /// <param name="parent">A parent lifetime which limits the lifetime given to your action, and migth terminate it before the action ends.</param>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    public static async Task<T> UsingAsync<T>(OuterLifetime parent, [NotNull] [InstantHandle] Func<Lifetime, Task<T>> action)
    {
      if(action == null)
        throw new ArgumentNullException(nameof(action));

      using(var def = new LifetimeDefinition())
      {
        parent.Def.Attach(def);
        return await action(def.Lifetime);
      }
    }
    
    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    ///   <para>The parent lifetime which might cause premature termination of our lifetime (and, supposedly, the chain of tasks executed under the lifetime, if started correctly).</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    public async Task<TRetVal> UsingNestedAsync<TRetVal>([NotNull] [InstantHandle] Func<Lifetime, Task<TRetVal>> action)
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
    public async Task UsingNestedAsync([NotNull] [InstantHandle] Func<Lifetime, Task> action)
    {
      if(action == null) throw new ArgumentNullException(nameof(action));

      using (var def = CreateNested())
        await action(def.Lifetime);
    }

   // #endif
    
    #endregion
    
    #region Candidates for extension methods
    
    [PublicAPI, NotNull]
    public LifetimeDefinition CreateNested() => new LifetimeDefinition(this);
        
    [PublicAPI, NotNull]
    public LifetimeDefinition CreateNested([NotNull, InstantHandle] Action<LifetimeDefinition> atomicAction) => new LifetimeDefinition(this, atomicAction);
    
    [PublicAPI]
    public Lifetime KeepAlive([NotNull] object @object)
    {
      if (@object == null) throw new ArgumentNullException(nameof(@object));      

      return OnTermination(() => GC.KeepAlive(@object));
    }

    #endregion
               
    
    #region Obsolete
    
    // ReSharper disable InconsistentNaming
    [Obsolete("Use `Bracket` method instead")]
    public Lifetime AddBracket([NotNull, InstantHandle] Action FOpening, [NotNull] Action FClosing) { Bracket(FOpening, FClosing); return this; }
    // ReSharper restore InconsistentNaming

    [Obsolete("Use `OnTermination()` instead")]
    public Lifetime AddAction([NotNull] Action action) => OnTermination(action);
    
    public Lifetime AddDispose([NotNull] IDisposable action) => OnTermination(action);

    [Obsolete("For most cases you need `IsNotAlive` which means lifetime is terminated or soon will be terminated (somebody called Terminate() on this lifetime or its parent)." +
              " If your operation makes sense in Canceled status (but must be stopped when resources termination already began) use Status < Terminating ")]
    public bool IsTerminated => Status >= LifetimeStatus.Terminating;

    [Obsolete("Use `KeepAlive() instead`")]
    public Lifetime AddRef([NotNull] object @object) => KeepAlive(@object);

    
    /// <summary>
    /// Synchronizes termination of two lifetime definitions.
    /// Whenever any one is terminated, the other will be terminated also.
    /// </summary>
    [Obsolete("Reconsider your architecture and use Intersect")]
    public static void Synchronize([NotNull] params LifetimeDefinition[] definitions)
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
            alpha.Attach(betta);            
        }
      }
    }
    
    #endregion
    
    
    
    #region Intersection


    
    /// <summary>
    /// Creates an intersection with other lifetime — a lifetime to terminate when either one terminates.
    /// </summary>
    [PublicAPI] 
    public Lifetime Intersect(Lifetime other) => Intersect(this, other);
    
    /// <summary>
    /// Creates an intersection of some lifetimes — a lifetime to terminate when either one terminates.
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
    
    [PublicAPI] public static Lifetime Intersect([NotNull] params Lifetime[] lifetimes)
    {
      return DefineIntersection(lifetimes).Lifetime;
    }

    public static LifetimeDefinition DefineIntersection(Lifetime lifetime1, Lifetime lifetime2) //reduces memory traffic
    {
      if (lifetime1.Status >= LifetimeStatus.Terminating || lifetime2.Status >= LifetimeStatus.Terminating)
        return LifetimeDefinition.Terminated;

      var res = new LifetimeDefinition();
      lifetime1.Definition.Attach(res);
      lifetime2.Definition.Attach(res);
      return res;
    }
    
    /// <summary>
    /// Creates an intersection of some lifetimes — a lifetime to terminate when either one terminates.
    /// </summary>
    [PublicAPI, NotNull]
    public static LifetimeDefinition DefineIntersection([NotNull] params Lifetime[] lifetimes)
    {
      if (lifetimes == null) throw new ArgumentNullException(nameof(lifetimes));
      
      var res = new LifetimeDefinition();
      foreach (var lf in lifetimes)
        lf.Definition.Attach(res);

      return res;
    }
        
    #endregion

    
    
    
    #region Diagnostics

    [PublicAPI, CanBeNull] public object Id
    {
      get => Definition.Id;
      set => Definition.Id = value;
    }
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
    [NotNull]
    public static LifetimeDefinition Define(Lifetime lifetime, [CanBeNull] string id = null, [CanBeNull] [InstantHandle] Action<LifetimeDefinition> atomicAction = null)
    {
      var res = new LifetimeDefinition {Id = id};
      try
      {
        atomicAction?.Invoke(res);
        lifetime.Definition.Attach(res);
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

    public static LifetimeDefinition Define(Lifetime lifetime, [CanBeNull] [InstantHandle] Action<Lifetime> atomicAction) => Define(lifetime, null, atomicAction);

    public static LifetimeDefinition Define(Lifetime lifetime, [CanBeNull] string id, [CanBeNull] [InstantHandle] Action<Lifetime> atomicAction)
    {
      var res = new LifetimeDefinition {Id = id};
      try
      {
        atomicAction?.Invoke(res.Lifetime);
        lifetime.Definition.Attach(res);
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
    
    [NotNull]
    [Pure]
    public static LifetimeDefinition Define([CanBeNull] string id = null) => new LifetimeDefinition {Id = id};
    
    #endregion
    

    #if !NET35
    #region Task API
    
    
    [MethodImpl(MethodImplOptions.AggressiveInlining)] private ScopedAsyncLocal<Lifetime> UsingAsyncLocal() => new ScopedAsyncLocal<Lifetime>(AsyncLocal, this);


    [PublicAPI, NotNull] public Task ExecuteAsync([NotNull] Func<Task> closure) { using (UsingAsyncLocal()) return Definition.ExecuteAsync(closure); }
    [PublicAPI, NotNull] public Task<T> ExecuteAsync<T>([NotNull] Func<Task<T>> closure)  { using (UsingAsyncLocal()) return Definition.ExecuteAsync(closure); }

    [PublicAPI, NotNull] public Task TryExecuteAsync([NotNull] Func<Task> closure, bool wrapExceptions = false) => Definition.TryExecuteAsync(closure, wrapExceptions);
    [PublicAPI, NotNull] public Task<T> TryExecuteAsync<T>([NotNull] Func<Task<T>> closure, bool wrapExceptions = false) => Definition.TryExecuteAsync(closure, wrapExceptions);
    
    
    [PublicAPI, NotNull] public Task Start(TaskScheduler scheduler, Action action, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, this, options, scheduler); }
    [PublicAPI, NotNull] public Task<T> Start<T>(TaskScheduler scheduler, Func<T> action, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, this, options, scheduler); }
    
    [PublicAPI, NotNull] public Task StartAsync(TaskScheduler scheduler, Func<Task> action, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, this, options, scheduler).Unwrap(); }
    [PublicAPI, NotNull] public Task<T> StartAsync<T>(TaskScheduler scheduler, Func<Task<T>> action, TaskCreationOptions options = TaskCreationOptions.None) { using (UsingAsyncLocal()) return Task.Factory.StartNew(action, this, options, scheduler).Unwrap(); }


    [PublicAPI, NotNull] public Task StartAttached(TaskScheduler scheduler, Action action, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(Start(scheduler, action, options)); 
    [PublicAPI, NotNull] public Task<T> StartAttached<T>(TaskScheduler scheduler, Func<T> action, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(Start(scheduler, action, options));
    
    [PublicAPI, NotNull] public Task StartAttachedAsync(TaskScheduler scheduler, Func<Task> action, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(StartAsync(scheduler, action, options));
    [PublicAPI, NotNull] public Task<T> StartAttachedAsync<T>(TaskScheduler scheduler, Func<Task<T>> action, TaskCreationOptions options = TaskCreationOptions.None) => Definition.Attached(StartAsync(scheduler, action, options));

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

    public override bool Equals(object obj)
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

    [PublicAPI] public static object WrapAsObject(Lifetime lifetime) => lifetime.Definition;

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
    public Lifetime AssertEverTerminated(string comment = null)
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
    public async void AssertTerminatesIn(TimeSpan timeout, string comment = null)
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

    public async Task RetryWhileOperationCancellingAsync(Func<Task> task)
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
    
    
    public async Task<T> RetryWhileOperationCancellingAsync<T>(Func<Task<T>> task)
    {
      while (IsAlive)
      {  
        try
        {
          return await task();
        }
        catch (OperationCanceledException) //use isOCE
        {
          //retry
        }
      }
      
      throw new LifetimeCanceledException(this);
    }
    
    
    #endregion
  }
}