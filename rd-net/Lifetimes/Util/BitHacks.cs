using System;
using System.Numerics;
using System.Runtime.CompilerServices;

namespace JetBrains.Util
{
  /// <summary>
  /// Inspired by http://graphics.stanford.edu/~seander/bithacks.html
  /// </summary>
  
  public static class BitHacks
  {
    // de Bruijn multiplication, see http://supertech.csail.mit.edu/papers/debruijn.pdf
    private const uint DeBruijnSequence32 = 0x07C4ACDD;
    private const ulong DeBruijnSequence64 = 0x03F79D71B4CB0A89;

    private static readonly byte[] ourDeBruijnBitTable32 = {
      0, 9, 1, 10, 13, 21, 2, 29,
      11, 14, 16, 18, 22, 25, 3, 30,
      8, 12, 20, 28, 15, 17, 24, 7,
      19, 27, 23, 6, 26, 5, 4, 31
    };

    private static readonly byte[] ourDeBruijnBitTable64 = {
      0, 47, 1, 56, 48, 27, 2, 60,
      57, 49, 41, 37, 28, 16, 3, 61,
      54, 58, 35, 52, 50, 42, 21, 44,
      38, 32, 29, 23, 17, 11, 4, 62,
      46, 55, 26, 59, 40, 36, 15, 53,
      34, 51, 20, 43, 31, 22, 10, 45,
      25, 39, 14, 33, 19, 30, 9, 24,
      13, 18, 8, 12, 7, 6, 5, 63
    };

    /// <summary>
    /// Returns the integer (floor) log of the specified value, base 2. 
    /// </summary>
    /// <returns><c>y : 2^y&lt;=x</c></returns>
    public static int Log2Floor(uint x)
    {
#if NET5_0_OR_GREATER
      return BitOperations.Log2(x);
#else
      return ReverseBitScan(x);
#endif
    }  
    
    /// <summary>
    /// Returns largest non-negative integer <c>y</c> such that <c>2^y&lt;=x</c> if <c>x&gt;0</c>, <c>0</c> if <c>x=0</c>, or throw ArgumentException if <c>x&lt;0</c> 
    /// </summary>
    /// <param name="x">Must be greater than or equal to zero.</param>
    /// <returns><c>y : 2^y&lt;=x</c></returns>
    public static int Log2Floor(int x)
    {
      if (x < 0) throw new ArgumentException("x must be greater than 0");
      return Log2Floor((uint)x);
    }

    /// <summary>
    /// Returns the integer (floor) log of the specified value, base 2.
    /// </summary>
    /// <param name="x">Must be greater than or equal to zero.</param>
    /// <returns><c>y : 2^y&lt;=x</c></returns>
    public static int Log2Floor(ulong x)
    {
#if NET5_0_OR_GREATER
      return BitOperations.Log2(x);
#else
      return ReverseBitScan(x);
#endif
    }    
    
    /// <summary>
    /// Returns largest non-negative integer <c>y</c> such that <c>2^y&lt;=x</c> if <c>x&gt;0</c>, <c>0</c> if <c>x=0</c>, or throw ArgumentException if <c>x&lt;0</c> 
    /// </summary>
    /// <param name="x">Must be greater than or equal to zero.</param>
    /// <returns><c>y : 2^y&lt;=x</c></returns>
    public static int Log2Floor(long x)
    {
      if (x < 0) throw new ArgumentException("x must be greater than 0");
      return Log2Floor((ulong)x);
    }

    
    /// <summary>
    /// Returns lowest non-negative integer <c>y</c> such that <c>2^y&gt;=x</c> if <c>x&gt;=0</c> or throw ArgumentException if <c>x&lt;0</c> 
    /// </summary>
    /// <param name="x">Must be greater than or equal to zero.</param>    
    /// <returns><c>y : 2^y&gt;=x</c></returns>    
    public static int Log2Ceil(int x)
    {      
      if (x < 0) throw new ArgumentException("x must be greater than 0");
      return Log2Ceil((uint)x);
    }
    
