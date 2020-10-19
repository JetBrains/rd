using System;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class PolymorphicScalarIntrinsicTest : RdReflectionTestBase
  {
    [Test]
    public void TestIntrinsicClass()
    {
      WithExts<TestExt>((c, s) =>
      {
        c.Class.Value = new Type1();
        var x = c.Class.Value; var y = s.Class.Value;
        Assert.AreEqual(x.GetType(), y.GetType());
        Assert.AreNotEqual(x.Mark, y.Mark);
        Assert.AreNotSame(x, y);
      });
    }

    [Test]
    public void TestIntrinsicInterface()
    {
      WithExts<TestExt>((c, s) =>
      {
        c.Interface.Value = new Type1();
        var x = c.Interface.Value; var y = s.Interface.Value;
        Assert.AreEqual(x.GetType(), y.GetType());
        Assert.AreNotEqual(x.Mark, y.Mark);
        Assert.AreNotSame(x, y);
      });
    }


    /// <summary>
    /// Types should use intrinsic serializer even when in the base class exists intrinsic serializer.
    /// </summary>
    [Test]
    public void TestReflectionSerializer()
    {
      WithExts<TestExt>((c, s) =>
      {
        c.Interface.Value = new TypeReflectionSerializer();
        var x = c.Interface.Value; var y = s.Interface.Value;
        Assert.AreEqual(x.GetType(), y.GetType());
        Assert.AreNotSame(x, y);
      });
    }



    public override void SetUp()
    {
      base.SetUp();
      AddType(typeof(Base));
      AddType(typeof(Type1));
      AddType(typeof(TypeReflectionSerializer));
    }


    [RdExt]
    public class TestExt : RdExtReflectionBindableBase
    {
      public RdProperty<IBase> Interface { get; }
      internal RdProperty<Type1> Class { get; }
      public RdProperty<Base[]> Array { get; }
    }


    public interface IBase
    {
      /// <summary>
      /// This Guid always change during intrinsic deserialization
      /// </summary>
      Guid Mark { get; set; }
    }

    public  class Base : IBase
    {
      public Guid Mark { get; set; }

      public static Base Read(UnsafeReader reader)
      {
        Assert.AreEqual("base", reader.ReadString());
        return new Base { Mark = Guid.NewGuid() };
      }

      public void Write(UnsafeWriter writer) => writer.Write("base");
    }

    public class Type1 : Base
    {
      public new static Type1 Read(UnsafeReader reader)
      {
        Assert.AreEqual("type1", reader.ReadString());
        return new Type1 { Mark = Guid.NewGuid() };
      }

      public new void Write(UnsafeWriter writer) => writer.Write("type1");
    }

    public class TypeReflectionSerializer : Base
    {
    }
  }
}