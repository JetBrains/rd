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
    private LifetimeDefinition myCurrentDef = LifetimeDefinition.Terminated;

    /// <summary>Creates and binds to the lifetime.</summary>
    /// <param name="lifetime">When this lifetime is closed, the last of the sequential lifetimes is closed too.</param>
    public SequentialLifetimes(Lifetime lifetime)
    {
      myParentLifetime = lifetime;
    }

    /// <summary>
    /// Terminates current lifetime and starts new.
    /// </summary>
    /// <returns>New lifetime. Note, In case of a race condition this lifetime might be terminated.</returns>
    public Lifetime Next()
    {
      TerminateCurrent();
      var next = new LifetimeDefinition(myParentLifetime);
      return TrySetNewAndTerminateOld(next).Lifetime;
    }

    /// <summary>
    /// Terminates the current lifetime, calls your handler with the new lifetime and tries to set it as current.
    /// Similar to <see cref="DefineNext"/>
    /// </summary>
    public void Next(Action<Lifetime> atomicAction)
    {
      if (atomicAction == null) throw new ArgumentNullException(nameof(atomicAction));
      DefineNext(lifetimeDefinition => atomicAction(lifetimeDefinition.Lifetime));
    }

    /// <summary>
    /// Terminates the current lifetime, calls your handler with the new lifetime and tries to set it as current.
    /// Similar to <see cref="Next(System.Action{JetBrains.Lifetimes.Lifetime})"/>
    /// </summary>
    public void DefineNext(Action<LifetimeDefinition> atomicAction)
    {
      if (atomicAction == null) throw new ArgumentNullException(nameof(atomicAction));
      
      TerminateCurrent();
      var next = new LifetimeDefinition(myParentLifetime);
      TrySetNewAndTerminateOld(next, definition => definition.ExecuteOrTerminateOnFail(atomicAction));
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
      TrySetNewAndTerminateOld(LifetimeDefinition.Terminated);
    }

    /// For sequential usage (say single-threaded or actor-like) usage only.
    public bool IsCurrentTerminated => myCurrentDef.Status == LifetimeStatus.Terminated;

    /// For sequential usage (say single-threaded or actor-like) usage only.
    public bool IsCurrentAlive => myCurrentDef.Status == LifetimeStatus.Alive;

    /// For sequential usage (say single-threaded or actor-like) usage only.
    public LifetimeStatus CurrentStatus => myCurrentDef.Status;


    /// <summary>
    /// Atomically, assigns the new lifetime and terminates the old one.
    /// In case of a race condition when current lifetime is overwritten new lifetime is terminated
    /// </summary>
    /// <param name="newLifetimeDefinition">New lifetime definition to set</param>
    /// <param name="actionWithNewLifetime">Action to perform once new lifetime is set</param>
    /// <returns>New lifetime definition which can be terminated in case of a race condition</returns>
    private LifetimeDefinition TrySetNewAndTerminateOld(LifetimeDefinition newLifetimeDefinition, Action<LifetimeDefinition>? actionWithNewLifetime = null)
    {
      void TerminateLifetimeDefinition(LifetimeDefinition lifetimeDefinition)
      {
        lifetimeDefinition.AllowTerminationUnderExecution = true;
        lifetimeDefinition.Terminate();
      }

      // temporary lifetime definition to cope with race condition
      var tempLifetimeDefinition = new LifetimeDefinition(myParentLifetime);
      // the lifetime needs to be terminated but LifetimeDefinition.Terminated cannot be used as we will use Interlocked.CompareExchange
      tempLifetimeDefinition.Terminate();

      var old = Interlocked.Exchange(ref myCurrentDef, tempLifetimeDefinition);
      try
      {
        TerminateLifetimeDefinition(old);
      }
      catch (Exception e)
      {
        Log.Root.Error(e);
      }

      try
      {
        actionWithNewLifetime?.Invoke(newLifetimeDefinition);
      }
      finally
      {
        if (Interlocked.CompareExchange(ref myCurrentDef, newLifetimeDefinition, tempLifetimeDefinition) != tempLifetimeDefinition)
        {
          TerminateLifetimeDefinition(newLifetimeDefinition);
        }
      }

      return newLifetimeDefinition;
    }
  }
}