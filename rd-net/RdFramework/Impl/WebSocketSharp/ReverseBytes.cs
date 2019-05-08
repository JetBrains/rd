using System;

namespace JetBrains.Rd.Impl.WebSocketSharp
{
  public static class ReverseBytes
  {
    public static UInt16 Of(UInt16 value)
    {
      return (UInt16)(
        (value & 0xFFU) << 8 | 
        (value & 0xFF00U) >> 8);
    }

    public static UInt32 Of(UInt32 value)
    {
      return (value & 0x000000FFU) << 24 | 
             (value & 0x0000FF00U) << 8 |
             (value & 0x00FF0000U) >> 8 | 
             (value & 0xFF000000U) >> 24;
    }

    public static UInt64 Of(UInt64 value)
    {
      return (value & 0x00000000000000FFUL) << 56 | 
             (value & 0x000000000000FF00UL) << 40 |
             (value & 0x0000000000FF0000UL) << 24 | 
             (value & 0x00000000FF000000UL) << 8 |
             (value & 0x000000FF00000000UL) >> 8 | 
             (value & 0x0000FF0000000000UL) >> 24 |
             (value & 0x00FF000000000000UL) >> 40 | 
             (value & 0xFF00000000000000UL) >> 56;
    }
  }
}