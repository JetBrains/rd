using System.Collections.Generic;
using System.Linq;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
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
      TestRdTypesCatalog.Register<ColorFields>();
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
          // ReadonlyListInts = new List<int>() {1, 2, 3},
          // EnumerableInts = new List<int>() {1, 2, 3},
        };

        CollectionAssert.AreEqual(c.Objects.Value.InterfaceListOfInts , s.Objects.Value.InterfaceListOfInts );
        CollectionAssert.AreEqual(c.Objects.Value.ListOfInts          , s.Objects.Value.ListOfInts          );
        CollectionAssert.AreEqual(c.Objects.Value.CollectionOfInts    , s.Objects.Value.CollectionOfInts    );
        // CollectionAssert.AreEqual(c.Objects.Value.ReadonlyListInts    , s.Objects.Value.ReadonlyListInts    );
        // CollectionAssert.AreEqual(c.Objects.Value.EnumerableInts      , s.Objects.Value.EnumerableInts      );
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
        // not supported
        // public IReadOnlyList<int> ReadonlyListInts;
        // public IEnumerable<int> EnumerableInts;
      }
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
      public class ColorFields
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

        protected bool Equals(ColorFields other)
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