using System;
using System.Collections.Generic;
using System.Runtime.InteropServices;
using JetBrains.Util.Internal;
using NUnit.Framework;

namespace Test.Lifetimes.Utils
{
  public class MemoryTest
  {
    private static int MaxAtomicSize => IntPtr.Size;

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

    [StructLayout(LayoutKind.Explicit)]
    private struct ExplicitLayoutLargeOffset
    {
      [FieldOffset(0)]
      public byte A;
      [FieldOffset(300)]
      public byte B;
    }

    private struct PaddedSequentialStruct
    {
      public short C;
      public int A;
      public byte B;
    }

    // Same fields as PaddedSequentialStruct but ordered largest-first → no padding waste
    private struct OptimalSequentialStruct
    {
      public int A;
      public short C;
      public byte B;
    }

    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    private struct PackedStruct
    {
      public byte A;
      public int B;
      public short C;
    }

    // Auto layout, mixed types: int(4) + byte(1) = 5 bytes of fields
    // Worst-case with padding: int(4) + byte(1) + 3pad = 8 → always ≤ 8 → atomic
    [StructLayout(LayoutKind.Auto)]
    private struct AutoLayoutMixedSmall
    {
      public int A;
      public byte B;
    }

    // Auto layout, mixed types: int(4) + byte(1) + long(8) = 13 bytes of fields
    // Non-atomic regardless of reordering (field sum alone > 8)
    [StructLayout(LayoutKind.Sequential)]
    private struct AutoLayoutMixedLarge
    {
      public int A;
      public byte B;
      public long C;
    }

    // Sequential, mixed types: short(2) + pad(2) + int(4) + short(2) + pad(2) = 12 bytes
    // Field sum = 8 but actual = 12 → non-atomic
    private struct SequentialMixedPadded
    {
      public short A;
      public int B;
      public short C;
    }

    // --- Generic structs ---

    private struct Wrapper<T>
    {
      public T Value;
    }

    private struct Pair<T1, T2>
    {
      public T1 First;
      public T2 Second;
    }

    private struct Triple<T1, T2, T3>
    {
      public T1 A;
      public T2 B;
      public T3 C;
    }

    // --- Layout variations ---

    private struct EmptyStruct { }

    [StructLayout(LayoutKind.Explicit)]
    private struct Union32
    {
      [FieldOffset(0)] public int AsInt;
      [FieldOffset(0)] public float AsFloat;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct Union64
    {
      [FieldOffset(0)] public long AsLong;
      [FieldOffset(0)] public double AsDouble;
      [FieldOffset(0)] public int AsInt;
    }

    [StructLayout(LayoutKind.Explicit)]
    private struct ExplicitPadded
    {
      [FieldOffset(0)] public byte A;
      [FieldOffset(8)] public byte B;
    }

    [StructLayout(LayoutKind.Sequential, Pack = 2)]
    private struct Pack2Struct
    {
      public byte A;
      public int B;
      public byte C;
    }

    [StructLayout(LayoutKind.Sequential, Pack = 4)]
    private struct Pack4Struct
    {
      public byte A;
      public long B;
      public byte C;
    }

    [StructLayout(LayoutKind.Sequential, Pack = 8)]
    private struct Pack8Struct
    {
      public byte A;
      public long B;
      public byte C;
    }

    [StructLayout(LayoutKind.Sequential, Size = 32)]
    private struct FixedSizeStruct
    {
      public int A;
    }

    [StructLayout(LayoutKind.Sequential, Size = 4)]
    private struct SmallFixedSizeStruct
    {
      public byte A;
    }

    [StructLayout(LayoutKind.Sequential, Pack = 1, Size = 3)]
    private struct Pack1Size3Struct
    {
      public byte A;
      public byte B;
      public byte C;
    }

    // --- Pack variations for atomicity testing ---

    // Pack=1 single byte: size=1, but Pack=1 → non-default Pack
    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    private struct Pack1SingleByte
    {
      public byte A;
    }

    // Pack=1 with int: 5 bytes total (byte + int, no padding), misaligned int
    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    private struct Pack1ByteInt
    {
      public byte A;
      public int B;
    }

    // Pack=1 single int: size=4, but fields could be at odd addresses
    [StructLayout(LayoutKind.Sequential, Pack = 1)]
    private struct Pack1SingleInt
    {
      public int A;
    }

    // Pack=2 single short: size=2
    [StructLayout(LayoutKind.Sequential, Pack = 2)]
    private struct Pack2SingleShort
    {
      public short A;
    }

    // Pack=2 with int: byte(1) + pad(1) + int(4) = 6 bytes, int at 2-byte alignment
    [StructLayout(LayoutKind.Sequential, Pack = 2)]
    private struct Pack2ByteInt
    {
      public byte A;
      public int B;
    }

    // Pack=4, single int: size=4, alignment=4
    [StructLayout(LayoutKind.Sequential, Pack = 4)]
    private struct Pack4SingleInt
    {
      public int A;
    }

    // Pack=4, single long: size=8, but long is at 4-byte alignment (not 8-byte)
    [StructLayout(LayoutKind.Sequential, Pack = 4)]
    private struct Pack4SingleLong
    {
      public long A;
    }

    // Pack=16 (more than natural alignment) — effectively same as default
    [StructLayout(LayoutKind.Sequential, Pack = 16)]
    private struct Pack16SingleInt
    {
      public int A;
    }

