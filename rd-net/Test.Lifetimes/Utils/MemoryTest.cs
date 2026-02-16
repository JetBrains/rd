using System;
using JetBrains.Util.Internal;
using NUnit.Framework;

namespace Test.Lifetimes.Utils
{
  public class MemoryTest
  {
    // Helper to determine expected atomic size threshold
    // This matches the logic in Memory.ComputeMaxAtomicSize()
    private static int MaxAtomicSize
    {
      get
      {
        if (Environment.Is64BitOperatingSystem)
        {
          return sizeof(long);
        }
        return IntPtr.Size;
      }
    }

    #region Test Types

    private enum ByteEnum : byte { Value = 1 }
    private enum IntEnum : int { Value = 1 }
    private enum LongEnum : long { Value = 1 }

    private struct SmallStruct
    {
      public int Value;
    }

    private struct TwoIntStruct
    {
      public int A;
      public int B;
    }

    private struct LargeStruct
    {
      public long A;
      public long B;
      public long C;
    }

    private struct StructWithReference
    {
      public object Ref;
      public int Value;
    }

    private struct StructWithString
    {
      public string Text;
      public int Length;
    }

    private struct StructWithNullableInt
    {
      public int? NullableValue;
    }

    private struct StructWithNullableLong
    {
      public long? NullableValue;
    }

    private struct NestedSmallStruct
    {
      public SmallStruct Inner;
    }

    private struct NestedLargeStruct
    {
      public TwoIntStruct Inner;
      public int Extra;
    }

    private struct MixedStruct
    {
      public object Ref;
      public int IntValue;
      public byte ByteValue;
    }

    private class SampleClass
    {
      public int Value;
    }

    #endregion

    #region Primitive Types