    /// <summary>Returns the integer (ceiling) log of the specified value, base 2.</summary>
    /// <param name="value">The value.</param>
    [MethodImpl(MethodImplOptions.AggressiveInlining)]
    public static int Log2Ceil(uint value)
    {
      if (value == 0) return 0;
#if NET5_0_OR_GREATER
      int result = Log2Floor(value);
      if (PopCount(value) != 1)
      {
        result++;
      }
      return result;
#else
      var log2Floor = ReverseBitScan(value);
      if (1U << log2Floor == value) return log2Floor;
      return log2Floor + 1;
#endif
    }

    /// <summary>
    /// Returns lowest non-negative integer <c>y</c> such that <c>2^y&gt;=x</c> if <c>x&gt;=0</c> or throw ArgumentException if <c>x&lt;0</c> 
    /// </summary>
    /// <param name="x">Must be greater than or equal to zero.</param>    
    /// <returns><c>y : 2^y&gt;=x</c></returns>    
    public static int Log2Ceil(long x)
    {      
      if (x < 0) throw new ArgumentException("x must be greater than 0");
      if (x == 0) return 0;
      var log2Floor = ReverseBitScan((ulong)x);
      if (1UL << log2Floor == (ulong)x) return log2Floor;
      return log2Floor + 1;
    }

    /// <summary>
    /// Return number of <c>1</c>-s in binary representation of <c>x</c>
    /// </summary>
    /// <param name="x"></param>
    /// <returns></returns>
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static int NumberOfBitSet(int x)
    {
#if NET5_0_OR_GREATER
      return BitOperations.PopCount((uint)x);
#else
      unchecked
      {
        x = x - ((x >> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >> 2) & 0x33333333); 
        return ((x + (x >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;
      }
#endif
    }  
    
    /// <summary>
    /// Return number of <c>1</c>-s in binary representation of <c>x</c>
    /// </summary>
    /// <param name="x"></param>
    /// <returns></returns>
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static int PopCount(uint x)
    {
#if NET5_0_OR_GREATER
      return BitOperations.PopCount(x);
#else
      unchecked
      {
        const uint c1 = 0x_55555555u;
        const uint c2 = 0x_33333333u;
        const uint c3 = 0x_0F0F0F0Fu;
        const uint c4 = 0x_01010101u;

        var value = x;

        value -= (value >> 1) & c1;
        value = (value & c2) + ((value >> 2) & c2);
        value = (((value + (value >> 4)) & c3) * c4) >> 24;

        return (int)value;
      }
#endif
    }  
    
    /// <summary>
    /// Return number of <c>1</c>-s in binary representation of <c>x</c>
    /// </summary>
    /// <param name="x"></param>
    /// <returns></returns>
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static int PopCount(ulong x)
    {
#if NET5_0_OR_GREATER
      return BitOperations.PopCount(x);
#else
      unchecked
      {
        const ulong c1 = 0x_55555555_55555555ul;
        const ulong c2 = 0x_33333333_33333333ul;
        const ulong c3 = 0x_0F0F0F0F_0F0F0F0Ful;
        const ulong c4 = 0x_01010101_01010101ul;

        var value = x;
        
        value -= (value >> 1) & c1;
        value = (value & c2) + ((value >> 2) & c2);
        value = (((value + (value >> 4)) & c3) * c4) >> 56;

        return (int)value;
      }
#endif
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private static int ReverseBitScan(uint value)
    {
      unchecked
      {
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        return ourDeBruijnBitTable32[(int) ((value * DeBruijnSequence32) >> 27)];
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    private static int ReverseBitScan(ulong value)
    {
      unchecked
      {
        value |= value >> 1;
        value |= value >> 2;
        value |= value >> 4;
        value |= value >> 8;
        value |= value >> 16;
        value |= value >> 32;
        return ourDeBruijnBitTable64[(int) ((value * DeBruijnSequence64) >> 58)];
      }
    }
  }
}