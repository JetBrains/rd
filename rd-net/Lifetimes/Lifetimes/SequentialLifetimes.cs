using System;
using System.Threading;

using JetBrains.Annotations;
using JetBrains.Diagnostics;

namespace JetBrains.Lifetimes
{
  /// <summary>
  /// Maintains a sequence of lifetimes, so that the previous lifetime is closed before the new one is opened.
  /// Good for implementing a sequence of non-simultaneous activities when it's nice to guarantee only one is active at a time.
  /// </summary>
  public class SequentialLifetimes
  {
    
    private readonly Lifetime myParentLifetime;
    [NotNull] private LifetimeDefinition myCurrentDef = LifetimeDefinition.Terminated;

    /// <summary>Creates and binds to the lifetime.</summary>
    /// <param name="lifetime">When this lifetime is closed, the last of the sequential lifetimes is closed too.</param>
    public SequentialLifetimes(Lifetime lifetime)
    {
      myParentLifetime = lifetime;
    }

    public Lifetime Next()
    {
      TerminateCurrent();
      var next = new LifetimeDefinition(myParentLifetime);
      return SetNextAndTerminateCurrent(next).Lifetime;
    }

    /// <summary>
    /// Terminates the current lifetime and calls your handler with the new lifetime.
    /// </summary>
    public void Next([NotNull] Action<Lifetime> atomicAction)
    {
      if (atomicAction == null) throw new ArgumentNullException(nameof(atomicAction));
      TerminateCurrent();
      var next = new LifetimeDefinition(myParentLifetime, atomicAction);
      SetNextAndTerminateCurrent(next);
    }

    public void DefineNext([NotNull] Action<LifetimeDefinition> atomicAction)
    {
      if (atomicAction == null) throw new ArgumentNullException(nameof(atomicAction));
      
      TerminateCurrent();
      var next = new LifetimeDefinition(myParentLifetime, atomicAction);
      SetNextAndTerminateCurrent(next);
    }
    
    /// <summary>
    /// Terminates the current lifetime and calls your handler with the new lifetime.
    /// The lifetime definition allows to terminate it as desired.
    /// Also, the lifetime will be terminated when either parent lifetime is terminated,
    /// or <see cref="TerminateCurrent"/> is called, or <see cref="DefineNext"/>/<see cref="Next()"/> is called.
    /// </summary>

    /// <summary>
    /// Terminates the current lifetime.
    /// </summary>
    public void TerminateCurrent()
    {
      SetNextAndTerminateCurrent(LifetimeDefinition.Terminated);
    }

    // For sequential usage (say single-threaded or actor-like) usage only.
    public bool IsCurrentTerminated => myCurrentDef.Status == LifetimeStatus.Terminated;

    /// <summary>
    /// Atomically, assigns the new lifetime and terminates the old one.
    /// </summary>
    
    private LifetimeDefinition SetNextAndTerminateCurrent([NotNull] LifetimeDefinition def)
    {
      var old = Interlocked.Exchange(ref myCurrentDef, def);
      try
      {
        old.AllowTerminationUnderExecution = true;
        old.Terminate();
      }
      catch(Exception ex)
      {
        Log.Root.Error(ex);
      }

      return def;
    }
  }
}