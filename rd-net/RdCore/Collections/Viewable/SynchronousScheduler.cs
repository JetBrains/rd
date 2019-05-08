using System;
using JetBrains.Lifetimes;

namespace JetBrains.Collections.Viewable
{
  public class SynchronousScheduler : IScheduler
  {
    public static SynchronousScheduler Instance = new SynchronousScheduler();

    [ThreadStatic] private static int ourActive;

    public void SetActive(Lifetime lifetime)
    {      
      lifetime.Bracket(() => { ourActive++; }, () => { ourActive--; });
    }

    private SynchronousScheduler(){}

    public void Queue(Action action)
    {
      try
      {
        ourActive ++;
        action();
      }
      finally
      {
        ourActive--;
      }
    }

    public bool IsActive { get { return ourActive > 0; } }
    public bool OutOfOrderExecution { get { return false; } }
  }
}