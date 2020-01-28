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
}