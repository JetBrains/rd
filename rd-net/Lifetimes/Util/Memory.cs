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
#if (NET35)
  Thread.MemoryBarrier();
#else
  Interlocked.MemoryBarrier();
#endif
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static T VolatileRead<T>(ref T location) where T : class
    {
#if (NET35)
      var obj = location;
      Thread.MemoryBarrier();
      return obj;
#else
      return Volatile.Read(ref location);
#endif
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void VolatileWrite<T>(ref T location, T value) where T : class
    {
#if (NET35)
      Thread.MemoryBarrier();
      location = value;
#else
      Volatile.Write(ref location, value);
#endif
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static int VolatileRead(ref int location)
    {
#if (NET35)
      return Thread.VolatileRead(ref location);
#else
      return Volatile.Read(ref location);
#endif
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void VolatileWrite(ref int location, int value)
    {
#if (NET35)
      Thread.VolatileWrite(ref location, value);
#else
      Volatile.Write(ref location, value);
#endif
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static bool VolatileRead(ref bool location)
    {
#if (NET35)
      var obj = location;
      Thread.MemoryBarrier();
      return obj;
#else
      return Volatile.Read(ref location);
#endif
    }
    
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void VolatileWrite(ref bool location, bool value)
    {
#if (NET35)
      Thread.MemoryBarrier();
      location = value;
#else
      Volatile.Write(ref location, value);
#endif
    }
  }
}