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
        c.Map.Add(new ColorFields(100,100,100), long.MaxValue);
        Assert.AreEqual(s.Map[new ColorFields(100,100,100)], c.Map.Values.First());
      });
    }

    [Test]
    public void TestColor2()
    {
      WithExts<ColorsExt>((c, s) =>
      {
        c.List.Add(new ColorStruct() { Blue = 1, Green = 2, Red = 3 });
        c.List.Add(new ColorStruct());
        CollectionAssert.AreEqual(s.List, c.List);
      });
    }

    [Test]
    public void TestColor3()
    {
      WithExts<ColorsExt>((c, s) =>
      {
        c.List.Add(new ColorStruct() { Blue = 1, Green = 2, Red = 3 });
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
        s.IntArray.Value = new[]{1,2,3};
        CollectionAssert.AreEqual(s.IntArray.Value, c.IntArray.Value);
      });
    }

    [Test, Explicit("TODO:")]
    public void TestArrays2()
    {
      WithExts<ArraysExt>((c, s) =>
      {
        s.ComplexArray.Value = new[] {new[] {new[] {1, 2, 3}}};
        CollectionAssert.AreEqual(s.ComplexArray.Value[0][0], c.ComplexArray.Value[0][0]);
      });
    }

    [Test, Explicit("TODO:")]
    public void TestArrays3()
    {
      WithExts<ArraysExt>((c, s) =>
      {
        s.ValueTupleArray.Value = new[] { ("key", "value"), ("key", "value")};
        CollectionAssert.AreEqual(s.ValueTupleArray.Value, c.ValueTupleArray.Value);
      });
    }


    [RdExt]
    public class ArraysExt : RdExtReflectionBindableBase
    {
      public IViewableProperty<int[]> IntArray { get; }
      public IViewableProperty<int[][][]> ComplexArray { get; }
      public IViewableProperty<(string, string)[]> ValueTupleArray { get; }
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
      public int Red { get;  }
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
        if (obj.GetType() != this.GetType()) return false;
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