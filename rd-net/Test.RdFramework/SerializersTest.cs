using JetBrains.Rd;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using NUnit.Framework;
using Test.Lifetimes;

namespace Test.RdFramework
{
  [TestFixture]
  public class SerializersTest : LifetimesTestBase
  {
    [Test]
    public void TestPolymorphicSimple()
    {
      var serializers = new Serializers();
      serializers.Register(MyTestObject.Read, MyTestObject.Write);
      var serializationCtx = new SerializationCtx(serializers, new SequentialIdentities(IdKind.Client));
      var testObject = new MyTestObject("Monomorphic");

      byte[] data;
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        serializers.Write(serializationCtx, cookie.Writer, testObject);
        data = cookie.CloneData();
      }

      MyTestObject newTestObject = null;
      UnsafeReader.With(data, reader => newTestObject = serializers.Read<MyTestObject>(serializationCtx, reader, null));
      Assert.AreEqual(testObject.Data, newTestObject.Data);
    }
    
    private class MyTestObject
    {
      public string Data;
      
      public MyTestObject(string data) { Data = data; }

      public static CtxReadDelegate<MyTestObject> Read = (ctx, reader) =>
      {
        var data = reader.ReadString();
        return new MyTestObject(data);
      };

      public static CtxWriteDelegate<MyTestObject> Write = (ctx, writer, value) => { writer.WriteString(value.Data); };
    }
  }
}