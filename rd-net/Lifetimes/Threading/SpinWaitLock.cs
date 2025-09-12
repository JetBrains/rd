using System;
using System.ComponentModel;
using System.Diagnostics;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Util;

namespace JetBrains.Threading
{
  [StructLayout(LayoutKind.Auto)]
  public struct SpinWaitLock
  {
    static SpinWaitLock() { CalcApprovedProcessorCount(); }

    public static int ApprovedProcessorCount
    {
      get { return _processorCount; }
    }

    public bool TryEnter()
    {
      return TryEnter(Thread.CurrentThread.GetHashCode());
    }

    public void Enter()
    {
      int currentThreadId = Thread.CurrentThread.GetHashCode();
      if (!TryEnter(currentThreadId))
      {
        if (_processorCount == 1)
        {
          do
          {
            Sleep();
          }
          while (!TryEnter(currentThreadId));
        }
        else
        {
          int iterations = 16;
          do
          {
            if (iterations > 256)
            {
              Sleep();
              iterations = 16;
            }
            else
            {
              Thread.SpinWait(iterations);
              iterations <<= 1;
            }
          } while (!TryEnter(currentThreadId));
        }
      }
    }

    public void Exit()
    {
      if (Interlocked.Decrement(ref _lockCount) == 0)
      {
        Interlocked.Exchange(ref _ownerThreadId, 0);
      }
    }

    private bool TryEnter(int currentThreadId)
    {
      int oldThreadId = Interlocked.CompareExchange(ref _ownerThreadId, currentThreadId, 0);
      bool result = oldThreadId == 0 || oldThreadId == currentThreadId;
      if (result)
      {
        Interlocked.Increment(ref _lockCount);
      }
      return result;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private static void CalcApprovedProcessorCount()
    {
      // TODO //_processorCount = (int)ProcessorUtil.GetProcessorCountWithAffinityMask();

      int procCount = 0;
      try
      {
        unchecked
        {
#pragma warning disable CA1416
          long am = Process.GetCurrentProcess().ProcessorAffinity.ToInt64();
#pragma warning restore CA1416
          while (am != 0)
          {
            ++procCount;
            am &= (am - 1);
          }
        }
      }
      catch (Win32Exception)
      {
        // Access is denied, or whatever else
      }
      catch (PlatformNotSupportedException)
      {
        // Thrown under .NET Core on macos and possibly other platforms
        procCount = Environment.ProcessorCount;
      }
      finally
      {
        if (procCount == 0)
        {
          procCount = 1;
        }
        _processorCount = procCount;
      }
    }

    private static void Sleep()
    {
      // The following code is not MT-safe, but that's actually doesn't matter, because we are just
      // trying to calc number of processors "rarely" in order to decrease its affect on performance.
      if ((++_sleepCount & 0xfff) == 0)
      {
        CalcApprovedProcessorCount();
      }

      Thread.Yield();
    }

    private int _ownerThreadId;
    private int _lockCount;
    private static int _processorCount = 1;
    private static int _sleepCount = 0;
  }

  [UsedImplicitly]
  public static class SpinWaitLockExtensions // Don't remove this, otherwise smb will write a new one
  {
    public struct SpinWaitLockCookie : IDisposable
    {
      public void Dispose()
      {
      }
    }

    // Don't remove this, otherwise smb will write a new one
    [Obsolete("It is not possible to use the using() pattern with the non-allocating SpinWaitLock because its lock status is cloned on copying. Either call paired methods, or use SpinWaitLockRef and allocate a heap object.")]
    [UsedImplicitly]
    public static SpinWaitLockCookie Acquire(this SpinWaitLock spinWaitLock)
    {
      return new SpinWaitLockCookie();
    }
  }
}