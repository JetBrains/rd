using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ScalarIntrinsicTests : RdReflectionTestBase
  {
    [Test]
    public void TestReadWriteMethods()
    {
      WithExts<IntrinsicExt1>((c, s) =>
      {
        c.Simple.Value = new NoRedIntrinsic1(1,1,1);
        Assert.AreEqual(0, s.Simple.Value.Red);
        Assert.AreEqual(1, s.Simple.Value.Green);
        Assert.AreEqual(1, s.Simple.Value.Blue);
      });
    }

    [Test]
    public void TestReadWriteFields()
    {
      WithExts<IntrinsicExt2>((c, s) =>
      {
        c.Simple.Value = new NoRedIntrinsic2(1,1,1);
        Assert.AreEqual(0, s.Simple.Value.Red);
        Assert.AreEqual(1, s.Simple.Value.Green);
        Assert.AreEqual(1, s.Simple.Value.Blue);
      });
    }

    [Test]
    public void TestRdScalarAttribute()
    {
      WithExts<IntrinsicExt3>((c, s) =>
      {
        c.Simple.Value = new NoRedIntrinsic3(1,1,1);
        Assert.AreEqual(0, s.Simple.Value.Red);
        Assert.AreEqual(1, s.Simple.Value.Green);
        Assert.AreEqual(1, s.Simple.Value.Blue);
      });
    }

    [Test]
    public void TestExternalSerialization()
    {
      ReflectionSerializersFactory.Cache.Register(
        (ctx, reader) => new ScalarTests.ColorFields(0, reader.ReadByte(), reader.ReadByte()),
        (ctx, writer, value) =>
        {
          writer.Write((byte) value.Green);
          writer.Write((byte) value.Blue);
        });
      WithExts<IntrinsicExt4>((c, s) =>
      {
        c.Simple.Value = new ScalarTests.ColorFields(1,1,1);
        Assert.AreEqual(0, s.Simple.Value.Red);
        Assert.AreEqual(1, s.Simple.Value.Green);
        Assert.AreEqual(1, s.Simple.Value.Blue);
      });
    }

    [RdExt]
    public class IntrinsicExt1 : RdExtReflectionBindableBase
    {
      public IViewableProperty<NoRedIntrinsic1> Simple { get; }
    }

    [RdExt]
    public class IntrinsicExt2 : RdExtReflectionBindableBase
    {
      public IViewableProperty<NoRedIntrinsic2> Simple { get; }
    }

    [RdExt]
    public class IntrinsicExt3 : RdExtReflectionBindableBase
    {
      public IViewableProperty<NoRedIntrinsic3> Simple { get; }
    }

    [RdExt]
    public class IntrinsicExt4 : RdExtReflectionBindableBase
    {
      public IViewableProperty<ScalarTests.ColorFields> Simple { get; }
    }


    /// <summary>
    /// Intrinsic way 1: special fields
    /// </summary>
    [RdScalar] // not required
    public class NoRedIntrinsic1
    {
      public int Red { get;  }
      public int Green { get; }
      public int Blue { get; }

      public NoRedIntrinsic1(int red, int green, int blue)
      {
        Red = red;
        Green = green;
        Blue = blue;
      }

      public static CtxReadDelegate<NoRedIntrinsic1> Read = (ctx, reader) => new NoRedIntrinsic1(0/*reader.ReadByte()*/, reader.ReadByte(), reader.ReadByte());
      public static CtxWriteDelegate<NoRedIntrinsic1> Write = (ctx, writer, value) =>
      {
        // writer.Write((byte) value.Red);
        writer.Write((byte) value.Green);
        writer.Write((byte) value.Blue);
      };
    }

    /// <summary>
    /// Intrinsic way 2: magic static methods
    /// </summary>
    [RdScalar] // not required
    public class NoRedIntrinsic2
    {
      public int Red { get;  }
      public int Green { get; }
      public int Blue { get; }

      public NoRedIntrinsic2(int red, int green, int blue)
      {
        Red = red;
        Green = green;
        Blue = blue;
      }

      public static NoRedIntrinsic2 Read(SerializationCtx ctx, UnsafeReader reader)
      {
        return new NoRedIntrinsic2(0 /*reader.ReadByte()*/, reader.ReadByte(), reader.ReadByte());
      }

      public static void Write(SerializationCtx ctx, UnsafeWriter writer, NoRedIntrinsic2 value)
      {
        // writer.Write((byte) value.Red);
        writer.Write((byte) value.Green);
        writer.Write((byte) value.Blue);
      }
    }

    /// <summary>
    /// Intrinsic way 3: marshallers
    /// </summary>
    [RdScalar(typeof(Marshaller))]
    public class NoRedIntrinsic3
    {
      public int Red { get;  }
      public int Green { get; }
      public int Blue { get; }

      public NoRedIntrinsic3(int red, int green, int blue)
      {
        Red = red;
        Green = green;
        Blue = blue;
      }
    }

    public class Marshaller : IIntrinsicMarshaller<NoRedIntrinsic3>
    {
      public NoRedIntrinsic3 Read(SerializationCtx ctx, UnsafeReader reader)
      {
        return new NoRedIntrinsic3(0 /*reader.ReadByte()*/, reader.ReadByte(), reader.ReadByte());
      }

      public void Write(SerializationCtx ctx, UnsafeWriter writer, NoRedIntrinsic3 value)
      {
        // writer.Write((byte) value.Red);
        writer.Write((byte) value.Green);
        writer.Write((byte) value.Blue);
      }
    }
  }
}