    // Pack=16 with int+short: same as default layout, should be safe
    [StructLayout(LayoutKind.Sequential, Pack = 16)]
    private struct Pack16IntShort
    {
      public int A;
      public short B;
    }

    // --- Explicit layout atomicity edge cases ---

    // Explicit layout, naturally aligned, fits in word
    [StructLayout(LayoutKind.Explicit)]
    private struct ExplicitAligned4
    {
      [FieldOffset(0)] public int Value;
    }

    // Explicit layout, 8-byte union — should be atomic on 64-bit
    [StructLayout(LayoutKind.Explicit)]
    private struct ExplicitUnion8
    {
      [FieldOffset(0)] public long AsLong;
      [FieldOffset(0)] public double AsDouble;
    }

    // Explicit layout with odd field offset — potential misalignment
    [StructLayout(LayoutKind.Explicit)]
    private struct ExplicitOddOffset
    {
      [FieldOffset(0)] public byte A;
      [FieldOffset(1)] public int B; // int at offset 1 — misaligned!
    }

    // Explicit layout, 3 bytes — odd size, but fits in word
    [StructLayout(LayoutKind.Explicit)]
    private struct Explicit3Bytes
    {
      [FieldOffset(0)] public byte A;
      [FieldOffset(1)] public byte B;
      [FieldOffset(2)] public byte C;
    }

    // Explicit layout, size exactly MaxAtomicSize on 64-bit
    [StructLayout(LayoutKind.Explicit)]
    private struct ExplicitExact8
    {
      [FieldOffset(0)] public int A;
      [FieldOffset(4)] public int B;
    }

    // --- Size= attribute edge cases ---

    // Struct with Size forcing it to exactly MaxAtomicSize
    [StructLayout(LayoutKind.Sequential, Size = 8)]
    private struct FixedSize8
    {
      public int A;
    }

    // Struct with Size = 9 — one byte over the atomic limit
    [StructLayout(LayoutKind.Sequential, Size = 9)]
    private struct FixedSize9
    {
      public int A;
    }

    // Struct with Size = 1
    [StructLayout(LayoutKind.Sequential, Size = 1)]
    private struct FixedSize1
    {
      public byte A;
    }

    // --- Generic struct with constrained T ---

    private struct WrapperWithExtra<T>
    {
      public T Value;
      public int Extra;
    }

    // Generic struct containing a reference and a value type
    private struct RefValuePair<TRef, TVal> where TRef : class where TVal : struct
    {
      public TRef Ref;
      public TVal Val;
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
    public void IsReadWriteAtomic_Long_DependsOnArchitecture()
    {
      Assert.AreEqual(MaxAtomicSize >= 8, Memory.IsReadWriteAtomic<long>());
    }

    [Test]
    public void IsReadWriteAtomic_ULong_DependsOnArchitecture()
    {
      Assert.AreEqual(MaxAtomicSize >= 8, Memory.IsReadWriteAtomic<ulong>());
    }

    [Test]
    public void IsReadWriteAtomic_Float_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<float>());
    }

