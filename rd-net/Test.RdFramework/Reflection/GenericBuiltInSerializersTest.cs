using System;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework.Reflection;

[TestFixture]
public unsafe class GenericBuiltInSerializersTest : RdReflectionTestBase
{
  [Test]
  public void TestClass()
  {
    var originalType = new TypeWithCtx<int, int>(Guid.NewGuid(), "some");
    var readType = SerializeRoundTrip(originalType);

    Assert.AreEqual(originalType.Mark, readType.Mark);
    Assert.AreEqual(null, readType.AlwaysLost);
  }

  [Test]
  public void TestNoCtx()
  {
    var originalType = new TypeWithoutCtx<int, int>(Guid.NewGuid(), "some");
    var readType = SerializeRoundTrip(originalType);

    Assert.AreEqual(originalType.Mark, readType.Mark);
    Assert.AreEqual(null, readType.AlwaysLost);
  }

  [Test]
  public void TestClass3()
  {
    var originalType = new TypeWithCtx3<int, int, int>(Guid.NewGuid(), "some");
    var readType = SerializeRoundTrip(originalType);

    Assert.AreEqual(originalType.Mark, readType.Mark);
    Assert.AreEqual(null, readType.AlwaysLost);
  }

  [Test]
  public void TestNoCtx3()
  {
    var originalType = new TypeWithoutCtx3<int, int, int>(Guid.NewGuid(), "some");
    var readType = SerializeRoundTrip(originalType);

    Assert.AreEqual(originalType.Mark, readType.Mark);
    Assert.AreEqual(null, readType.AlwaysLost);
  }


  private T SerializeRoundTrip<T>(T originalType)
  {
    var serializers = CFacade.Serializers;
    using var cookie = UnsafeWriter.NewThreadLocalWriter();

    var serializationCtx = new SerializationCtx(serializers, new SequentialIdentities(IdKind.Client));
    serializers.Write(serializationCtx, cookie.Writer, originalType);
    var reader = UnsafeReader.CreateReader(cookie.Data, cookie.Count);
    return serializers.Read<T>(serializationCtx, reader);
  }

  public class TypeWithCtx<T1, T2>
  {
    public Guid Mark { get; }
    public string AlwaysLost { get; }

    public TypeWithCtx(Guid mark, string alwaysLost)
    {
      Mark = mark;
      AlwaysLost = alwaysLost;
    }

    public void Write(SerializationCtx ctx, UnsafeWriter writer) => writer.WriteGuid(Mark);
    public static TypeWithCtx<T1, T2> Read(SerializationCtx ctx, UnsafeReader reader) => new(reader.ReadGuid(), null);
  }

  public class TypeWithoutCtx<T1, T2>
  {
    public Guid Mark { get; }
    public string AlwaysLost { get; }

    public TypeWithoutCtx(Guid mark, string alwaysLost)
    {
      Mark = mark;
      AlwaysLost = alwaysLost;
    }

    public void Write(UnsafeWriter writer) => writer.WriteGuid(Mark);
    public static TypeWithoutCtx<T1, T2> Read(UnsafeReader reader) => new(reader.ReadGuid(), null);
  }



  public class TypeWithCtx3<T1, T2, T3>
  {
    public Guid Mark { get; }
    public string AlwaysLost { get; }

    public TypeWithCtx3(Guid mark, string alwaysLost)
    {
      Mark = mark;
      AlwaysLost = alwaysLost;
    }

    public void Write(SerializationCtx ctx, UnsafeWriter writer) => writer.WriteGuid(Mark);
    public static TypeWithCtx3<T1, T2, T3> Read(SerializationCtx ctx, UnsafeReader reader) => new(reader.ReadGuid(), null);
  }

  public class TypeWithoutCtx3<T1, T2, T3>
  {
    public Guid Mark { get; }
    public string AlwaysLost { get; }

    public TypeWithoutCtx3(Guid mark, string alwaysLost)
    {
      Mark = mark;
      AlwaysLost = alwaysLost;
    }

    public void Write(UnsafeWriter writer) => writer.WriteGuid(Mark);
    public static TypeWithoutCtx3<T1, T2, T3> Read(UnsafeReader reader) => new(reader.ReadGuid(), null);
  }

}