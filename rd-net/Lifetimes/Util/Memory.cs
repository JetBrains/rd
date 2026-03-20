using System;
using System.Runtime.CompilerServices;
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

    private static class IsReadWriteAtomicCache<T>
    {
      // ReSharper disable once StaticMemberInGenericType
      public static readonly bool IsReadWriteAtomic = Compute();

      private static bool Compute()
      {
        //According to runtime specification
        //https://github.com/dotnet/runtime/blob/main/docs/design/specs/Memory-model.md#atomic-memory-accesses
        //Managed references accesses are atomic
        if (!typeof(T).IsValueType)
          return true;
        //Otherwise, only primitive and Enum types with size up to the platform pointer size accesses are atomic
        if (!typeof(T).IsPrimitive && !typeof(T).IsEnum)
          return false;

        //Native integer primitive types
        if (typeof(T) == typeof(IntPtr) || typeof(T) == typeof(UIntPtr))
          return true;

        //Other primitive types and Enum types (via underlying primitive type)
        switch (Type.GetTypeCode(typeof(T)))
        {
          case TypeCode.Boolean:
          case TypeCode.SByte:
          case TypeCode.Byte:
          case TypeCode.Char:
          case TypeCode.Int16:
          case TypeCode.UInt16:
          case TypeCode.Int32:
          case TypeCode.UInt32:
          case TypeCode.Single:
            return true;
          case TypeCode.Int64:
          case TypeCode.UInt64:
          case TypeCode.Double:
            return IntPtr.Size == 8;
          default:
            return false;
        }
      }
    }
  }
}