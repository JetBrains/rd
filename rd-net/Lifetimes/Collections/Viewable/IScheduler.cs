using System;

namespace JetBrains.Collections.Viewable
{
  /// <summary>
  /// Abstraction of executor. Can be be UI thread, Pooled, synchronous and so on   
  /// </summary>
  public interface IScheduler
  {
    /// <summary>
    /// Queue action for execution. 
    /// </summary>
    /// <param name="action"></param>
    void Queue(Action action);
    
    /// <summary>
    /// Returns whether current task is being executed on this scheduler. Could be used for assert.
    /// </summary>
    bool IsActive { get; }
    
    /// <summary>
    /// Helps to relax expectations and speed up some usages if this scheduler
    /// can doesn't preserve sequential FIFO semantics (e.g. ThreadPool scheduler).
    /// </summary>
    bool OutOfOrderExecution { get; }
  }

  public interface IRunWhileScheduler : IScheduler
  {
    /// <summary>
    /// Pumps the scheduler while given condition is satisfied or until timeout elapses.
    /// </summary>
    /// <param name="condition">A delegate to be executed over and over while it returns true.</param>
    /// <param name="timeout">Maximum time to spend pumping. Use <see cref="TimeSpan.MaxValue"/> for no limit.</param>
    /// <returns>True if the condition was reached (condition returned false), false if timeout elapsed (when throwOnTimeout is false).</returns>
    bool RunWhile(Func<bool> condition, TimeSpan timeout);
  }
}