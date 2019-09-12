using System;
using JetBrains.Diagnostics;
using JetBrains.Util.Util;
using NUnit.Framework;

namespace Test.Lifetimes.Utils
{
  public class BitSliceTest
  {
    private enum E0 {}
    private enum EMinus { Single = -1}
    private enum EUint : uint { Single = uint.MaxValue}
    private enum ELong : long { Single = long.MaxValue}
    
    private enum E1 { Single }
    private enum E4 : uint { Zero, One, Two, Three }
    
    private BitSlice<int> mySliceInt;
    private BitSlice<bool> mySliceBool;
    private BitSlice<E4> mySliceEnum;
    private BitSlice<E1> mySliceE1;

    [SetUp]
    public void BeforeClass()
    {
      mySliceInt = BitSlice.Int(4);
      mySliceEnum = BitSlice.Enum<E4>(mySliceInt);
      mySliceBool = BitSlice.Bool(mySliceEnum);
      mySliceE1 = BitSlice.Enum<E1>(mySliceBool);
    }

    [Test]
    public void TestBadEnums()
    {
      Assert.Throws<Assertion.AssertionException>(() => BitSlice.Enum<EMinus>());
      Assert.Throws<Assertion.AssertionException>(() => BitSlice.Enum<EUint>());
      
      Assert.Throws<Assertion.AssertionException>(() => BitSlice.Enum<E0>());
      Assert.Throws<TypeInitializationException>(() => BitSlice.Enum<ELong>());
    }
    

    [Test]
    public void TestZero()
    {
      var x = 0;
      Assert.AreEqual(0, mySliceInt[x]);
      Assert.AreEqual(E4.Zero, mySliceEnum[x]);
      Assert.AreEqual(false, mySliceBool[x]);
    }

    [Test]
    public void Test15()
    {
      var x = 15;
      Assert.AreEqual(15, mySliceInt[x]);
      Assert.AreEqual(E4.Zero, mySliceEnum[x]);
      Assert.AreEqual(false, mySliceBool[x]);
    }
    
    [Test]
    public void Test16()
    {
      var x = 16;
      Assert.AreEqual(0, mySliceInt[x]);
      Assert.AreEqual(E4.One, mySliceEnum[x]);
      Assert.AreEqual(false, mySliceBool[x]);
    }
    
    [Test]
    public void TestUpdate()
    {
      var x = 0;

//      Assert.Throws<Assertion.AssertionException>(() => mySliceInt.Updated(x, 16));

      x = mySliceInt.Updated(x, 10);
      x = mySliceEnum.Updated(x, E4.Two);
      x = mySliceBool.Updated(x, true);
      
      Assert.AreEqual(10, mySliceInt[x]);
      Assert.AreEqual(E4.Two, mySliceEnum[x]);
      Assert.AreEqual(true, mySliceBool[x]);
    }
  }
}