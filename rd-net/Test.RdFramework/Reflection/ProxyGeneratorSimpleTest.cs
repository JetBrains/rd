using System.Linq;
using System.Threading.Tasks;
using JetBrains.Diagnostics;
using JetBrains.dotMemoryUnit;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class ProxyGeneratorSimpleTest : RdReflectionTestBase
  {
#if NET35
    private static TaskHack Task = new TaskHack();
#endif

    [Test]
    public void TestSimple()
    {
      var proxy = SFacade.ActivateProxy<ISimpleCalls>(TestLifetime, ServerProtocol);
      SaveGeneratedAssembly();

      var client = CFacade.Activator.ActivateBind<SimpleCalls>(TestLifetime, ClientProtocol);
      // typeof(Reflection.SimpleCalls);//
      Assertion.Assert(((RdExtReflectionBindableBase)proxy).Connected.Value, "((RdReflectionBindableBase)proxy).Connected.Value");

      proxy.M();
      Assert.AreEqual(client.GetString(), proxy.GetString());
      Assert.AreEqual(client.GetInt(), proxy.GetInt());
      Assert.AreEqual(client.GetLong(), proxy.GetLong());
      Assert.AreEqual(client.ReverseString("test"), proxy.ReverseString("test"));

      Assert.AreEqual(null, proxy.GetStoredString());
      proxy.StoreString("test");
      Assert.AreEqual("test", proxy.GetStoredString());
    }

    [Test]
    public void TestSimple2()
    {
      var proxy = SFacade.ActivateProxy<IUnitTestRemoteAgent>(TestLifetime, ServerProtocol);

      var client = CFacade.Activator.ActivateBind<UnitTestRemoteAgent>(TestLifetime, ClientProtocol);
      Assertion.Assert(((RdExtReflectionBindableBase)proxy).Connected.Value, "((RdReflectionBindableBase)proxy).Connected.Value");

      proxy.RunTests(new TestRunRequest());
    }

    [Test]
    public void TestNulls()
    {
      var proxy = SFacade.ActivateProxy<IUnitTestRemoteAgent>(TestLifetime, ServerProtocol);
      var client = CFacade.Activator.ActivateBind<UnitTestRemoteAgent>(TestLifetime, ClientProtocol);
      Assertion.Assert(((RdExtReflectionBindableBase)proxy).Connected.Value, "((RdReflectionBindableBase)proxy).Connected.Value");
      proxy.RunTests(null);
      proxy.RunTests(new TestRunRequest());
    }

    [Test, Explicit]
    public void TestLeaks()
    {
      var proxy = SFacade.ActivateProxy<IUnitTestRemoteAgent>(TestLifetime, ServerProtocol);
      var client = CFacade.Activator.ActivateBind<UnitTestRemoteAgent>(TestLifetime, ClientProtocol);

      var checkpoint = dotMemory.Check();
      for (int i = 0; i < 100000; i++)
      {
        proxy.RunTests(new TestRunRequest());
      }
      dotMemory.Check(m => Assert.Less(m.GetDifference(checkpoint).GetNewObjects().SizeInBytes, 128_000));
    }

    [Test, Explicit]
    public void TestLeaksFromSerializers()
    {
      var checkpoint = dotMemory.Check();
      for (int i = 0; i < 100000; i++)
      {
        var serializers = new Serializers();
      }
      dotMemory.Check(m => Assert.Less(m.GetDifference(checkpoint).GetNewObjects().SizeInBytes, 128_000));
    }


    [RdRpc]
    public interface ISimpleCalls
    {
      void M();
      string GetString();
      string ReverseString(string input);
      int GetInt();
      long GetLong();
      void StoreString(string input);
      string GetStoredString();
    }

    [RdRpc]
    public interface IUnitTestRemoteAgent
    {
      Task RunTests(TestRunRequest request);
    }

    [RdExt]
    public class UnitTestRemoteAgent : RdExtReflectionBindableBase, IUnitTestRemoteAgent
    {
      public Task RunTests(TestRunRequest request) => Task.CompletedTask;
    }
    public class TestRunRequest
    {
      public TypeWithBuiltInSerializer Val;

      public sealed class TypeWithBuiltInSerializer
      {
        public static TypeWithBuiltInSerializer Read(UnsafeReader reader) => new();
        public void Write(UnsafeWriter writer)
        {
        }
      }
    }

    [RdExt]
    public class SimpleCalls : RdExtReflectionBindableBase, ISimpleCalls
    {
      // Non Serialized not required in RdExt
      // [NonSerialized]
      private string myString;

      public void M() { }

      public string GetString() => "Hello world!";
      public int GetInt() => 28000;
      public long GetLong() => 14000;

      public void StoreString(string input)
      {
        myString = input;
      }

      public string GetStoredString()
      {
        return myString;
      }

      public string ReverseString(string input) => new string(input.Reverse().ToArray());
    }

    /* Expected Generated Proxy 
    [RdExt]
    internal sealed class SimpleCalls : RdReflectionBindableBase, ProxyGeneratorTests.ISimpleCalls, IProxyTypeMarker
    {
      public IRdCall<Unit, Unit> M_proxy;
      public IRdCall<Unit, string> GetString_proxy;
      public IRdCall<ValueTuple<string>, string> ReverseString_proxy;
      public IRdCall<Unit, int> GetInt_proxy;
      public IRdCall<Unit, long> GetLong_proxy;

      void ProxyGeneratorTests.ISimpleCalls.M()
      {
        M_proxy.Sync(Unit.Instance, null);
      }

      string ProxyGeneratorTests.ISimpleCalls.GetString()
      {
        return GetString_proxy.Start(Unit.Instance, SynchronousScheduler.Instance).Result.Value.Result;
        // var startAsTaskSyncResponse = GetString_proxy.StartAsTask_SyncResponse(Unit.Instance);
        // return startAsTaskSyncResponse.Result;
      }

      string ProxyGeneratorTests.ISimpleCalls.ReverseString(string obj0)
      {
        return ReverseString_proxy.Sync(new ValueTuple<string>(obj0), null);
      }

      int ProxyGeneratorTests.ISimpleCalls.GetInt()
      {
        return GetInt_proxy.Sync(Unit.Instance, null);
      }

      long ProxyGeneratorTests.ISimpleCalls.GetLong()
      {
        return GetInt_proxy.Sync(Unit.Instance, null);
      }
    }
    */
  }
}