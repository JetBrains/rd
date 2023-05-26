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
  }
}
