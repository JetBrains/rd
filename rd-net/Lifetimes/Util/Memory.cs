using System;
using System.Reflection;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Threading;

namespace JetBrains.Util.Internal
{
  public class Memory
  {
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static unsafe void CopyMemory(byte* src, byte* dest, int len)
    {

      
      if(len >= 0x10)
      {
        do
        {
          *((int*)dest) = *((int*)src);
          *((int*)(dest + 4)) = *((int*)(src + 4));
          *((int*)(dest + 8)) = *((int*)(src + 8));
          *((int*)(dest + 12)) = *((int*)(src + 12));
          dest += 0x10;
          src += 0x10;
        } while((len -= 0x10) >= 0x10);
      }
      if(len > 0)
      {
        if((len & 8) != 0)
        {
          *((int*)dest) = *((int*)src);
          *((int*)(dest + 4)) = *((int*)(src + 4));
          dest += 8;
          src += 8;
        }
        if((len & 4) != 0)
        {
          *((int*)dest) = *((int*)src);
          dest += 4;
          src += 4;
        }
        if((len & 2) != 0)
        {
          *((short*)dest) = *((short*)src);
          dest += 2;
          src += 2;
        }
        if((len & 1) != 0)
        {
          dest[0] = src[0];
          dest++;
          src++;
        }
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void Barrier()
    {
  Interlocked.MemoryBarrier();
    }
#nullable disable
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static T VolatileRead<T>(ref T location) where T : class
    {
      return Volatile.Read(ref location);
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void VolatileWrite<T>(ref T location, T value) where T : class
    {
      Volatile.Write(ref location, value);
    }
#nullable restore

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static int VolatileRead(ref int location)
    {
      return Volatile.Read(ref location);
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void VolatileWrite(ref int location, int value)
    {
      Volatile.Write(ref location, value);
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static bool VolatileRead(ref bool location)
    {
      return Volatile.Read(ref location);
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void VolatileWrite(ref bool location, bool value)
    {
      Volatile.Write(ref location, value);
    }

    public static bool IsReadWriteAtomic<T>()
    {
      return IsReadWriteAtomicCache<T>.IsReadWriteAtomic;
    }

    private static int MaxAtomicSize = ComputeMaxAtomicSize();
    private static int NonAtomicSize = MaxAtomicSize + 1;

    private static int ComputeMaxAtomicSize()
    {
      if (Environment.Is64BitOperatingSystem)
      {
        return sizeof(long);
      }
      
      return IntPtr.Size;
    }

    private static class IsReadWriteAtomicCache<T>
    {
      public static readonly bool IsReadWriteAtomic = Compute();

      private static bool Compute()
      {
        var size = GetSize();
        return size <= MaxAtomicSize;
      }

      private static int GetSize()
      {
#if NET5_0_OR_GREATER
        return Unsafe.SizeOf<T>();
#else
        try
        {
          return ComputeApproximateSize(typeof(T));
        }
        catch
        {
          // just in case something went wrong
          return NonAtomicSize;
        }
#endif
      }

      private static int ComputeApproximateSize(Type type)
      {
        if (!type.IsValueType)
        {
          return IntPtr.Size;
        }

        if (type.IsPrimitive || type.IsPointer)
        {
          return Marshal.SizeOf(type);
        }
        
        if (type.IsEnum)
        {
          var underlyingType = Enum.GetUnderlyingType(type);
          return Marshal.SizeOf(underlyingType);
        }

        var totalSize = 0;
        var types = type.GetRuntimeFields();
        foreach (var fieldInfo in types)
        {
          if (fieldInfo.IsStatic) continue;

          totalSize += ComputeApproximateSize(fieldInfo.FieldType);
          if (totalSize > MaxAtomicSize) return NonAtomicSize;
        }
        
        return totalSize;
      }
    }
  }
}