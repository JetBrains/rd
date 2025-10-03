using System;
using JetBrains.Util;
using NUnit.Framework;

namespace Test.Lifetimes.Utils
{
  public class BitHacksTest
  {
    [TestCase(0, 0)]
    [TestCase(1, 0)]
    [TestCase(2, 1)]
    [TestCase(3, 1)]
    [TestCase(4, 2)]
    [TestCase(5, 2)]
    [TestCase(7, 2)]
    [TestCase(8, 3)]
    [TestCase(9, 3)]
    [TestCase(15, 3)]
    [TestCase(16, 4)]
    [TestCase(byte.MaxValue, 7)]
    [TestCase(ushort.MaxValue, 15)]
    public static void Log2FloorInt32(int n, int expected)
    {
      int actual = BitHacks.Log2Floor(n);
      Assert.AreEqual(expected, actual);
    }

    [TestCase(0, 0)]
    [TestCase(1, 0)]
    [TestCase(2, 1)]
    [TestCase(3, 1)]
    [TestCase(4, 2)]
    [TestCase(5, 2)]
    [TestCase(7, 2)]
    [TestCase(8, 3)]
    [TestCase(9, 3)]
    [TestCase(15, 3)]
    [TestCase(16, 4)]
    [TestCase(byte.MaxValue, 7)]
    [TestCase(ushort.MaxValue, 15)]
    [TestCase(uint.MaxValue, 31)]
    public static void Log2FloorInt64(long n, int expected)
    {
      int actual = BitHacks.Log2Floor(n);
      Assert.AreEqual(expected, actual);
    }

    [TestCase(0, 0)]
    [TestCase(1, 0)]
    [TestCase(2, 1)]
    [TestCase(3, 2)]
    [TestCase(4, 2)]
    [TestCase(5, 3)]
    [TestCase(7, 3)]
    [TestCase(8, 3)]
    [TestCase(9, 4)]
    [TestCase(15, 4)]
    [TestCase(16, 4)]
    [TestCase(byte.MaxValue, 8)]
    [TestCase(ushort.MaxValue, 16)]
    public static void Log2CeilInt32(int n, int expected)
    {
      int actual = BitHacks.Log2Ceil(n);
      Assert.AreEqual(expected, actual);
    }

    [TestCase(0, 0)]
    [TestCase(1, 0)]
    [TestCase(2, 1)]
    [TestCase(3, 2)]
    [TestCase(4, 2)]
    [TestCase(5, 3)]
    [TestCase(7, 3)]
    [TestCase(8, 3)]
    [TestCase(9, 4)]
    [TestCase(15, 4)]
    [TestCase(16, 4)]
    [TestCase(byte.MaxValue, 8)]
    [TestCase(ushort.MaxValue, 16)]
    [TestCase(uint.MaxValue, 32)]
    public static void Log2CeilInt64(long n, int expected)
    {
      int actual = BitHacks.Log2Ceil(n);
      Assert.AreEqual(expected, actual);
    }

    [Test]
    public static void ThrowsOnNegativeArgument()
    {
      Assert.Throws<ArgumentException>(() => BitHacks.Log2Floor(-1));
      Assert.Throws<ArgumentException>(() => BitHacks.Log2Floor(-1L));
      Assert.Throws<ArgumentException>(() => BitHacks.Log2Ceil(-1));
      Assert.Throws<ArgumentException>(() => BitHacks.Log2Ceil(-1L));
    }

    [Test]
    public static void NumberOfBitSet_Int32()
    {
      Assert.AreEqual(0, BitHacks.NumberOfBitSet(0));
      Assert.AreEqual(1, BitHacks.NumberOfBitSet(1));
      Assert.AreEqual(1, BitHacks.NumberOfBitSet(2));
      Assert.AreEqual(2, BitHacks.NumberOfBitSet(3));
      Assert.AreEqual(4, BitHacks.NumberOfBitSet(0b_1111));
      Assert.AreEqual(16, BitHacks.NumberOfBitSet(0x0F0F0F0F)); // 00001111 pattern repeated
      Assert.AreEqual(32, BitHacks.NumberOfBitSet(-1));
      Assert.AreEqual(1, BitHacks.NumberOfBitSet(int.MinValue));
    }

    [Test]
    public static void PopCount_UInt32()
    {
      Assert.AreEqual(0, BitHacks.PopCount(0u));
      Assert.AreEqual(1, BitHacks.PopCount(1u));
      Assert.AreEqual(1, BitHacks.PopCount(2u));
      Assert.AreEqual(2, BitHacks.PopCount(3u));
      Assert.AreEqual(16, BitHacks.PopCount(0xF0F0F0F0u));
      Assert.AreEqual(32, BitHacks.PopCount(uint.MaxValue));
    }

    [Test]
    public static void PopCount_UInt64()
    {
      Assert.AreEqual(0, BitHacks.PopCount(0ul));
      Assert.AreEqual(1, BitHacks.PopCount(1ul));
      Assert.AreEqual(1, BitHacks.PopCount(2ul));
      Assert.AreEqual(2, BitHacks.PopCount(3ul));
      Assert.AreEqual(32, BitHacks.PopCount(0xF0F0F0F0F0F0F0F0ul));
      Assert.AreEqual(64, BitHacks.PopCount(ulong.MaxValue));
    }

    [Test]
    public static void Log2Floor_UInt64_specific()
    {
      Assert.AreEqual(0, BitHacks.Log2Floor(0ul));
      Assert.AreEqual(0, BitHacks.Log2Floor(1ul));
      Assert.AreEqual(1, BitHacks.Log2Floor(2ul));
      Assert.AreEqual(63, BitHacks.Log2Floor(1ul << 63));
      Assert.AreEqual(63, BitHacks.Log2Floor(ulong.MaxValue));
    }

    [Test]
    public static void Log2Ceil_UInt32_specific()
    {
      Assert.AreEqual(0, BitHacks.Log2Ceil(0u));
      Assert.AreEqual(0, BitHacks.Log2Ceil(1u));
      Assert.AreEqual(1, BitHacks.Log2Ceil(2u));
      Assert.AreEqual(2, BitHacks.Log2Ceil(3u));
      Assert.AreEqual(32, BitHacks.Log2Ceil(uint.MaxValue));
    }
  }
}
