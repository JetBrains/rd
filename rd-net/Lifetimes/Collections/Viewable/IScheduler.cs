using System;

namespace JetBrains.Collections.Viewable
{
  public interface IScheduler
  {
    void Queue(Action action);
    bool IsActive { get; }
    bool OutOfOrderExecution { get; }
  }
}