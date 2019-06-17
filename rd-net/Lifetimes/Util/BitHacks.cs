using System;

namespace JetBrains.Util
{
  /// <summary>
  /// Inspired by http://graphics.stanford.edu/~seander/bithacks.html
  /// </summary>
  
  public static class BitHacks
  {
    private static readonly int[] ourLogFloor2Lookup = new int[256];

    static BitHacks()
    {
      ourLogFloor2Lookup[0] = 0;

      for (int i = 0; i < 8; i++)
      {
        for (int j = 1 << i; j < 1 << (i + 1); j++)
          ourLogFloor2Lookup[j] = i;
      }
    }

    /// <summary>
    /// Returns largest non-negative integer <c>y</c> such that <c>2^y&lt;=x</c> if <c>x&gt;0</c>, <c>0</c> if <c>x=0</c>, or throw ArgumentException if <c>x&lt;0</c> 
    /// </summary>
    /// <param name="x">Must be greater than or equal to zero.</param>
    /// <returns><c>y : 2^y&lt;=x</c></returns>
    public static int Log2Floor(int x)
    {
      if (x < 0) throw new ArgumentException("x must be greater than 0");
      
      if (x >= 1 << 16)
      {
        if (x >= 1 << 24) 
          return 24 + ourLogFloor2Lookup[x >> 24];
        else 
          return 16 + ourLogFloor2Lookup[x >> 16];
      }
      else
      {
        if (x >= 1 << 8) 
          return 8 + ourLogFloor2Lookup[x >> 8];
        else 
          return ourLogFloor2Lookup[x];
      }
    }

    /// <summary>
    /// Returns lowest non-negative integer <c>y</c> such that <c>2^y&gt;=x</c> if <c>x&gt;=0</c> or throw ArgumentException if <c>x&lt;0</c> 
    /// </summary>
    /// <param name="x">Must be greater than or equal to zero.</param>    
    /// <returns><c>y : 2^y&gt;=x</c></returns>    
    public static int Log2Ceil(long x)
    {      
      if (x < 0) throw new ArgumentException("x must be greater than 0");
      
      //Current implementation is suboptimal. Make it faster if you need more performance.
      for (int i=0; i<62; i++)
        if (1 << i >= x)
          return i;
      
      return 63;
    }

    /// <summary>
    /// Return number of <c>1</c>-s in binary representation of <c>x</c>
    /// </summary>
    /// <param name="x"></param>
    /// <returns></returns>
    public static int NumberOfBitSet(int x)
    {
      unchecked
      {
        x = x - ((x >> 1) & 0x55555555);
        x = (x & 0x33333333) + ((x >> 2) & 0x33333333); 
        return ((x + (x >> 4) & 0xF0F0F0F) * 0x1010101) >> 24;
      }
    }
  }
}