    [Test]
    public void IsReadWriteAtomic_Byte_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<byte>());
    }

    [Test]
    public void IsReadWriteAtomic_SByte_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<sbyte>());
    }

    [Test]
    public void IsReadWriteAtomic_Short_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<short>());
    }

    [Test]
    public void IsReadWriteAtomic_UShort_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<ushort>());
    }

    [Test]
    public void IsReadWriteAtomic_Int_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<int>());
    }

    [Test]
    public void IsReadWriteAtomic_UInt_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<uint>());
    }

    [Test]
    public void IsReadWriteAtomic_Long_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<long>());
    }

    [Test]
    public void IsReadWriteAtomic_ULong_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<ulong>());
    }

    [Test]
    public void IsReadWriteAtomic_Float_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<float>());
    }

    [Test]
    public void IsReadWriteAtomic_Double_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<double>());
    }

    [Test]
    public void IsReadWriteAtomic_Bool_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<bool>());
    }

    [Test]
    public void IsReadWriteAtomic_Char_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<char>());
    }

    #endregion

    #region Reference Types

    [Test]
    public void IsReadWriteAtomic_Object_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<object>());
    }

    [Test]
    public void IsReadWriteAtomic_String_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<string>());
    }

    [Test]
    public void IsReadWriteAtomic_Class_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<SampleClass>());
    }

    [Test]
    public void IsReadWriteAtomic_Array_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<int[]>());
    }

    #endregion

    #region Pointer Types

    [Test]
    public void IsReadWriteAtomic_IntPtr_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<IntPtr>());
    }

    [Test]
    public void IsReadWriteAtomic_UIntPtr_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<UIntPtr>());
    }

    #endregion

    #region Enums

    [Test]
    public void IsReadWriteAtomic_ByteEnum_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<ByteEnum>());
    }

    [Test]
    public void IsReadWriteAtomic_IntEnum_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<IntEnum>());
    }

    [Test]
    public void IsReadWriteAtomic_LongEnum_ReturnsTrue()
    {
      // long is 8 bytes - atomic only if MaxAtomicSize >= 8
      if (MaxAtomicSize >= 8)
      {
        Assert.IsTrue(Memory.IsReadWriteAtomic<LongEnum>());
      }
      else
      {
        Assert.IsFalse(Memory.IsReadWriteAtomic<LongEnum>());
      }
    }

    #endregion

    #region Small Structs

    [Test]
    public void IsReadWriteAtomic_SmallStruct_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<SmallStruct>());
    }

    [Test]
    public void IsReadWriteAtomic_TwoIntStruct_ReturnsDependsOnArchitecture()
    {
      // 8 bytes - atomic only if MaxAtomicSize >= 8
      if (MaxAtomicSize >= 8)
      {
        Assert.IsTrue(Memory.IsReadWriteAtomic<TwoIntStruct>());
      }
      else
      {
        Assert.IsFalse(Memory.IsReadWriteAtomic<TwoIntStruct>());
      }
    }

    [Test]
    public void IsReadWriteAtomic_NestedSmallStruct_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<NestedSmallStruct>());
    }

    #endregion

    #region Large Structs

    [Test]
    public void IsReadWriteAtomic_LargeStruct_ReturnsFalse()
    {
      // 24 bytes - too large
      Assert.IsFalse(Memory.IsReadWriteAtomic<LargeStruct>());
    }

    [Test]
    public void IsReadWriteAtomic_NestedLargeStruct_ReturnsFalse()
    {
      // 12 bytes - too large
      Assert.IsFalse(Memory.IsReadWriteAtomic<NestedLargeStruct>());
    }

    [Test]
    public void IsReadWriteAtomic_Guid_ReturnsFalse()
    {
      // 16 bytes
      Assert.IsFalse(Memory.IsReadWriteAtomic<Guid>());
    }

    [Test]
    public void IsReadWriteAtomic_Decimal_ReturnsFalse()
    {
      // 16 bytes
      Assert.IsFalse(Memory.IsReadWriteAtomic<decimal>());
    }

    #endregion

    #region Structs with References

    [Test]
    public void IsReadWriteAtomic_StructWithReference_ReturnsDependsOnArchitecture()
    {
      // Contains object reference (IntPtr.Size) + int (4 bytes)
      // Total: IntPtr.Size + 4
      // On 32-bit: 4 + 4 = 8 bytes (atomic if MaxAtomicSize >= 8)
      // On 64-bit: 8 + 4 = 12 bytes (not atomic)
      var expectedSize = IntPtr.Size + 4;
      if (expectedSize <= MaxAtomicSize)
      {
        Assert.IsTrue(Memory.IsReadWriteAtomic<StructWithReference>());
      }
      else
      {
        Assert.IsFalse(Memory.IsReadWriteAtomic<StructWithReference>());
      }
    }

    [Test]
    public void IsReadWriteAtomic_StructWithString_ReturnsDependsOnArchitecture()
    {
      // Contains string reference (IntPtr.Size) + int (4 bytes)
      // Total: IntPtr.Size + 4
      var expectedSize = IntPtr.Size + 4;
      if (expectedSize <= MaxAtomicSize)
      {
        Assert.IsTrue(Memory.IsReadWriteAtomic<StructWithString>());
      }
      else
      {
        Assert.IsFalse(Memory.IsReadWriteAtomic<StructWithString>());
      }
    }

    [Test]
    public void IsReadWriteAtomic_MixedStruct_ReturnsDependsOnArchitecture()
    {
      // Contains object reference (IntPtr.Size) + int (4 bytes) + byte (1 byte)
      // Total: IntPtr.Size + 5
      // On 32-bit: 4 + 5 = 9 bytes (not atomic)
      // On 64-bit: 8 + 5 = 13 bytes (not atomic)
      var expectedSize = IntPtr.Size + 5;
      if (expectedSize <= MaxAtomicSize)
      {
        Assert.IsTrue(Memory.IsReadWriteAtomic<MixedStruct>());
      }
      else
      {
        Assert.IsFalse(Memory.IsReadWriteAtomic<MixedStruct>());
      }
    }

    #endregion

    #region Nullable Types

    [Test]
    public void IsReadWriteAtomic_NullableInt_ReturnsDependsOnArchitecture()
    {
      // Nullable<int> contains bool (1 byte) + int (4 bytes) = 5 bytes in field calculation
      // Atomic if MaxAtomicSize >= 5
      if (MaxAtomicSize >= 5)
      {
        Assert.IsTrue(Memory.IsReadWriteAtomic<int?>());
      }
      else
      {
        Assert.IsFalse(Memory.IsReadWriteAtomic<int?>());
      }
    }

    [Test]
    public void IsReadWriteAtomic_NullableLong_ReturnsFalse()
    {
      // Nullable<long> contains bool (1 byte) + long (8 bytes) = 9 bytes in field calculation
      // Always exceeds MaxAtomicSize (max 8 bytes)
      Assert.IsFalse(Memory.IsReadWriteAtomic<long?>());
    }

    [Test]
    public void IsReadWriteAtomic_NullableByte_ReturnsBasedOnSize()
    {
      // Nullable<byte> contains bool (1 byte) + byte (1 byte) = 2 bytes
      // Should be atomic
      Assert.IsTrue(Memory.IsReadWriteAtomic<byte?>());
    }

    [Test]
    public void IsReadWriteAtomic_StructWithNullableInt_ReturnsDependsOnArchitecture()
    {
      // Struct containing Nullable<int> - 5 bytes (bool + int) in field calculation
      // Atomic if MaxAtomicSize >= 5
      if (MaxAtomicSize >= 5)
      {
        Assert.IsTrue(Memory.IsReadWriteAtomic<StructWithNullableInt>());
      }
      else
      {
        Assert.IsFalse(Memory.IsReadWriteAtomic<StructWithNullableInt>());
      }
    }

    [Test]
    public void IsReadWriteAtomic_StructWithNullableLong_ReturnsFalse()
    {
      Assert.IsFalse(Memory.IsReadWriteAtomic<StructWithNullableLong>());
    }

    #endregion

    #region Common BCL Types

    [Test]
    public void IsReadWriteAtomic_DateTime_ReturnsTrue()
    {
      // DateTime is 8 bytes (single ulong internally)
      Assert.IsTrue(Memory.IsReadWriteAtomic<DateTime>());
    }

    [Test]
    public void IsReadWriteAtomic_TimeSpan_ReturnsTrue()
    {
      // TimeSpan is 8 bytes (single long internally)
      Assert.IsTrue(Memory.IsReadWriteAtomic<TimeSpan>());
    }

    [Test]
    public void IsReadWriteAtomic_DateTimeOffset_ReturnsFalse()
    {
      // DateTimeOffset contains DateTime + short = 10 bytes
      Assert.IsFalse(Memory.IsReadWriteAtomic<DateTimeOffset>());
    }

    #endregion

    #region Caching Verification

    [Test]
    public void IsReadWriteAtomic_ReturnsSameValueOnMultipleCalls()
    {
      // Verify caching works correctly
      var first = Memory.IsReadWriteAtomic<int>();
      var second = Memory.IsReadWriteAtomic<int>();
      Assert.AreEqual(first, second);

      var firstLarge = Memory.IsReadWriteAtomic<LargeStruct>();
      var secondLarge = Memory.IsReadWriteAtomic<LargeStruct>();
      Assert.AreEqual(firstLarge, secondLarge);
    }

    #endregion
  }
}
