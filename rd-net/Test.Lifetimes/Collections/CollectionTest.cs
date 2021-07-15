using System;
using System.Collections.Generic;
using JetBrains.Collections;
using NUnit.Framework;

namespace Test.Lifetimes.Collections
{
  [TestFixture]
  public class CollectionTest : LifetimesTestBase
  {
    private class ComplexType
    {
      public int MagicNumber { get; }

      public ComplexType(int magicNumber) { MagicNumber = magicNumber; }

      public override int GetHashCode() { throw new InvalidOperationException("Use external comparer"); }
    }

    private sealed class ComplexTypeExternalEqualityComparer : IEqualityComparer<ComplexType>
    {
      public static IEqualityComparer<ComplexType> Instance { get; } = new ComplexTypeExternalEqualityComparer();

      public bool Equals(ComplexType x, ComplexType y)
      {
        if (ReferenceEquals(x, y)) return true;
        if (ReferenceEquals(x, null)) return false;
        if (ReferenceEquals(y, null)) return false;
        if (x.GetType() != y.GetType()) return false;
        return x.MagicNumber == y.MagicNumber;
      }

      public int GetHashCode(ComplexType obj) { return obj.MagicNumber; }
    }

    [Test]
    public void ContentHashCode01()
    {
      var set = new List<ComplexType>
      {
        new ComplexType(42),
        new ComplexType(666)
      };
      
      // ReSharper disable ReturnValueOfPureMethodIsNotUsed
      Assert.Throws<InvalidOperationException>(() => set.ContentHashCode());
      Assert.DoesNotThrow(() => set.ContentHashCode(ComplexTypeExternalEqualityComparer.Instance));
      // ReSharper restore ReturnValueOfPureMethodIsNotUsed
    }
  }
}