    [Test]
    public void IsReadWriteAtomic_Double_DependsOnArchitecture()
    {
      Assert.AreEqual(MaxAtomicSize >= 8, Memory.IsReadWriteAtomic<double>());
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
    public void IsReadWriteAtomic_DateTime_DependsOnArchitecture()
    {
      // DateTime is 8 bytes (single ulong internally)
      Assert.AreEqual(MaxAtomicSize >= 8, Memory.IsReadWriteAtomic<DateTime>());
    }

    [Test]
    public void IsReadWriteAtomic_TimeSpan_DependsOnArchitecture()
    {
      // TimeSpan is 8 bytes (single long internally)
      Assert.AreEqual(MaxAtomicSize >= 8, Memory.IsReadWriteAtomic<TimeSpan>());
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

    #region Struct Layout Edge Cases

    [Test]
    public void IsReadWriteAtomic_ExplicitLayout_LargeOffset_ReturnsFalse()
    {
      // Real size is 301 bytes (FieldOffset(300) + 1 byte), clearly non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<ExplicitLayoutLargeOffset>());
    }

    [Test]
    public void IsReadWriteAtomic_PaddedSequentialStruct_ReturnsFalse()
    {
      // Real size is 12 bytes (short=2 + 2 padding + int=4 + byte=1 + 3 padding), exceeds MaxAtomicSize
      Assert.IsFalse(Memory.IsReadWriteAtomic<PaddedSequentialStruct>());
    }

    [Test]
    public void IsReadWriteAtomic_FieldOrder_AffectsAtomicity()
    {
      // Same fields, different order → different real size due to padding
      // OptimalSequentialStruct: int + short + byte + 1pad = 8 bytes → atomic on 64-bit
      // PaddedSequentialStruct:  short + 2pad + int + byte + 3pad = 12 bytes → non-atomic
      Assert.AreEqual(MaxAtomicSize >= 8, Memory.IsReadWriteAtomic<OptimalSequentialStruct>());
      Assert.IsFalse(Memory.IsReadWriteAtomic<PaddedSequentialStruct>());
    }

    [Test]
    public void IsReadWriteAtomic_PackedStruct_ReturnsFalse()
    {
      // Pack=1 means fields may be misaligned, pessimistic safety says non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<PackedStruct>());
    }

    [Test]
    public void IsReadWriteAtomic_AutoLayoutMixedSmall_DependsOnArchitecture()
    {
      // int + byte, worst-case padded to 8 → atomic only if MaxAtomicSize >= 8
      Assert.AreEqual(MaxAtomicSize >= 8, Memory.IsReadWriteAtomic<AutoLayoutMixedSmall>());
    }

    [Test]
    public void IsReadWriteAtomic_AutoLayoutMixedLarge_ReturnsFalse()
    {
      // int + byte + long = 13 bytes min → non-atomic regardless of reordering
      Assert.IsFalse(Memory.IsReadWriteAtomic<AutoLayoutMixedLarge>());
    }

    [Test]
    public void IsReadWriteAtomic_SequentialMixedPadded_ReturnsFalse()
    {
      // short + int + short: field sum = 8 but actual size = 12 due to padding
      Assert.IsFalse(Memory.IsReadWriteAtomic<SequentialMixedPadded>());
    }

    #endregion

    #region Pack Variations and Alignment

    [Test]
    public void IsReadWriteAtomic_Pack1SingleByte_ReturnsFalse()
    {
      // Pack=1 is a non-default Pack value → pessimistic: non-atomic
      // Even though size=1 would fit, the Pack=1 attribute signals potential misalignment
      Assert.IsFalse(Memory.IsReadWriteAtomic<Pack1SingleByte>());
    }

    [Test]
    public void IsReadWriteAtomic_Pack1SingleInt_ReturnsFalse()
    {
      // Pack=1, size=4: the int could be placed at any byte address
      // On ARM, misaligned 4-byte access faults; on x86, it may cross a cache line → non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<Pack1SingleInt>());
    }

    [Test]
    public void IsReadWriteAtomic_Pack1ByteInt_ReturnsFalse()
    {
      // Pack=1: byte(1) + int(4) = 5 bytes, int at offset 1 (misaligned)
      Assert.IsFalse(Memory.IsReadWriteAtomic<Pack1ByteInt>());
    }

    [Test]
    public void IsReadWriteAtomic_Pack2SingleShort_ReturnsFalse()
    {
      // Pack=2 ≠ MaxAtomicSize → rejected as non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<Pack2SingleShort>());
    }

    [Test]
    public void IsReadWriteAtomic_Pack2ByteInt_ReturnsFalse()
    {
      // Pack=2: int field at 2-byte alignment, may be misaligned for 4-byte atomic access
      Assert.IsFalse(Memory.IsReadWriteAtomic<Pack2ByteInt>());
    }

    [Test]
    public void IsReadWriteAtomic_Pack4SingleInt_DependsOnArchitecture()
    {
      // Pack=4, size=4. On 32-bit (MaxAtomicSize=4): Pack==MaxAtomicSize → atomic
      // On 64-bit (MaxAtomicSize=8): Pack=4 ≠ 8 → rejected as non-atomic (conservative)
      if (MaxAtomicSize == 4)
        Assert.IsTrue(Memory.IsReadWriteAtomic<Pack4SingleInt>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<Pack4SingleInt>());
    }

    [Test]
    public void IsReadWriteAtomic_Pack4SingleLong_DependsOnArchitecture()
    {
      // Pack=4: long at 4-byte alignment. On 64-bit, needs 8-byte alignment for atomic access
      // Pack=4 ≠ MaxAtomicSize(8) on 64-bit → false
      // On 32-bit, MaxAtomicSize=4, long(8) > 4 → false (too big)
      Assert.IsFalse(Memory.IsReadWriteAtomic<Pack4SingleLong>());
    }

    [Test]
    public void IsReadWriteAtomic_Pack8Struct_DependsOnArchitecture()
    {
      // Pack=8, size=24 → too big regardless
      Assert.IsFalse(Memory.IsReadWriteAtomic<Pack8Struct>());
    }

    [Test]
    public void IsReadWriteAtomic_Pack16SingleInt_ReturnsTrue()
    {
      // Pack=16 >= MaxAtomicSize, so fields keep their natural alignment (no degradation).
      // Size=4 <= MaxAtomicSize, so this is atomic.
      Assert.IsTrue(Memory.IsReadWriteAtomic<Pack16SingleInt>());
    }

    [Test]
    public void IsReadWriteAtomic_Pack16IntShort_DependsOnArchitecture()
    {
      // Pack=16 >= MaxAtomicSize, so fields keep their natural alignment (no degradation).
      // Size=8 <= MaxAtomicSize on 64-bit, so this is atomic.
      Assert.AreEqual(MaxAtomicSize >= 8, Memory.IsReadWriteAtomic<Pack16IntShort>());
    }

    #endregion

    #region Explicit Layout and Misalignment

    [Test]
    public void IsReadWriteAtomic_ExplicitAligned4_ReturnsTrue()
    {
      // Explicit layout, single int at offset 0, default Pack → no misalignment issue
      Assert.IsTrue(Memory.IsReadWriteAtomic<ExplicitAligned4>());
    }

    [Test]
    public void IsReadWriteAtomic_ExplicitUnion8_DependsOnArchitecture()
    {
      // Overlapping long/double at offset 0, size=8
      if (MaxAtomicSize >= 8)
        Assert.IsTrue(Memory.IsReadWriteAtomic<ExplicitUnion8>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<ExplicitUnion8>());
    }

    [Test]
    public void IsReadWriteAtomic_Union32_ReturnsTrue()
    {
      // Overlapping int/float at offset 0, size=4 → fits in word
      Assert.IsTrue(Memory.IsReadWriteAtomic<Union32>());
    }

    [Test]
    public void IsReadWriteAtomic_Union64_DependsOnArchitecture()
    {
      // Size=8, default Pack
      if (MaxAtomicSize >= 8)
        Assert.IsTrue(Memory.IsReadWriteAtomic<Union64>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<Union64>());
    }

    [Test]
    public void IsReadWriteAtomic_ExplicitOddOffset_ReturnsBasedOnSize()
    {
      // Explicit layout with int at offset 1 (misaligned within the struct).
      // The struct itself has default Pack (no StructLayout.Pack set for Explicit),
      // so the struct's own alignment is governed by the runtime.
      // Size is 5 bytes (offset 1 + sizeof(int)=4). On 64-bit, 5 <= 8 → atomic.
      // The internal misalignment (int at offset 1) doesn't affect the struct-level
      // atomicity check — we're checking if the WHOLE struct can be read atomically.
      var size = Memory.SizeOf<ExplicitOddOffset>();
      if (size <= MaxAtomicSize)
        Assert.IsTrue(Memory.IsReadWriteAtomic<ExplicitOddOffset>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<ExplicitOddOffset>());
    }

    [Test]
    public void IsReadWriteAtomic_Explicit3Bytes_ReturnsTrue()
    {
      // 3 bytes, default Pack → always fits in word (MaxAtomicSize >= 4)
      Assert.IsTrue(Memory.IsReadWriteAtomic<Explicit3Bytes>());
    }

    [Test]
    public void IsReadWriteAtomic_ExplicitExact8_DependsOnArchitecture()
    {
      // Exactly 8 bytes (two ints at offset 0 and 4)
      if (MaxAtomicSize >= 8)
        Assert.IsTrue(Memory.IsReadWriteAtomic<ExplicitExact8>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<ExplicitExact8>());
    }

    [Test]
    public void IsReadWriteAtomic_ExplicitPadded_ReturnsFalse()
    {
      // byte at offset 0, byte at offset 8 → size=9 > MaxAtomicSize → non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<ExplicitPadded>());
    }

    #endregion

    #region Size= Attribute Edge Cases

    [Test]
    public void IsReadWriteAtomic_FixedSize8_DependsOnArchitecture()
    {
      // Size forced to 8 via StructLayout.Size, only int(4) field
      if (MaxAtomicSize >= 8)
        Assert.IsTrue(Memory.IsReadWriteAtomic<FixedSize8>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<FixedSize8>());
    }

    [Test]
    public void IsReadWriteAtomic_FixedSize9_ReturnsFalse()
    {
      // Size forced to 9 → exceeds MaxAtomicSize on all architectures
      Assert.IsFalse(Memory.IsReadWriteAtomic<FixedSize9>());
    }

    [Test]
    public void IsReadWriteAtomic_FixedSize1_ReturnsTrue()
    {
      // Size=1, single byte → always atomic
      Assert.IsTrue(Memory.IsReadWriteAtomic<FixedSize1>());
    }

    [Test]
    public void IsReadWriteAtomic_FixedSize32_ReturnsFalse()
    {
      // Size=32 far exceeds MaxAtomicSize
      Assert.IsFalse(Memory.IsReadWriteAtomic<FixedSizeStruct>());
    }

    [Test]
    public void IsReadWriteAtomic_SmallFixedSize4_ReturnsTrue()
    {
      // Size=4, single byte field but struct padded to 4 → fits in word
      Assert.IsTrue(Memory.IsReadWriteAtomic<SmallFixedSizeStruct>());
    }

    [Test]
    public void IsReadWriteAtomic_Pack1Size3_ReturnsFalse()
    {
      // Pack=1, Size=3 → Pack ≠ 0 and Pack ≠ MaxAtomicSize → non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<Pack1Size3Struct>());
    }

    [Test]
    public void IsReadWriteAtomic_EmptyStruct_ReturnsTrue()
    {
      // Empty struct has size 1 → always fits
      Assert.IsTrue(Memory.IsReadWriteAtomic<EmptyStruct>());
    }

    #endregion

    #region Generic Structs and IsReadWriteAtomic

    [Test]
    public void IsReadWriteAtomic_WrapperInt_ReturnsTrue()
    {
      // Wrapper<int>: size=4, default layout → atomic
      Assert.IsTrue(Memory.IsReadWriteAtomic<Wrapper<int>>());
    }

    [Test]
    public void IsReadWriteAtomic_WrapperByte_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<Wrapper<byte>>());
    }

