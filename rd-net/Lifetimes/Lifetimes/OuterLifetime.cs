using System;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Diagnostics;

namespace JetBrains.Lifetimes
{
  /// <summary>
  ///   <para>A subset of the <see cref="Lifetime" /> interface with which you cannot “bind” actions to a lifetime.</para>
  ///   <para>It's “outer” in the sense that it's not your lifetime, but of some parent object potentially more long-lived than yours. You cannot schedule anything to its termination because it will happen way after your object goes off scope.</para>
  ///   <para>The only thing you can know is that it's an “outer”, it's limiting the life of your object, and if it's terminated — so are you. Checking for <see cref="IsTerminated" /> is one of the allowed option.</para>
  ///   <para>Another option is to define a nested lifetime, in which case you MUST ensure it's terminated explicitly, without relying on the outer lifetime. The outer lifetime is only a safety catch to make sure it does not live too long.</para>
  /// </summary>
  public struct OuterLifetime
  {
    #region Data

    private readonly Lifetime myLifetime;

    #endregion

    #region Init

    private OuterLifetime(Lifetime lifetime)
    {
      myLifetime = lifetime;
    }

    #endregion

    #region Attributes

    /// <summary>
    ///   <para>Gets whether this lifetime has already been terminated.</para>
    ///   <para>It's an error to continue scheduling on a terminated lifetime.</para>
    /// </summary>
    public bool IsTerminated
    {
      get
      {
#pragma warning disable 618
        return myLifetime.IsTerminated;
#pragma warning restore 618
      }
    }

#if JET_MODE_ANNOTATE_CALL_STACKS
    internal string myId
    {
      get
      {
        return myLifetime != default ? myLifetime.myId : "<NULL>";
      }
    }
#endif

    #endregion

    #region Operations

    public static implicit operator OuterLifetime(Lifetime lifetime)
    {
      return new OuterLifetime(lifetime);
    }

    public static implicit operator OuterLifetime(LifetimeDefinition lifetime)
    {
      return new OuterLifetime(lifetime.Lifetime);
    }

    public void AssertNotNull()
    {
//      if(myLifetime == null)
//        throw new NullReferenceException("This OuterLifetime object is Null.");
    }

    #endregion

    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    ///   <para>The parent lifetime which might cause premature termination of our lifetime (and, supposedly, the chain of tasks executed under the lifetime, if started correctly).</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    public Task<TRetVal> UsingNestedAsync<TRetVal>([InstantHandle] Func<Lifetime, Task<TRetVal>> action) => myLifetime.UsingNestedAsync(action);

    /// <summary>
    ///   <para>Scopes your code in <paramref name="action" /> with a lifetime that is terminated automatically when <paramref name="action" /> completes execution, or when its execution is aborted by an exception.</para>
    ///   <para>Analogous to the <c>using</c> statement of the C# language on everything that is added to the lifetime.</para>
    ///   <para>The parent lifetime which might cause premature termination of our lifetime (and, supposedly, the chain of tasks executed under the lifetime, if started correctly).</para>
    /// </summary>
    /// <param name="action">The code to execute with a temporary lifetime.</param>
    public Task UsingNestedAsync([InstantHandle] Func<Lifetime, Task> action) => myLifetime.UsingNestedAsync(action);
    
    internal LifetimeDefinition Def => myLifetime.Definition;

    /// <summary>
    ///   <para>See documentation on an overload which takes a <see cref="Lifetime" />.</para>
    /// </summary>
    public static LifetimeDefinition Define(OuterLifetime lifetime, string? id = null, [InstantHandle] Action<LifetimeDefinition, Lifetime>? atomicAction = null)
    {
      lifetime.AssertNotNull();

      // Fallback to defaults
      if(String.IsNullOrEmpty(id))
        id = LifetimeDefinition.AnonymousLifetimeId;

//      var nested = new LifetimeDefinition(id, 0, logger ?? Log.Root);
      var nested = new LifetimeDefinition {Id = id}/*{ Logger = logger ?? Log.Root }*/;
      try
      {
        // Atomic init on the new lifetime
        atomicAction?.Invoke(nested, nested.Lifetime);

        // Attach as nested to the parent lifetime
//        if(!nested.IsTerminated) // Might have been terminated by FAtomic
        lifetime.Def.Attach(nested, true); // Pass True: might be terminated async on another thread between our check and AttachNested body (example: Queue from another thread)
      }
      catch
      {
        nested.Terminate();
        throw;
      }
      return nested;
    }
    
    
        
    /// <summary>
    /// Creates an intersection of some lifetimes — a lifetime to terminate when either one terminates.
    /// Created lifetime inherits the smallest <see cref="Lifetime.TerminationTimeoutKind"/>
    /// </summary>
    [PublicAPI]
    public static LifetimeDefinition DefineIntersection(params OuterLifetime[] lifetimes)
    {
      Assertion.Assert(lifetimes.Length > 0, "One or more parameters must be passed");
      var res = new LifetimeDefinition();
      var minTimeoutKind = (LifetimeTerminationTimeoutKind)int.MaxValue;
      foreach (var lf in lifetimes)
      {
        lf.Def.Attach(res, false);

        var timeoutKind = lf.Def.TerminationTimeoutKind;
        if (minTimeoutKind > timeoutKind)
          minTimeoutKind = timeoutKind;
      }

      res.TerminationTimeoutKind = minTimeoutKind;

      return res;
    }
  }
}