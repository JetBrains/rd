using System;
using JetBrains.Util.Internal;
using NUnit.Framework;

namespace Test.Lifetimes.Utils
{
  public class MemoryTest
  {
    #region TestTypes
    private enum ByteEnum : byte { Value = 1 }
    private enum IntEnum : int { Value = 1 }
    private enum UIntEnum : int { Value = 1 }
    private enum LongEnum : long { Value = 1 }
    private enum ULongEnum : long { Value = 1 }

    private struct UserDefinedStruct
    {
      public int Value;
    }
    #endregion

    [TestCase(typeof(string))]
    [TestCase(typeof(object))]
    public void IsReadWriteAtomic_ReferenceTypes_ReturnsTrue(Type type)
    {
      Assert.IsTrue(IsReadWriteAtomic(type));
    }

    [TestCase(typeof(bool))]
    [TestCase(typeof(byte))]
    [TestCase(typeof(sbyte))]
    [TestCase(typeof(char))]
    [TestCase(typeof(short))]
    [TestCase(typeof(ushort))]
    [TestCase(typeof(int))]
    [TestCase(typeof(uint))]
    [TestCase(typeof(float))]
    [TestCase(typeof(IntPtr))]
    [TestCase(typeof(UIntPtr))]
    public void IsReadWriteAtomic_PrimitiveTypes_ReturnsTrue(Type type)
    {
      Assert.IsTrue(IsReadWriteAtomic(type));
    }

    [TestCase(typeof(long))]
    [TestCase(typeof(ulong))]
    [TestCase(typeof(double))]
    public void IsReadWriteAtomic_LargePrimitiveTypes_DependsOnArchitecture(Type type)
    {
      Assert.AreEqual(IsReadWriteAtomic(type), IntPtr.Size == 8);
    }

    [TestCase(typeof(ByteEnum))]
    [TestCase(typeof(IntEnum))]
    [TestCase(typeof(UIntEnum))]
    public void IsReadWriteAtomic_Enums_ReturnsTrue(Type type)
    {
      Assert.IsTrue(IsReadWriteAtomic(type));
    }

    [TestCase(typeof(LongEnum))]
    [TestCase(typeof(ULongEnum))]
    public void IsReadWriteAtomic_LargeEnums_DependsOnArchitecture(Type type)
    {
      Assert.AreEqual(IsReadWriteAtomic(type), IntPtr.Size == 8);
    }

    [TestCase(typeof(DateTime))]
    [TestCase(typeof(decimal))]
    [TestCase(typeof(UserDefinedStruct))]
    public void IsReadWriteAtomic_UserDefinedStructs_ReturnsFalse(Type type)
    {
      Assert.IsFalse(IsReadWriteAtomic(type));
    }

    private static bool IsReadWriteAtomic(Type type)
    {
      var canon = typeof(Memory).GetMethod(nameof(Memory.IsReadWriteAtomic));
      var generic = canon!.MakeGenericMethod(type);
      return (bool)generic.Invoke(null,null);
    }
  }
}