    [Test]
    public void IsReadWriteAtomic_WrapperLong_DependsOnArchitecture()
    {
      // Wrapper<long>: size=8
      if (MaxAtomicSize >= 8)
        Assert.IsTrue(Memory.IsReadWriteAtomic<Wrapper<long>>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<Wrapper<long>>());
    }

    [Test]
    public void IsReadWriteAtomic_WrapperObject_IsValueType()
    {
      // Wrapper<object> is a struct containing a reference → it's a value type, not a reference type
      // Size = IntPtr.Size (just a single reference field) → fits in word → atomic
      Assert.IsTrue(typeof(Wrapper<object>).IsValueType);
      Assert.IsTrue(Memory.IsReadWriteAtomic<Wrapper<object>>());
    }

    [Test]
    public void IsReadWriteAtomic_WrapperString_ReturnsTrue()
    {
      // Wrapper<string>: value type containing string ref, size = IntPtr.Size
      Assert.IsTrue(Memory.IsReadWriteAtomic<Wrapper<string>>());
    }

    [Test]
    public void IsReadWriteAtomic_PairIntInt_DependsOnArchitecture()
    {
      // Pair<int,int>: size=8
      if (MaxAtomicSize >= 8)
        Assert.IsTrue(Memory.IsReadWriteAtomic<Pair<int, int>>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<Pair<int, int>>());
    }

