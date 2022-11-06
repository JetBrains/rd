using System.Threading;
using JetBrains.Lifetimes;

namespace JetBrains.Threading;

public readonly ref struct MutexCookie
{
  private readonly object myLocker;
  public readonly bool Success;

  public MutexCookie(Lifetime lifetime, object locker)
  {
    myLocker = locker;

    do
    {
      if (lifetime.IsNotAlive)
      {
        Success = false;
        return;
      }
    } while (!Monitor.TryEnter(locker, 50));

    Success = true;
  }

  public void Dispose()
  {
    if (Success)
      Monitor.Exit(myLocker);
  }
}