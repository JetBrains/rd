#if !NET35
using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Rd;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture] [Apartment(System.Threading.ApartmentState.STA)]
  public class ProxyGeneratorScalarInterfaceSerializerTest : RdReflectionTestBase
  {

    [Test]
    public void TestSimple()
    {
     WithExtsProxy<SimpleCalls, ISimpleCalls>((c, proxy) =>
     {
       Assert.AreEqual(2, proxy.Count(new[] {"test", null, "123"}.Where(x => x != null)));
       Assert.AreEqual(typeof(List<>).Name, proxy.GetTypeName(new HashSet<string>()));
     });
    }

    [Test]
    public void TestCustomType()
    {
      WithBothFacades(f =>
      {
        f.Serializers.Register(typeof(IMyInterface), SerializerPair.FromMarshaller(new MyInterfaceMarshaller()));
      });
      WithExtsProxy<SimpleCalls, ISimpleCalls>((c, proxy) =>
      {
        //var instanceSerializer = ((ISerializersSource)CFacade.Serializers).GetOrRegisterSerializerPair(typeof(IReadOnlyDictionary<string, string>), true);

        Assert.AreEqual(typeof(MyImpl2).Name, proxy.GetTypeName2(new MyImpl1()));
        proxy.GetTypeName3(new IMyInterface[]{new MyImpl1(), new MyImpl2()});

        Assert.AreEqual(1, proxy.ReadonlyDictionaryCount(new Dictionary<string, string>() {{"", ""}}));
      });
    }

    public class MyInterfaceMarshaller : IIntrinsicMarshaller<IMyInterface>
    {
      public IMyInterface Read(SerializationCtx ctx, UnsafeReader reader)
      {
        return new MyImpl2();
      }

      public void Write(SerializationCtx ctx, UnsafeWriter writer, IMyInterface value)
      {
      }
    }

    public interface IMyInterface
    {
    }

    public class MyImpl1 : IMyInterface
    {
      // this filed makes MyImpl1 serialization unpossible
      public Action myAction;
    } 
    public class MyImpl2 : IMyInterface { }   

    [RdRpc]
    public interface ISimpleCalls
    {
      int Count(IEnumerable<string> items);
      string GetTypeName(IEnumerable<string> items);
      string GetTypeName2(IMyInterface obj);
      string GetTypeName3(IEnumerable<IMyInterface> obj);
      int ReadonlyDictionaryCount(IReadOnlyDictionary<string, string> dict);
    }

    [RdExt]
    public class SimpleCalls : RdExtReflectionBindableBase, ISimpleCalls
    {
      public int Count(IEnumerable<string> items)
      {
        return items.Count();
      }

      public string GetTypeName(IEnumerable<string> items)
      {
        return items.GetType().Name;
      }

      public string GetTypeName3(IEnumerable<IMyInterface> obj)
      {
        return obj.GetType().Name;
      }

      public int ReadonlyDictionaryCount(IReadOnlyDictionary<string, string> dict) => dict.Count;

      public string GetTypeName2(IMyInterface obj)
      {
        return obj.GetType().Name;
      }
    }
  }
}

#endif