    [Test]
    public void IsReadWriteAtomic_PairByteByte_ReturnsTrue()
    {
      // Pair<byte,byte>: size=2 → always atomic
      Assert.IsTrue(Memory.IsReadWriteAtomic<Pair<byte, byte>>());
    }

    [Test]
    public void IsReadWriteAtomic_PairIntLong_ReturnsFalse()
    {
      // Pair<int,long>: size=16 (with padding) → always non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<Pair<int, long>>());
    }

    [Test]
    public void IsReadWriteAtomic_PairObjectObject_ReturnsFalse()
    {
      // Pair<object,object>: struct with 2 refs → size = 2 * IntPtr.Size
      // On 64-bit: 16 bytes → non-atomic. On 32-bit: 8 bytes → depends
      var size = Memory.SizeOf<Pair<object, object>>();
      if (size <= MaxAtomicSize)
        Assert.IsTrue(Memory.IsReadWriteAtomic<Pair<object, object>>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<Pair<object, object>>());
    }

    [Test]
    public void IsReadWriteAtomic_TripleIntIntInt_ReturnsFalse()
    {
      // Triple<int,int,int>: size=12 → always non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<Triple<int, int, int>>());
    }

    [Test]
    public void IsReadWriteAtomic_TripleByteByteByte_ReturnsTrue()
    {
      // Triple<byte,byte,byte>: size=3 → always atomic
      Assert.IsTrue(Memory.IsReadWriteAtomic<Triple<byte, byte, byte>>());
    }

    [Test]
    public void IsReadWriteAtomic_WrapperWrapperInt_ReturnsTrue()
    {
      // Nested generic: Wrapper<Wrapper<int>> has size=4 → atomic
      Assert.IsTrue(Memory.IsReadWriteAtomic<Wrapper<Wrapper<int>>>());
    }

    [Test]
    public void IsReadWriteAtomic_WrapperPairByteByte_ReturnsTrue()
    {
      // Wrapper<Pair<byte,byte>>: size=2 → atomic
      Assert.IsTrue(Memory.IsReadWriteAtomic<Wrapper<Pair<byte, byte>>>());
    }

    [Test]
    public void IsReadWriteAtomic_WrapperWithExtraInt_DependsOnArchitecture()
    {
      // WrapperWithExtra<int>: int(4) + int(4) = 8
      if (MaxAtomicSize >= 8)
        Assert.IsTrue(Memory.IsReadWriteAtomic<WrapperWithExtra<int>>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<WrapperWithExtra<int>>());
    }

    [Test]
    public void IsReadWriteAtomic_WrapperWithExtraLong_ReturnsFalse()
    {
      // WrapperWithExtra<long>: long(8) + int(4) + pad(4) = 16 → non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<WrapperWithExtra<long>>());
    }

    [Test]
    public void IsReadWriteAtomic_WrapperWithExtraByte_DependsOnArchitecture()
    {
      // WrapperWithExtra<byte>: byte(1) + pad(3) + int(4) = 8
      if (MaxAtomicSize >= 8)
        Assert.IsTrue(Memory.IsReadWriteAtomic<WrapperWithExtra<byte>>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<WrapperWithExtra<byte>>());
    }

    [Test]
    public void IsReadWriteAtomic_RefValuePair_StringInt_ReturnsFalse()
    {
      // RefValuePair<string, int>: ref(IntPtr.Size) + int(4) + pad
      // On 64-bit: 8+4+4pad = 16 → non-atomic
      // On 32-bit: 4+4 = 8 → depends
      var size = Memory.SizeOf<RefValuePair<string, int>>();
      if (size <= MaxAtomicSize)
        Assert.IsTrue(Memory.IsReadWriteAtomic<RefValuePair<string, int>>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<RefValuePair<string, int>>());
    }

