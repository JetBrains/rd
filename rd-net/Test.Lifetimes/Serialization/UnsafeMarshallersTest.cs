using System.Collections.Generic;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.Lifetimes.Serialization
{  
  public unsafe class UnsafeMarshallersTest : LifetimesTestBase
  {
    [Test]
    public void Test1()
    {
      UnsafeReader reader;
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        cookie.Writer.Write(false);
        cookie.Writer.Write(true);
        cookie.Writer.Write((byte) 0);
        cookie.Writer.Write((byte) 10);
        cookie.Writer.Write('y');
        cookie.Writer.Write('й');
        cookie.Writer.Write(1234.5678m);
        cookie.Writer.Write(1234.5678d);
        cookie.Writer.Write((short) 1000);
        cookie.Writer.Write((int) 1001);
        cookie.Writer.Write((long) -1002);


        cookie.Writer.Write((string) null);
        cookie.Writer.Write("");
        cookie.Writer.Write("abcd = yй");

        cookie.Writer.Write((int[]) (null));
        cookie.Writer.Write(new int[0]);
        cookie.Writer.Write(new[] {1, 2, 3});

        cookie.Writer.Write(UnsafeWriter.StringDelegate, (string[]) null);
        cookie.Writer.Write(UnsafeWriter.StringDelegate, new string[0]);
        cookie.Writer.Write(UnsafeWriter.StringDelegate, new[] {"a", "b", "c"});

        cookie.Writer.Write(UnsafeWriter.StringDelegate, (List<string>) null);
        cookie.Writer.Write(UnsafeWriter.StringDelegate, new List<string>());
        cookie.Writer.Write(UnsafeWriter.StringDelegate, new List<string> {"d", "e"});

        reader = UnsafeReader.CreateReader(cookie.Data, cookie.Count);
      }

      Assert.False(reader.ReadBoolean());
      Assert.True(reader.ReadBoolean());
      Assert.AreEqual(0, reader.ReadByte());
      Assert.AreEqual(10, reader.ReadByte());
      Assert.AreEqual('y', reader.ReadChar());
      Assert.AreEqual('й', reader.ReadChar());
      Assert.AreEqual(1234.5678m, reader.ReadDecimal());
      Assert.AreEqual(1234.5678d, reader.ReadDouble(), 1e-6);
      Assert.AreEqual(1000, reader.ReadInt16());
      Assert.AreEqual(1001, reader.ReadInt32());
      Assert.AreEqual(-1002, reader.ReadInt64());

      Assert.Null(reader.ReadString());
      Assert.AreEqual("", reader.ReadString());
      Assert.AreEqual("abcd = yй", reader.ReadString());

      Assert.Null(reader.ReadIntArray());
      Assert.AreEqual(new int[0], reader.ReadIntArray());
      Assert.AreEqual(new[] {1, 2, 3}, reader.ReadIntArray());

      Assert.Null(reader.ReadArray(UnsafeReader.StringDelegate));
      Assert.AreEqual(new string[0], reader.ReadArray(UnsafeReader.StringDelegate));
      Assert.AreEqual(new[] {"a", "b", "c"}, reader.ReadArray(UnsafeReader.StringDelegate));

      Assert.Null(reader.ReadCollection(UnsafeReader.StringDelegate, n => new List<string>(n)));
      CollectionAssert.AreEqual(new List<string>(), reader.ReadCollection(UnsafeReader.StringDelegate, n => new List<string>(n)));
      CollectionAssert.AreEqual(new List<string> {"d", "e"}, reader.ReadCollection(UnsafeReader.StringDelegate, n => new List<string>(n)));
    }
  }
}