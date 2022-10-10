using System;
using System.Diagnostics;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics;

namespace JetBrains.Util.Util
{
  public class BitSlice
  {
    [PublicAPI] public int LoBit;
    [PublicAPI] public int BitCount;

    [PublicAPI] public int HiBit 
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get => LoBit + BitCount - 1;
    }

    [PublicAPI] public int Mask; //frequently used field, so it can't should be stored rather than calculated 

    public BitSlice(int loBit, int bitCount)
    {
      LoBit = loBit;
      BitCount = bitCount;
      Mask = (1 << BitCount) - 1;
    }

    
    #region Assertions

    [AssertionMethod]
    protected void AssertSliceFitsHostType<T>()
    {
      if (!Mode.IsAssertion) return;

      var type = typeof(T);      
      var maxBit = type == typeof(int) ? 31
        : type == typeof(long) ? 64
        : 0;
      
      Assertion.Assert(maxBit > 0, "Unsupported host type: {0}", type);      
      Assertion.Assert(HiBit <= maxBit, "{0} doesn't fit into host type {1}; must be inside [0, {2}]", this, type, maxBit);
    }

    [AssertionMethod]
    private void AssertValueFitsSlice(int value)
    {
      if (!Mode.IsAssertion) return;

      Assertion.Assert(value >= 0, "[{0}] must be >= 0; actual: {1}", nameof(value), value);
      Assertion.Assert(value <= Mask, "[{0}] must be <= {1} to fit {2}; actual: {3}", nameof(value), Mask, this, value);
    }
    
    #endregion


    #region For inheritance
        
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    protected int GetRaw(int host)
    {
      return (host >> LoBit) & Mask;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    protected int UpdatedRaw(int host, int value)
    {
      AssertValueFitsSlice(value);
      
      return (host & ((Mask << LoBit) ^ -1)) | (value << LoBit);
    }
    
    #endregion


    #region Static API

    private static int NextSliceLoBit(BitSlice? slice) => slice?.HiBit + 1 ?? 0; 
    
    public static IntBitSlice Int(int bitCount, BitSlice? previousSlice = null)
    {
      return new IntBitSlice(NextSliceLoBit(previousSlice), bitCount);
    }

    public static BoolBitSlice Bool(BitSlice? previousSlice = null)
    {
      return new BoolBitSlice(NextSliceLoBit(previousSlice), 1);
    }
    
    public static Enum32BitSlice<T> Enum<T>(BitSlice? previousSlice = null) where T  :
#if !NET35
    unmanaged, 
#endif
     Enum
    {
      return new Enum32BitSlice<T>(NextSliceLoBit(previousSlice));
    }
    

    #endregion
    
    
    public override string ToString() => $"BitSlice[{LoBit}, {HiBit}]";
  }

  
  public abstract class BitSlice<T> : BitSlice
  {
    protected BitSlice(int loBit, int bitCount) : base(loBit, bitCount) {}

    public abstract T this[int host] { get; }
    public abstract int Updated(int host, T value);

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public void InterlockedUpdate(ref int host, T value)
    {
      var s = host;
      while (true)
      {
        var originalValue = Interlocked.CompareExchange(ref host, Updated(s, value), s);
        if (originalValue == s) break;

        s = originalValue;
      }
    }
    
    public override string ToString() => $"TypedBitSlice<{typeof(T).Name}>[{LoBit}, {HiBit}]";
  }

  public sealed class IntBitSlice : BitSlice<int>
  {
    public IntBitSlice(int loBit, int bitCount) : base(loBit, bitCount)
    {
      AssertSliceFitsHostType<int>();
    }
        
    public override int this[int host]
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get => GetRaw(host);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]      
    public override int Updated(int host, int value) => UpdatedRaw(host, value);
  }
  
  public sealed class BoolBitSlice : BitSlice<bool>
  {
    public BoolBitSlice(int loBit, int bitCount) : base(loBit, bitCount)
    {
      AssertSliceFitsHostType<int>();
    }

    public override bool this[int host]
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]      
      get => GetRaw(host) != 0;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]      
    public override int Updated(int host, bool value) => UpdatedRaw(host, value ? 1 : 0);
  }
  
  public sealed class Enum32BitSlice<T> : BitSlice<T> where T :
#if !NET35
    unmanaged, 
#endif
    Enum
  {
    private static int CalculateBitCount()
    {
      var values = ((T[]) System.Enum.GetValues(typeof(T))).Select(Cast32BitEnum<T>.ToUInt).ToArray();
      
      Assertion.Require(values.Length > 0, "Bit slice for enum {0} with no values is meaningless", typeof(T));
      
      var max = values.Max();
      Assertion.Require(max <= int.MaxValue, "Values in enum must {0} must be in range [0..int.MaxValue]", typeof(T));
      
      var count = max + 1L;            
      var res = BitHacks.Log2Ceil(count);
      if (res == 0) res++; //for enums with one value

      return res;
    }

    public Enum32BitSlice(int loBit) : base(loBit, CalculateBitCount())
    {
      AssertSliceFitsHostType<int>();
    }

    public override T this[int host]
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get => Cast32BitEnum<T>.FromInt(GetRaw(host));
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]      
    public override int Updated(int host, T value) => UpdatedRaw(host, Cast32BitEnum<T>.ToInt(value));
  }

}