    [Test]
    public void IsReadWriteAtomic_ValueTuple_ByteByte_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<(byte, byte)>());
    }

    [Test]
    public void IsReadWriteAtomic_ValueTuple_IntInt_DependsOnArchitecture()
    {
      // (int, int) = 8 bytes
      if (MaxAtomicSize >= 8)
        Assert.IsTrue(Memory.IsReadWriteAtomic<(int, int)>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<(int, int)>());
    }

    [Test]
    public void IsReadWriteAtomic_ValueTuple_IntIntInt_ReturnsFalse()
    {
      // (int, int, int) = 12 bytes → non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<(int, int, int)>());
    }

    [Test]
    public void IsReadWriteAtomic_KeyValuePair_ByteByte_ReturnsTrue()
    {
      Assert.IsTrue(Memory.IsReadWriteAtomic<System.Collections.Generic.KeyValuePair<byte, byte>>());
    }

    [Test]
    public void IsReadWriteAtomic_KeyValuePair_IntInt_DependsOnArchitecture()
    {
      if (MaxAtomicSize >= 8)
        Assert.IsTrue(Memory.IsReadWriteAtomic<System.Collections.Generic.KeyValuePair<int, int>>());
      else
        Assert.IsFalse(Memory.IsReadWriteAtomic<System.Collections.Generic.KeyValuePair<int, int>>());
    }

    [Test]
    public void IsReadWriteAtomic_KeyValuePair_StringInt_ReturnsFalse()
    {
      // string ref + int → 12-16 bytes → non-atomic
      Assert.IsFalse(Memory.IsReadWriteAtomic<System.Collections.Generic.KeyValuePair<string, int>>());
    }

    #endregion

    #region Endianness — Does Not Affect Atomicity

    // Atomicity depends on bus width and alignment, NOT byte ordering.
    // A torn read produces a mix of old/new bytes regardless of endianness.
    // These tests document that IsReadWriteAtomic is endian-independent:
    // the result is determined only by size and alignment.

    [Test]
    public void IsReadWriteAtomic_EndiannessIrrelevant_IntAlwaysAtomic()
    {
      // int (4 bytes) is atomic on all supported .NET architectures (x86, x64, ARM, ARM64)
      // regardless of endianness. All these architectures have at least 4-byte atomic ops.
      Assert.IsTrue(Memory.IsReadWriteAtomic<int>());
    }

    [Test]
    public void IsReadWriteAtomic_EndiannessIrrelevant_LongAtomicityDependsOnWordSize()
    {
      // long (8 bytes) atomicity depends on word size, not endianness.
      // On ARM (32-bit big-endian or little-endian): non-atomic (8 > 4)
      // On ARM64 / x64 (little-endian): atomic (8 <= 8)
      // The check is purely size-based.
      Assert.AreEqual(MaxAtomicSize >= 8, Memory.IsReadWriteAtomic<long>());
    }

    [Test]
    public void IsReadWriteAtomic_EndiannessIrrelevant_SingleByteAlwaysAtomic()
    {
      // A single byte is always atomic — endianness doesn't even apply to 1-byte values
      Assert.IsTrue(Memory.IsReadWriteAtomic<byte>());
    }

    #endregion

    #region SizeOf

    [Test]
    public void SizeOf_Int_Returns4()
    {
      Assert.AreEqual(4, Memory.SizeOf<int>());
    }

    [Test]
    public void SizeOf_Long_Returns8()
    {
      Assert.AreEqual(8, Memory.SizeOf<long>());
    }

    [Test]
    public void SizeOf_Byte_Returns1()
    {
      Assert.AreEqual(1, Memory.SizeOf<byte>());
    }

    [Test]
    public void SizeOf_Guid_Returns16()
    {
      Assert.AreEqual(16, Memory.SizeOf<Guid>());
    }

    [Test]
    public void SizeOf_Bool_Returns1()
    {
      Assert.AreEqual(1, Memory.SizeOf<bool>());
    }

    [Test]
    public void SizeOf_Short_Returns2()
    {
      Assert.AreEqual(2, Memory.SizeOf<short>());
    }

    [Test]
    public void SizeOf_Double_Returns8()
    {
      Assert.AreEqual(8, Memory.SizeOf<double>());
    }

    [Test]
    public void SizeOf_IntPtr_ReturnsPointerSize()
    {
      Assert.AreEqual(IntPtr.Size, Memory.SizeOf<IntPtr>());
    }

    [Test]
    public void SizeOf_ReferenceType_ReturnsIntPtrSize()
    {
      Assert.AreEqual(IntPtr.Size, Memory.SizeOf<object>());
      Assert.AreEqual(IntPtr.Size, Memory.SizeOf<string>());
      Assert.AreEqual(IntPtr.Size, Memory.SizeOf<SampleClass>());
    }

    [Test]
    public void SizeOf_ByteEnum_Returns1()
    {
      Assert.AreEqual(1, Memory.SizeOf<ByteEnum>());
    }

    [Test]
    public void SizeOf_IntEnum_Returns4()
    {
      Assert.AreEqual(4, Memory.SizeOf<IntEnum>());
    }

    [Test]
    public void SizeOf_LongEnum_Returns8()
    {
      Assert.AreEqual(8, Memory.SizeOf<LongEnum>());
    }

    [Test]
    public void SizeOf_SmallStruct_Returns4()
    {
      Assert.AreEqual(4, Memory.SizeOf<SmallStruct>());
    }

    [Test]
    public void SizeOf_TwoIntStruct_Returns8()
    {
      Assert.AreEqual(8, Memory.SizeOf<TwoIntStruct>());
    }

    [Test]
    public void SizeOf_LargeStruct_Returns24()
    {
      Assert.AreEqual(24, Memory.SizeOf<LargeStruct>());
    }

    #endregion

    #region SizeOf - Generic Structs

    [Test]
    public void SizeOf_WrapperInt_Returns4()
    {
      Assert.AreEqual(4, Memory.SizeOf<Wrapper<int>>());
    }

    [Test]
    public void SizeOf_WrapperLong_Returns8()
    {
      Assert.AreEqual(8, Memory.SizeOf<Wrapper<long>>());
    }

    [Test]
    public void SizeOf_WrapperByte_Returns1()
    {
      Assert.AreEqual(1, Memory.SizeOf<Wrapper<byte>>());
    }

    [Test]
    public void SizeOf_WrapperObject_ReturnsIntPtrSize()
    {
      // Wrapper<object> contains a reference field → size = IntPtr.Size
      Assert.AreEqual(IntPtr.Size, Memory.SizeOf<Wrapper<object>>());
    }

    [Test]
    public void SizeOf_WrapperString_ReturnsIntPtrSize()
    {
      Assert.AreEqual(IntPtr.Size, Memory.SizeOf<Wrapper<string>>());
    }

    [Test]
    public void SizeOf_PairIntInt_Returns8()
    {
      Assert.AreEqual(8, Memory.SizeOf<Pair<int, int>>());
    }

    [Test]
    public void SizeOf_PairByteByte_Returns2()
    {
      Assert.AreEqual(2, Memory.SizeOf<Pair<byte, byte>>());
    }

    [Test]
    public void SizeOf_PairIntLong_HasPadding()
    {
      // Sequential: int(4) + pad(4) + long(8) = 16
      Assert.AreEqual(16, Memory.SizeOf<Pair<int, long>>());
    }

    [Test]
    public void SizeOf_PairLongInt_Returns16()
    {
      // Sequential: long(8) + int(4) + pad(4) = 16
      Assert.AreEqual(16, Memory.SizeOf<Pair<long, int>>());
    }

    [Test]
    public void SizeOf_PairByteInt_HasPadding()
    {
      // Sequential: byte(1) + pad(3) + int(4) = 8
      Assert.AreEqual(8, Memory.SizeOf<Pair<byte, int>>());
    }

    [Test]
    public void SizeOf_PairIntObject_ContainsReference()
    {
      // int(4) + pad + object ref(IntPtr.Size)
      // On 64-bit: int(4) + pad(4) + ref(8) = 16
      // On 32-bit: int(4) + ref(4) = 8
      var size = Memory.SizeOf<Pair<int, object>>();
      Assert.Greater(size, 4); // at least bigger than int alone
      Assert.AreEqual(IntPtr.Size == 8 ? 16 : 8, size);
    }

    [Test]
    public void SizeOf_PairObjectObject_ReturnsTwoPointers()
    {
      Assert.AreEqual(IntPtr.Size * 2, Memory.SizeOf<Pair<object, object>>());
    }

    [Test]
    public void SizeOf_TripleIntIntInt_Returns12()
    {
      Assert.AreEqual(12, Memory.SizeOf<Triple<int, int, int>>());
    }

    [Test]
    public void SizeOf_TripleByteByteByte_Returns3()
    {
      Assert.AreEqual(3, Memory.SizeOf<Triple<byte, byte, byte>>());
    }

    [Test]
    public void SizeOf_TripleLongLongLong_Returns24()
    {
      Assert.AreEqual(24, Memory.SizeOf<Triple<long, long, long>>());
    }

    [Test]
    public void SizeOf_NestedGeneric_WrapperWrapperInt_Returns4()
    {
      Assert.AreEqual(4, Memory.SizeOf<Wrapper<Wrapper<int>>>());
    }

    [Test]
    public void SizeOf_NestedGeneric_WrapperPairIntLong()
    {
      // Wrapper<Pair<int, long>> should be same size as Pair<int, long>
      Assert.AreEqual(Memory.SizeOf<Pair<int, long>>(), Memory.SizeOf<Wrapper<Pair<int, long>>>());
    }

    [Test]
    public void SizeOf_NullableInt_Returns8()
    {
      // Nullable<int>: bool(1→4 padded) + int(4) = 8
      Assert.AreEqual(8, Memory.SizeOf<int?>());
    }

    [Test]
    public void SizeOf_NullableByte_Returns2()
    {
      // Nullable<byte>: bool(1) + byte(1) = 2
      Assert.AreEqual(2, Memory.SizeOf<byte?>());
    }

    [Test]
    public void SizeOf_NullableLong_Returns16()
    {
      // Nullable<long>: bool(1→8 padded) + long(8) = 16
      Assert.AreEqual(16, Memory.SizeOf<long?>());
    }

    [Test]
    public void SizeOf_KeyValuePair_IntInt_Returns8()
    {
      Assert.AreEqual(8, Memory.SizeOf<KeyValuePair<int, int>>());
    }

    [Test]
    public void SizeOf_KeyValuePair_IntString_ContainsReference()
    {
      var size = Memory.SizeOf<KeyValuePair<int, string>>();
      Assert.AreEqual(IntPtr.Size == 8 ? 16 : 8, size);
    }

    [Test]
    public void SizeOf_ValueTuple_IntInt_Returns8()
    {
      Assert.AreEqual(8, Memory.SizeOf<(int, int)>());
    }

    [Test]
    public void SizeOf_ValueTuple_ByteByteByteByteByteByteByteInt()
    {
      // (byte, byte, byte, byte, byte, byte, byte, int)
      // Large ValueTuple with Rest field: ValueTuple<b,b,b,b,b,b,b,ValueTuple<int>>
      var size = Memory.SizeOf<ValueTuple<byte, byte, byte, byte, byte, byte, byte, ValueTuple<int>>>();
      Assert.Greater(size, 7 + 4); // at least 11 bytes of data
    }

    #endregion

    #region SizeOf - Layout Variations

    [Test]
    public void SizeOf_EmptyStruct_Returns1()
    {
      // Empty struct has size 1 in .NET
      Assert.AreEqual(1, Memory.SizeOf<EmptyStruct>());
    }

    [Test]
    public void SizeOf_Union32_Returns4()
    {
      // Overlapping int and float at offset 0 → 4 bytes
      Assert.AreEqual(4, Memory.SizeOf<Union32>());
    }

    [Test]
    public void SizeOf_Union64_Returns8()
    {
      // Overlapping long/double/int at offset 0 → 8 bytes (max field)
      Assert.AreEqual(8, Memory.SizeOf<Union64>());
    }

    [Test]
    public void SizeOf_ExplicitPadded_Returns9()
    {
      // byte at offset 0, byte at offset 8 → size is 9
      Assert.AreEqual(9, Memory.SizeOf<ExplicitPadded>());
    }

    [Test]
    public void SizeOf_ExplicitLayoutLargeOffset()
    {
      // byte at 0, byte at 300 → at least 301 bytes
      Assert.GreaterOrEqual(Memory.SizeOf<ExplicitLayoutLargeOffset>(), 301);
    }

    [Test]
    public void SizeOf_Pack1Struct_Returns7()
    {
      // Pack=1: byte(1) + int(4) + short(2) = 7, no padding
      Assert.AreEqual(7, Memory.SizeOf<PackedStruct>());
    }

    [Test]
    public void SizeOf_Pack2Struct_Returns8()
    {
      // Pack=2: byte(1) + pad(1) + int(4) + byte(1) + pad(1) = 8
      Assert.AreEqual(8, Memory.SizeOf<Pack2Struct>());
    }

    [Test]
    public void SizeOf_Pack4Struct()
    {
      // Pack=4: byte(1) + pad(3) + long as 2×4(8) + byte(1) + pad(3) = 16
      Assert.AreEqual(16, Memory.SizeOf<Pack4Struct>());
    }

    [Test]
    public void SizeOf_Pack8Struct()
    {
      // Pack=8: byte(1) + pad(7) + long(8) + byte(1) + pad(7) = 24
      Assert.AreEqual(24, Memory.SizeOf<Pack8Struct>());
    }

    [Test]
    public void SizeOf_FixedSizeStruct_Returns32()
    {
      // Size=32 specified in StructLayout, even though only int(4) field
      Assert.AreEqual(32, Memory.SizeOf<FixedSizeStruct>());
    }

    [Test]
    public void SizeOf_SmallFixedSizeStruct_Returns4()
    {
      // Size=4, only byte(1) field → padded to 4
      Assert.AreEqual(4, Memory.SizeOf<SmallFixedSizeStruct>());
    }

    [Test]
    public void SizeOf_Pack1Size3Struct_Returns3()
    {
      // Pack=1, Size=3: 3 bytes, no padding
      Assert.AreEqual(3, Memory.SizeOf<Pack1Size3Struct>());
    }

    [Test]
    public void SizeOf_PaddedSequentialStruct_Returns12()
    {
      // short(2) + pad(2) + int(4) + byte(1) + pad(3) = 12
      Assert.AreEqual(12, Memory.SizeOf<PaddedSequentialStruct>());
    }

    [Test]
    public void SizeOf_OptimalSequentialStruct_Returns8()
    {
      // int(4) + short(2) + byte(1) + pad(1) = 8
      Assert.AreEqual(8, Memory.SizeOf<OptimalSequentialStruct>());
    }

    [Test]
    public void SizeOf_SequentialMixedPadded_Returns12()
    {
      // short(2) + pad(2) + int(4) + short(2) + pad(2) = 12
      Assert.AreEqual(12, Memory.SizeOf<SequentialMixedPadded>());
    }

    [Test]
    public void SizeOf_Decimal_Returns16()
    {
      Assert.AreEqual(16, Memory.SizeOf<decimal>());
    }

    [Test]
    public void SizeOf_DateTime_Returns8()
    {
      Assert.AreEqual(8, Memory.SizeOf<DateTime>());
    }

    [Test]
    public void SizeOf_DateTimeOffset_DependsOnArchitecture()
    {
      // 64-bit: DateTime(8) + short(2) + pad(6) = 16
      // 32-bit: DateTime(8) + short(2) + pad(2) = 12
      var expected = IntPtr.Size == 8 ? 16 : 12;
      Assert.AreEqual(expected, Memory.SizeOf<DateTimeOffset>());
    }

    #endregion

    #region SizeOf - Consistency with IsReadWriteAtomic

    [Test]
    public void SizeOf_ConsistentWithIsReadWriteAtomic_SmallValues()
    {
      // Anything with SizeOf <= MaxAtomicSize should be atomic (for value types without Pack issues)
      Assert.LessOrEqual(Memory.SizeOf<int>(), MaxAtomicSize);
      Assert.IsTrue(Memory.IsReadWriteAtomic<int>());

      Assert.LessOrEqual(Memory.SizeOf<byte>(), MaxAtomicSize);
      Assert.IsTrue(Memory.IsReadWriteAtomic<byte>());
    }

    [Test]
    public void SizeOf_ConsistentWithIsReadWriteAtomic_LargeValues()
    {
      // Anything with SizeOf > MaxAtomicSize should not be atomic
      Assert.Greater(Memory.SizeOf<LargeStruct>(), MaxAtomicSize);
      Assert.IsFalse(Memory.IsReadWriteAtomic<LargeStruct>());

      Assert.Greater(Memory.SizeOf<Guid>(), MaxAtomicSize);
      Assert.IsFalse(Memory.IsReadWriteAtomic<Guid>());
    }

    #endregion
  }
}
