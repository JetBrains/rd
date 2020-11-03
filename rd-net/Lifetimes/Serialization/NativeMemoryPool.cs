using System;
using System.IO;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Util;

namespace JetBrains.Serialization
{
  public static class NativeMemoryPool
  {
    public const int AllocSize = 1 << 20;
    public const int MaxAllocSize = 1 << 30;

    private const string LogCategory = nameof(NativeMemoryPool);

    [ThreadStatic] private static ThreadMemoryHolder ourThreadMemory;

    private static ThreadMemoryHolder[] ourBlocks = new ThreadMemoryHolder[Environment.ProcessorCount];
    private static readonly object ourLock = new object();

    public static int SampleUsed() => ourBlocks.Count(b => b != null && b.IsUsed);

    public static int SampleCount() => ourBlocks.Count(b => b != null);

    /// <summary>
    /// Reserve an unmanaged block of memory size of <see cref="Cookie.Length"/>.
    /// You use <see cref="Cookie.Data"/> to obtain pointer. Always call Dispose on provided cookie to avoid memory leaks.
    /// </summary>
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static Cookie Reserve()
    {
      // try to reuse the block which was used previous time by the current thread
      if (ourThreadMemory != null)
      {
        if (ourThreadMemory.TryReserve())
          return new Cookie(ourThreadMemory);
      }

      return ReserveMiss();
    }

    /// <summary>
    /// Tries to free one of the available pre-allocated native blocks.
    /// </summary>
    /// <returns>true if the block was deallocated, false if there are no such blocks.</returns>
    public static bool TryFreeMemory()
    {
      lock (ourLock)
      {
        for (int i = ourBlocks.Length - 1; i >= 0; i--)
        {
          var block = ourBlocks[i];
          if (block != null && block.TryReserve())
          {
            block.Free();
            block.Dispose();

            // make a hole
            ourBlocks[i] = null;
            // try to fill the hole. ourBlocks array should be filled from the start
            for (int j = ourBlocks.Length - 1; j > i; j--)
            {
              if (ourBlocks[j] != null)
              {
                ourBlocks[i] = ourBlocks[j];
                ourBlocks[j] = null;
                return true;
              }
            }
            return true;
          }
        }
      }

      return false;
    }

    private static Cookie ReserveMiss()
    {
      // try to search in the array of all memory blocks
      ThreadMemoryHolder h;
      if (SearchAndReserve(out h))
        return new Cookie(h);
      if (AllocateNew(out h))
        return new Cookie(h);

      // All items in array blocks are busy and not available, increase the array size and try once more
      lock (ourLock)
      {
        var newBlocks = new ThreadMemoryHolder[ourBlocks.Length * 2];
        Array.Copy(ourBlocks, newBlocks, ourBlocks.Length);
        ourBlocks = newBlocks;

        if (SearchAndReserve(out h))
          return new Cookie(h);
        if (AllocateNew(out h))
          return new Cookie(h);
      }

      Assertion.Fail("Unable to reserve a native memory block");
      return new Cookie();


      bool SearchAndReserve(out ThreadMemoryHolder holder)
      {
        var blocks = ourBlocks;
        var start = blocks.Length % Thread.CurrentThread.ManagedThreadId;
        for (int i = start; i < blocks.Length; i++)
        {
          holder = blocks[i];
          if (holder == null)
            break;
          if (holder.TryReserve())
          {
            ourThreadMemory = holder;
            return true;
          }
        }
        for (int i = 0; i < start; i++)
        {
          holder = blocks[i];
          if (holder == null)
            break;
          if (holder.TryReserve())
          {
            ourThreadMemory = holder;
            return true;
          }
        }

        holder = null;
        return false;
      }

      bool AllocateNew(out ThreadMemoryHolder holder)
      {
        lock (ourLock)
        {
          for (int i = 0; i < ourBlocks.Length; i++)
          {
            if (ourBlocks[i] != null)
              continue;

            ourThreadMemory = holder = new ThreadMemoryHolder();
            if (holder.TryReserve())
            {
              ourBlocks[i] = holder;
              return true;
            }

            Assertion.Fail("Unable to reserve newly created memory block");
          }
        }

        holder = null;
        return false;
      }
    }

    private static IntPtr AllocateMemory(int allocSize)
    {
      //can't use ILog.Verbose here because logger is closed to UnsafeWriter
      LogLog.Verbose(LogCategory, "New memory block allocated, size: {0:N0} bytes, total allocated blocks: {1}, used: {2}", allocSize, SampleCount(), SampleUsed());
      var pointer = Marshal.AllocHGlobal(allocSize);
      if (pointer == IntPtr.Zero)
        ErrorOomOldMono();
      return pointer;
    }

    private static void ErrorOomOldMono()
    {
      throw new Exception("Allocator returned NULLPTR. Usually this happens in case of OOM on Mono Runtime.");
    }

    public readonly struct Cookie : IDisposable
    {
      private readonly ThreadMemoryHolder myHolder;
      public bool IsValid => myHolder != null;

      public IntPtr Data => myHolder.Data;
      public int Length => myHolder.Length;

      internal IntPtr Realloc(int size)
      {
        return myHolder.Realloc(size);
      }

      internal Cookie([NotNull] ThreadMemoryHolder holder)
      {
        myHolder = holder ?? throw new ArgumentNullException(nameof(holder));
      }

      public void Dispose()
      {
        myHolder.Free();
      }
    }

    internal class ThreadMemoryHolder : IDisposable
    {
      private const int Unused = 0;
      private const int Used = 1;
      private const int Disposed = 2;
      private IntPtr myPtr;

      private int myUse;
      public IntPtr Data => myPtr;

      [ThreadStatic]
      internal static int AllocatedOnThread;

      public bool IsUsed => myUse == Used;
      public bool IsDisposed => myUse == Disposed;

      public int Length { get; private set; }

      public ThreadMemoryHolder()
      {
        AllocatedOnThread++;
        Assertion.Assert(myPtr == IntPtr.Zero, "myPtr == IntPtr.Zero");
        myPtr = AllocateMemory(AllocSize);
        Length = AllocSize;
      }

      public bool TryReserve()
      {
        if (myUse == Used)
          return false;

        if (Interlocked.CompareExchange(ref myUse, Used, Unused) == Unused)
          return myPtr != IntPtr.Zero;

        return false;
      }

      public void Free()
      {
        if (Length < AllocSize)
        {
          LogLog.Error("Invalid attempt to release slot with too small chunk of memory");
          Realloc(AllocSize);
        }

        if (Interlocked.CompareExchange(ref myUse, Unused, Used) != Used)
          Assertion.Fail("Invalid attempt to free unused or disposed slot");
      }

      public IntPtr Realloc(int size)
      {
        if (size > MaxAllocSize)
          throw new ArgumentException($"Can't allocate more memory: {size:N0} bytes, max: {MaxAllocSize:N0}");

        myPtr = Marshal.ReAllocHGlobal(myPtr, new IntPtr(size));
        if (myPtr == null)
          ErrorOomOldMono();
        Length = size;
        return myPtr;
      }

      private void ReleaseUnmanagedResources()
      {
        var ptr = myPtr;
        if (Interlocked.CompareExchange(ref myPtr, IntPtr.Zero, ptr) == ptr)
        {
          Marshal.FreeHGlobal(ptr);
          myUse = Disposed;
        }
      }

      public void Dispose()
      {
        ReleaseUnmanagedResources();
        GC.SuppressFinalize(this);
      }

      ~ThreadMemoryHolder()
      {
        ReleaseUnmanagedResources();
      }
    }
  }
}