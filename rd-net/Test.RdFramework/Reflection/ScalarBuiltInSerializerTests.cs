using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;
using NUnit.Framework;
using static JetBrains.Rd.Reflection.BuiltInSerializers.BuiltInType;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ScalarBuiltInSerializerTests : RdReflectionTestBase
  {
    [Test]
    public void TestAllMarkedBuiltInTypes()
    {
      var additionalTypes = new Dictionary<Type, BuiltInSerializers.BuiltInType> 
      {
        { typeof(RdMap<int, int>), ProtocolCollectionLike2 },
        { typeof(RdList<int>), ProtocolCollectionLike1 }
      };
      foreach (var type in typeof(ScalarBuiltInSerializerTests).Assembly.GetTypes().Concat(additionalTypes.Keys))
      {
        var expectedType = type.GetCustomAttribute<AssertBuiltInTypeAttribute>()?.BuiltInType;
        if (additionalTypes.TryGetValue(type, out var knowType))
          expectedType = knowType;

        if (expectedType.HasValue)
        {
          var builtInType = BuiltInSerializers.GetBuiltInType(type.GetTypeInfo());
          if (builtInType != expectedType)
          {
            Assert.AreEqual(expectedType, builtInType, "Unexpected built-in serializer type detected for type {0}", type);
          }
        }
      }
    }


    [Test]
    public void TestReadWriteMethods()
    {
      WithExts<Ext1>((c, s) =>
      {
        c.Simple.Value = new NoRedBuiltIn1(1,1,1);
        Assert.AreEqual(0, s.Simple.Value.Red);
        Assert.AreEqual(1, s.Simple.Value.Green);
        Assert.AreEqual(1, s.Simple.Value.Blue);
      });
    }

    [Test]
    public void TestReadWriteFields()
    {
      WithExts<Ext2>((c, s) =>
      {
        c.Simple.Value = new NoRedBuiltIn2(1,1,1);
        Assert.AreEqual(0, s.Simple.Value.Red);
        Assert.AreEqual(1, s.Simple.Value.Green);
        Assert.AreEqual(1, s.Simple.Value.Blue);
      });
    }

    [Test]
    public void TestRdScalarAttribute()
    {
      WithExts<Ext3>((c, s) =>
      {
        c.Simple.Value = new NoRedBuiltIn3(1,1,1);
        Assert.AreEqual(0, s.Simple.Value.Red);
        Assert.AreEqual(1, s.Simple.Value.Green);
        Assert.AreEqual(1, s.Simple.Value.Blue);
      });
    }

    [Test]
    public void TestRdSimpleMethods()
    {
      WithExts<Ext4>((c, s) =>
      {
        c.Simple.Value = new NoRedBuiltIn4<int, int>(1,1,1);
        Assert.AreEqual(0, s.Simple.Value.Red);
        Assert.AreEqual(1, s.Simple.Value.Green);
        Assert.AreEqual(1, s.Simple.Value.Blue);
      });
    }

    [Test]
    public void TestRdSimpleMethodsStaticWrite()
    {
      WithExts<Ext5>((c, s) =>
      {
        c.Simple.Value = new OuterClass<Unit>.NoRedBuiltIn5(1,1,1);
        Assert.AreEqual(0, s.Simple.Value.Red);
        Assert.AreEqual(1, s.Simple.Value.Green);
        Assert.AreEqual(1, s.Simple.Value.Blue);
      });
    }


    [Test]
    public void TestExternalSerialization()
    {
      void Reg(ReflectionSerializers cache)
      {
        cache.Register(
          (ctx, reader) => new ScalarTests.ColorFields(0, reader.ReadByte(), reader.ReadByte()),
          (ctx, writer, value) =>
          {
            writer.Write((byte) value.Green);
            writer.Write((byte) value.Blue);
          });
      }

      Reg(CFacade.Serializers);
      Reg(SFacade.Serializers);

      WithExts<RegularExt>((c, s) =>
      {
        c.Simple.Value = new ScalarTests.ColorFields(1,1,1);
        Assert.AreEqual(0, s.Simple.Value.Red);
        Assert.AreEqual(1, s.Simple.Value.Green);
        Assert.AreEqual(1, s.Simple.Value.Blue);
      });
    }

    [RdExt]
    public class Ext1 : RdExtReflectionBindableBase
    {
      public IViewableProperty<NoRedBuiltIn1> Simple { get; }
    }

    [RdExt]
    public class Ext2 : RdExtReflectionBindableBase
    {
      public IViewableProperty<NoRedBuiltIn2> Simple { get; }
    }

    [RdExt]
    public class Ext3 : RdExtReflectionBindableBase
    {
      public IViewableProperty<NoRedBuiltIn3> Simple { get; }
    }

    [RdExt]
    public class Ext4 : RdExtReflectionBindableBase
    {
      public IViewableProperty<NoRedBuiltIn4<int, int>> Simple { get; }
    }
    [RdExt]
    public class Ext5 : RdExtReflectionBindableBase
    {
      public IViewableProperty<OuterClass<Unit>.NoRedBuiltIn5> Simple { get; }
    }


    [RdExt]
    public class RegularExt : RdExtReflectionBindableBase
    {
      public IViewableProperty<ScalarTests.ColorFields> Simple { get; }
    }


    /// <summary>
    /// Built-in serializers way 1: special fields
    /// </summary>
    [RdScalar, AssertBuiltInType(StaticFields)] // not required
    public class NoRedBuiltIn1
    {
      public int Red { get;  }
      public int Green { get; }
      public int Blue { get; }

      public NoRedBuiltIn1(int red, int green, int blue)
      {
        Red = red;
        Green = green;
        Blue = blue;
      }

      public static CtxReadDelegate<NoRedBuiltIn1> Read = (ctx, reader) => new NoRedBuiltIn1(0/*reader.ReadByte()*/, reader.ReadByte(), reader.ReadByte());
      public static CtxWriteDelegate<NoRedBuiltIn1> Write = (ctx, writer, value) =>
      {
        // writer.Write((byte) value.Red);
        writer.Write((byte) value.Green);
        writer.Write((byte) value.Blue);
      };
    }

    /// <summary>
    /// Built-in serializers way 2: magic static methods
    /// </summary>
    [RdScalar, AssertBuiltInType(StaticProtocolMethods)] // not required
    public class NoRedBuiltIn2
    {
      public int Red { get;  }
      public int Green { get; }
      public int Blue { get; }

      public NoRedBuiltIn2(int red, int green, int blue)
      {
        Red = red;
        Green = green;
        Blue = blue;
      }

      public static NoRedBuiltIn2 Read(SerializationCtx ctx, UnsafeReader reader)
      {
        return new NoRedBuiltIn2(0 /*reader.ReadByte()*/, reader.ReadByte(), reader.ReadByte());
      }

      public static void Write(SerializationCtx ctx, UnsafeWriter writer, NoRedBuiltIn2 value)
      {
        // writer.Write((byte) value.Red);
        writer.Write((byte) value.Green);
        writer.Write((byte) value.Blue);
      }
    }

    /// <summary>
    /// Built-in serializers way 3: marshallers
    /// </summary>
    [RdScalar(typeof(Marshaller)), AssertBuiltInType(MarshallerAttribute)]
    public class NoRedBuiltIn3
    {
      public int Red { get;  }
      public int Green { get; }
      public int Blue { get; }

      public NoRedBuiltIn3(int red, int green, int blue)
      {
        Red = red;
        Green = green;
        Blue = blue;
      }
    }

    /// <summary>
    /// Built-in serializers way 4: implemented directly in type via Non-protocol methods with unsafe writers
    /// </summary>
    [AssertBuiltInType(Methods)]
    public class NoRedBuiltIn4<T1/*optional*/, T2/*optional*/>
    {
      public int Red { get;  }
      public int Green { get; }
      public int Blue { get; }

      public NoRedBuiltIn4(int red, int green, int blue)
      {
        Red = red;
        Green = green;
        Blue = blue;
      }

      public static NoRedBuiltIn4<T1, T2> Read(UnsafeReader reader)
      {
        return new NoRedBuiltIn4<T1, T2>(0 /*reader.ReadByte()*/, reader.ReadByte(), reader.ReadByte());
      }

      public void Write(UnsafeWriter writer)
      {
        // writer.Write((byte) value.Red);
        writer.Write((byte) Green);
        writer.Write((byte) Blue);
      }
    }

    public class OuterClass<T>
    {
      /// <summary>
      /// Built-in serializers way 5: implemented directly in type via Non-protocol methods with unsafe writers, but write method can be static
      /// </summary>
      [AssertBuiltInType(StaticMethods)]
      public record NoRedBuiltIn5(int Red, int Green, int Blue)
      {
        public static NoRedBuiltIn5 Read(UnsafeReader reader)
        {
          return new NoRedBuiltIn5(0 /*reader.ReadByte()*/, reader.ReadByte(), reader.ReadByte());
        }

        public static void Write(UnsafeWriter writer, NoRedBuiltIn5 value)
        {
          // writer.Write((byte) value.Red);
          writer.Write((byte)value.Green);
          writer.Write((byte)value.Blue);
        }

        public virtual bool Equals(NoRedBuiltIn5 other)
        {
          if (ReferenceEquals(null, other)) return false;
          if (ReferenceEquals(this, other)) return true;
          return Red == other.Red && Green == other.Green && Blue == other.Blue;
        }

        public override int GetHashCode()
        {
          unchecked
          {
            var hashCode = Red;
            hashCode = (hashCode * 397) ^ Green;
            hashCode = (hashCode * 397) ^ Blue;
            return hashCode;
          }
        }
      }
    }

    public class Marshaller : IBuiltInMarshaller<NoRedBuiltIn3>
    {
      public NoRedBuiltIn3 Read(SerializationCtx ctx, UnsafeReader reader)
      {
        return new NoRedBuiltIn3(0 /*reader.ReadByte()*/, reader.ReadByte(), reader.ReadByte());
      }

      public void Write(SerializationCtx ctx, UnsafeWriter writer, NoRedBuiltIn3 value)
      {
        // writer.Write((byte) value.Red);
        writer.Write((byte) value.Green);
        writer.Write((byte) value.Blue);
      }
    }
  }
}