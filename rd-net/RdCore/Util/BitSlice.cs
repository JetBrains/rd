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
    private void AssertSliceFitsHostType<T>()
    {
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
      Assertion.Assert(value >= 0, "[{0}] must be >= 0; actual: {1}", nameof(value), value);
      Assertion.Assert(value <= Mask, "[{0}] must be <= {1} to fit {2}; actual: {3}", nameof(value), Mask, this, value);
    }
    
    #endregion


    #region For inheritance
        
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    protected int GetRaw(int host)
    {
//      AssertSliceFitsHostType<int>();
      
      return (host >> LoBit) & Mask;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    protected int UpdatedRaw(int host, int value)
    {
//      AssertSliceFitsHostType<int>();
//      AssertValueFitsSlice(value);
      
      return (host & ((Mask << LoBit) ^ -1)) | (value << LoBit);
    }
    
    #endregion


    #region Static API

    private static int NextSliceLoBit([CanBeNull] BitSlice slice) => slice?.HiBit + 1 ?? 0; 
    
    public static IntBitSlice Int(int bitCount, [CanBeNull] BitSlice previousSlice = null)
    {
      return new IntBitSlice(NextSliceLoBit(previousSlice), bitCount);
    }

    public static BoolBitSlice Bool([CanBeNull] BitSlice previousSlice = null)
    {
      return new BoolBitSlice(NextSliceLoBit(previousSlice), 1);
    }
    
    public static EnumBitSlice<T> Enum<T>([CanBeNull] BitSlice previousSlice = null) where T:struct
    {
      return new EnumBitSlice<T>(NextSliceLoBit(previousSlice));
    }
    

    #endregion
    
    
    public override string ToString() => $"BitSlice[{LoBit}, {HiBit}]";
  }

  
  public abstract class BitSlice<T> : BitSlice
  {
    protected BitSlice(int loBit, int bitCount) : base(loBit, bitCount) {}

    public abstract T this[int host] { get; }
    public abstract int Updated(int host, T value);

    public void InterlockedUpdate(ref int host, T value)
    {
      while (true)
      {
        var s = host;
        if (Interlocked.CompareExchange(ref host, Updated(host, value), s) == s)
        {
          break;
        }
      }
    }
    
    public override string ToString() => $"TypedBitSlice<{typeof(T).Name}>[{LoBit}, {HiBit}]";
  }

  public class IntBitSlice : BitSlice<int>
  {
    public IntBitSlice(int loBit, int bitCount) : base(loBit, bitCount){}
        
    public override int this[int host]
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get => GetRaw(host);
    }

    public override int Updated(int host, int value) => UpdatedRaw(host, value);
  }
  
  public class BoolBitSlice : BitSlice<bool>
  {
    public BoolBitSlice(int loBit, int bitCount) : base(loBit, bitCount){}
    public override bool this[int host]
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]      
      get => GetRaw(host) != 0;
    }

    public override int Updated(int host, bool value) => UpdatedRaw(host, value ? 1 : 0);
  }
  
  public class EnumBitSlice<T> : BitSlice<T> where T:struct
  {
    private static int CalculateBitCount()
    {
      var type = typeof(T);
#if !NETSTANDARD
      Assertion.Require(type.IsEnum, "Type must be enum, actual type: {0}", type);
#endif

      var values = ((T[]) System.Enum.GetValues(typeof(T))).Select(CastTo<long>.From).ToList();
      
      Assertion.Require(values.Count > 0, "Bit slice for enum with no value is meaningless");
      Assertion.Require(values.All(v => v >= 0), "Enums with negative values or values greater than long.MaxValue are unsupported");

      var count = values.Max() + 1;            
      var res = BitHacks.Log2Ceil(count);
      if (res == 0) res++; //for enums with one value

      return res;
    }
    
    public EnumBitSlice(int loBit) : base(loBit, CalculateBitCount()){}
    public override T this[int host]
    {
      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      get => CastTo<T>.From(GetRaw(host));
    }

    public override int Updated(int host, T value) => UpdatedRaw(host, CastTo<int>.From(value));
  }

}