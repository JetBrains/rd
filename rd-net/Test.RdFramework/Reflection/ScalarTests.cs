using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ScalarTests : RdReflectionTestBase
  {
    [Test]
    public void TestColor1()
    {
      WithExts<ColorsExt>((c, s) =>
      {
        c.Map.Add(new ColorFields(100, 100, 100), long.MaxValue);
        Assert.AreEqual(s.Map[new ColorFields(100, 100, 100)], c.Map.Values.First());
      });
    }

    [Test]
    public void TestColor2()
    {
      WithExts<ColorsExt>((c, s) =>
      {
        c.List.Add(new ColorStruct() {Blue = 1, Green = 2, Red = 3});
        c.List.Add(new ColorStruct());
        CollectionAssert.AreEqual(s.List, c.List);
      });
    }

    [Test]
    public void TestColor3()
    {
      WithExts<ColorsExt>((c, s) =>
      {
        c.List.Add(new ColorStruct() {Blue = 1, Green = 2, Red = 3});
        c.List.Add(new ColorStruct());
        CollectionAssert.AreEqual(s.List, c.List);
      });
    }

    [Test]
    public void TestValueTuple()
    {
      WithExts<ValueTuplesExt>((c, s) =>
      {
        var val = ("test", "test2");
        c.SimpleTuple.Value = val;
        Assert.AreEqual(val, s.SimpleTuple.Value);
      });
    }

    [Test]
    public void TestValueTupleNested()
    {
      WithExts<ValueTuplesExt>((c, s) =>
      {
        var val = (0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1);
        s.NestedTuple.Value = val;
        Assert.AreEqual(val, c.NestedTuple.Value);
      });
    }

    [Test]
    public void TestArrays()
    {
      WithExts<ArraysExt>((c, s) =>
      {
        s.IntArray.Value = new[] {1, 2, 3};
        CollectionAssert.AreEqual(s.IntArray.Value, c.IntArray.Value);
      });
    }

    [Test]
    public void TestArrays2()
    {
      WithExts<ArraysExt>((c, s) =>
      {
        s.ComplexArray.Value = new[] {new[] {new[] {1, 2, 3}}};
        CollectionAssert.AreEqual(s.ComplexArray.Value[0][0], c.ComplexArray.Value[0][0]);
      });
    }

    [Test]
    public void TestArrays3()
    {
      WithExts<ArraysExt>((c, s) =>
      {
        s.ValueTupleArray.Value = new[] {("key", "value"), ("key", "value")};
        CollectionAssert.AreEqual(s.ValueTupleArray.Value, c.ValueTupleArray.Value);
      });
    }

    [Test]
    public void TestArraysClass()
    {
      WithExts<ArraysExt2>((c, s) =>
      {
        s.Property.Value = new ColorFields(1, 1, 1);
        s.PropertyArray.Value = new[] {new ColorFields(2, 2, 2)};

        Assert.AreEqual(s.Property.Value, c.Property.Value);
        CollectionAssert.AreEqual(s.PropertyArray.Value, c.PropertyArray.Value);
      });
    }

    [Test]
    public void TestList2()
    {
      AddType(typeof(ColorFields));
      WithExts<ListObjectsExt>((c, s) =>
      {
        s.Objects.Value = new ListOwner()
        {
          Ints = new List<int>() {1, 2, 3},
          Polymorphic = new List<ColorFields>()
          {
            new ColorFields(1, 2, 3)
          },
          PolymorphicArray = new List<ColorFields[]>()
          {
            new ColorFields[30]
          }
        };

        CollectionAssert.AreEqual(s.Objects.Value.Ints, c.Objects.Value.Ints);
        Assert.AreEqual(s.Objects.Value.Polymorphic[0].Blue, s.Objects.Value.Polymorphic[0].Blue);
        Assert.AreEqual(s.Objects.Value.PolymorphicArray[0].Length, s.Objects.Value.PolymorphicArray[0].Length);
      });
    }

    [Test]
    public void TestListInterface()
    {
      WithExts<ListInterfacesExt>((c, s) =>
      {
        c.Objects.Value = new ListInterfacesExt.ListOwner1()
        {
          InterfaceListOfInts = new List<int>() {1, 2, 3},
          ListOfInts = new List<int>() {1, 2, 3},
          CollectionOfInts = new List<int>() {1, 2, 3},
          ReadonlyListInts = new List<int>() {1, 2, 3},
          EnumerableInts = new List<int>() {1, 2, 3},
        };

        CollectionAssert.AreEqual(c.Objects.Value.InterfaceListOfInts , s.Objects.Value.InterfaceListOfInts );
        CollectionAssert.AreEqual(c.Objects.Value.ListOfInts          , s.Objects.Value.ListOfInts          );
        CollectionAssert.AreEqual(c.Objects.Value.CollectionOfInts    , s.Objects.Value.CollectionOfInts    );
        CollectionAssert.AreEqual(c.Objects.Value.ReadonlyListInts    , s.Objects.Value.ReadonlyListInts    );
        CollectionAssert.AreEqual(c.Objects.Value.EnumerableInts      , s.Objects.Value.EnumerableInts      );
      });
    }
    
    [RdExt]
    public class ListInterfacesExt : RdExtReflectionBindableBase
    {
      public IViewableProperty<ListOwner1> Objects { get; }

      public class ListOwner1
      {
        public IList<int> InterfaceListOfInts;
        public List<int> ListOfInts;
        public IList<int> CollectionOfInts;
        public IReadOnlyList<int> ReadonlyListInts;
        public IEnumerable<int> EnumerableInts;
      }
    }

    [Test]
    public void TestEventArgs()
    {
      WithExts<EventArgsExt>((c, s) =>
      {
        c.Objects.Value = new MyEventArgs<int?>() { Value = 42 };
        Assert.AreEqual(42, s.Objects.Value.Value);
      });
    }

    [Test]
    public void TestGenericStruct()
    {
      WithExts<GenericStructExt>((c, s) =>
      {
        c.EnumStruct.Value = new TextControlOverridableValue<MyEnum>(MyEnum.First, MyEnum.Second);
        c.IntStruct.Value = new TextControlOverridableValue<int>(1,2);

        Assert.AreEqual(MyEnum.First, s.EnumStruct.Value.Original);
        Assert.AreEqual(MyEnum.Second, s.EnumStruct.Value.Override);
        Assert.AreEqual(1, s.IntStruct.Value.Original);
        Assert.AreEqual(2, s.IntStruct.Value.Override);
      });
    }


    [Test]
    public void TestDictionary()
    {
      RunScalarTest(new Dictionary<string, string>() { {"a", "b"}}, CollectionAssert.AreEqual);
    }

    [Test]
    public void TestIDictionary()
    {
      RunScalarTest(new Dictionary<string, string>() { {"a", "b"}} as IDictionary<string, string>, CollectionAssert.AreEqual);
    }

    [Test]
    public void TestReadOnlyDictionary()
    {
      RunScalarTest(new Dictionary<string, string>() { {"a", "b"}} as IReadOnlyDictionary<string, string>, CollectionAssert.AreEqual);
    }

    [Test]
    public void TestBoxing()
    {
      RunScalarTest<object>(true, (a, b) => Assert.AreEqual((bool)a, (bool)b));
      RunScalarTest<object>(MyEnum.First, (a, b) => Assert.AreEqual((MyEnum)a, (MyEnum)b));
    }

    [Test]
    public void TestEnumsOfAllUnderlyingTypes()
    {
      void Check<T>(int expectedSize, params T[] values) where T : Enum
      {
        foreach (var value in values)
        {
          RunScalarTest(value, (a, b) => Assert.AreEqual(a, b));
          Assert.AreEqual(expectedSize, SerializedSize(value), "{0}.{1}", typeof(T).Name, value);
        }
      }

      Check(4, SByteEnum.Min, SByteEnum.Max, SByteEnum.MinusOne);
      Check(4, ByteEnum.Min, ByteEnum.Max);
      Check(4, ShortEnum.Min, ShortEnum.Max, ShortEnum.MinusOne);
      Check(4, UShortEnum.Min, UShortEnum.Max);
      Check(4, IntEnum.Min, IntEnum.Max, IntEnum.MinusOne);
      Check(4, UIntFlags.None, UIntFlags.Bit0, UIntFlags.Bit31, UIntFlags.All);
      Check(8, LongEnum.Min, LongEnum.Max, LongEnum.MinusOne, LongEnum.IntMaxPlusOne);
      Check(8, ULongFlags.None, ULongFlags.Bit31, ULongFlags.Bit32, ULongFlags.Bit63, ULongFlags.All);
    }

    [Test]
    public void TestIntEnumWireFormatIsUnchanged()
    {
      var serializerPair = new ReflectionSerializers(CFacade.TypesCatalog).GetOrRegisterSerializerPair(typeof(UIntFlags), true);
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        serializerPair.GetWriter<UIntFlags>()(default, cookie.Writer, UIntFlags.Bit31 | UIntFlags.Bit0);
        unsafe
        {
          Assert.AreEqual(unchecked((int) 0x80000001), UnsafeReader.CreateReader(cookie.Data, cookie.Count).ReadInt());
        }
      }
    }

    private int SerializedSize<T>(T value)
    {
      var writer = new ReflectionSerializers(CFacade.TypesCatalog).GetOrRegisterSerializerPair(typeof(T), true).GetWriter<T>();
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        writer(default, cookie.Writer, value);
        return cookie.Count;
      }
    }

    public enum SByteEnum : sbyte { Min = sbyte.MinValue, MinusOne = -1, Max = sbyte.MaxValue }
    public enum ByteEnum : byte { Min = byte.MinValue, Max = byte.MaxValue }
    public enum ShortEnum : short { Min = short.MinValue, MinusOne = -1, Max = short.MaxValue }
    public enum UShortEnum : ushort { Min = ushort.MinValue, Max = ushort.MaxValue }
    public enum IntEnum { Min = int.MinValue, MinusOne = -1, Max = int.MaxValue }
    public enum LongEnum : long { Min = long.MinValue, MinusOne = -1, IntMaxPlusOne = (long) int.MaxValue + 1, Max = long.MaxValue }

    [Flags]
    public enum UIntFlags : uint { None = 0, Bit0 = 1, Bit31 = 0x80000000, All = uint.MaxValue }

    [Flags]
    public enum ULongFlags : ulong { None = 0, Bit31 = 0x80000000, Bit32 = 0x100000000, Bit63 = 0x8000000000000000, All = ulong.MaxValue }

    [Test]
    public void TestCyclic()
    {
      RunScalarTest(new RedBlackList {Start = new RedBlackList.BlackNode() {Next = new RedBlackList.RedNode()}}, (a, b) =>
      {
        Assert.NotNull(b.Start.Next);
        Assert.Null(b.Start.Next.Next);
      });
    }

    [Test]
    public void TestInvalidPointers()
    {
      Assert.Throws<ArgumentException>(() => { RunScalarTest(new NonScalarPtr(), (a, b) => { }); });
    }

    public class NonScalarPtr
    {
      public Action X;
    }

    public sealed class RedBlackList
    {
      public BlackNode Start;
      public sealed class BlackNode
      {
        public RedNode Next;
      }

      public sealed class RedNode
      {
        public BlackNode Next;
      }
    }
    
    public void RunScalarTest<T>(T instance, Action<T, T> checkEqual)
    {
      var scalar = new ReflectionSerializers(CFacade.TypesCatalog);
      var serializerPair = scalar.GetOrRegisterSerializerPair(typeof(T), true);
      var reader = serializerPair.GetReader<T>();
      var writer = serializerPair.GetWriter<T>();
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        unsafe
        {
          writer(default, cookie.Writer, instance);
          var unsafeReader = UnsafeReader.CreateReader(cookie.Data, cookie.Count);
          var restored = reader(default, unsafeReader);
          checkEqual(instance, restored);
        }
      }
    }

    [RdExt]
    public class GenericStructExt : RdExtReflectionBindableBase
    {
      public IViewableProperty<TextControlOverridableValue<int>> IntStruct { get; }
      public IViewableProperty<TextControlOverridableValue<MyEnum>> EnumStruct { get; }
    }

    [RdScalar]
    public struct TextControlOverridableValue<T>
    {
      public TextControlOverridableValue(T original, T @override)
      {
        Original = original;
        Override = @override;
      }
      public T Original;
      public T Override;
    }

    [RdExt]
    public class EventArgsExt : RdExtReflectionBindableBase
    {
      public IViewableProperty<MyEventArgs<int?>> Objects { get; }
    }

    [RdScalar]
    public class MyEventArgs<T> : EventArgs
    {
      public T Value;
    }

    [RdExt]
    public class ListObjectsExt : RdExtReflectionBindableBase
    {
      public IViewableProperty<ListOwner> Objects { get; }
    }

    [RdScalar] // not required
      public class ListOwner
      {
        public List<ColorFields> Polymorphic;
        public List<ColorFields[]> PolymorphicArray;
        public List<int> Ints;
      }

    [RdExt]
    public class ArraysExt : RdExtReflectionBindableBase
    {
      public IViewableProperty<int[]> IntArray { get; }
      public IViewableProperty<int[][][]> ComplexArray { get; }
      public IViewableProperty<(string, string)[]> ValueTupleArray { get; }
    }

    [RdExt]
    public class ArraysExt2 : RdExtReflectionBindableBase
    {
      public IViewableProperty<ColorFields> Property { get; }
      public IViewableProperty<ColorFields[]> PropertyArray { get; }
    }


    [RdExt]
    public class ValueTuplesExt : RdExtReflectionBindableBase
    {
      public IViewableProperty<(string, string)> SimpleTuple { get; }
      public IViewableProperty<(int, int, int, int, int, int, int, int, int, int, int, int, int)> NestedTuple { get; }
    }


    [RdExt]
    public class ColorsExt : RdExtReflectionBindableBase
    {
      public RdProperty<ColorClass> Property { get; }
      public IViewableMap<ColorFields, long> Map { get; }
      public IViewableList<ColorStruct> List { get; }
    }

    [RdScalar] // not required
      public struct ColorClass
      {
        public int Red { get; set; }
        public int Green { get; set; }
        public int Blue { get; set; }
      }

    [RdScalar] // not required
      public struct ColorStruct
      {
        public int Red { get; set; }
        public int Green { get; set; }
        public int Blue { get; set; }
      }

    [RdScalar] // not required
      public sealed class ColorFields
      {
        public int Red { get; }
        public int Green { get; }
        public int Blue { get; }

        public ColorFields(int red, int green, int blue)
        {
          Red = red;
          Green = green;
          Blue = blue;
        }

        public ColorFields()
        {
        }

        private bool Equals(ColorFields other)
        {
          return Red == other.Red;
        }

        public override bool Equals(object obj)
        {
          if (ReferenceEquals(null, obj)) return false;
          if (ReferenceEquals(this, obj)) return true;
          if (obj.GetType() != GetType()) return false;
          return Equals((ColorFields) obj);
        }

        public override int GetHashCode()
        {
          return Red;
        }
      }

    public class Scalar1
    {
      public string Name;
      public Scalar2 Value;
    }

    public class Scalar2
    {
      public Scalar2(long value, int only)
      {
        Value = value;
        SetOnly = only;
      }

      public long Value { get; set; }
      public int SetOnly { get; }
    }
  }
}