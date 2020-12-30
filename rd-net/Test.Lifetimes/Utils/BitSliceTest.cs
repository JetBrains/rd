using System;
using System.Threading.Tasks;
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

#if !NET35
    [Test]
    public void TestBadEnums()
    {
      Assert.Throws<Assertion.AssertionException>(() => BitSlice.Enum<EMinus>());
      Assert.Throws<Assertion.AssertionException>(() => BitSlice.Enum<EUint>());
      
      Assert.Throws<Assertion.AssertionException>(() => BitSlice.Enum<E0>());
      Assert.Throws<TypeInitializationException>(() => BitSlice.Enum<ELong>());
    }
#endif
    

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
    
    [Test]
    public void InterlockedUpdateStressTest()
    {
      const int n = 10000;

      var slice1 = BitSlice.Bool();
      var slice2 = BitSlice.Bool(slice1);
      
      var state = 0;

      for (var i = 0; i < 500; i++)
      {
        Parallel.Invoke(() =>
        {
          for (var j = 0; j < n; j++)
          {
            Enter(slice1);
            Release(slice1);
          }
        }, () =>
        {
          for (var j = 0; j < n; j++)
          {
            Enter(slice2);
            Release(slice2);
          }
        });
      }

      void Enter(BoolBitSlice slice)
      {
        Assert.IsFalse(slice[state]);
        slice.InterlockedUpdate(ref state, true);
      }

      void Release(BoolBitSlice slice)
      {
        Assert.IsTrue(slice[state]);
        slice.InterlockedUpdate(ref state, false);
      } 
    }
    
    [Test]
    public void InterlockedUpdateStressTest2()
    {
      const int bitCount = 8;
      const int n = (1 << bitCount) - 1;

      var slice1 = BitSlice.Int(bitCount);
      var slice2 = BitSlice.Int(bitCount, slice1);
      var slice3 = BitSlice.Int(bitCount, slice2);
      var slice4 = BitSlice.Int(bitCount, slice3);
      

      for (var i = 0; i < 1000; i++)
      {
        var state = 0;
        Parallel.Invoke(() =>
        {
          for (var j = 0; j < n; j++) 
            slice1.InterlockedUpdate(ref state, slice1[state] + 1);
        }, () =>
        {
          for (var j = 0; j < n; j++) 
            slice2.InterlockedUpdate(ref state, slice2[state] + 1);
        },() =>
        {
          for (var j = 0; j < n; j++) 
            slice3.InterlockedUpdate(ref state, slice3[state] + 1);
        }, () =>
        {
          for (var j = 0; j < n; j++) 
            slice4.InterlockedUpdate(ref state, slice4[state] + 1);
        });
      
        Assert.AreEqual(n, slice1[state]);
        Assert.AreEqual(n, slice2[state]);
        Assert.AreEqual(n, slice3[state]);
        Assert.AreEqual(n, slice4[state]);
      }
    }
  }
}