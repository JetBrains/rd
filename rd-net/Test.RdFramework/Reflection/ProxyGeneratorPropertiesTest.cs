using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class ProxyGeneratorPropertiesTest : RdReflectionTestBase
  {
    [RdRpc]
    public interface IPropertiesTest
    {
      IViewableProperty<string> RdProperty { get; }
      ISignal<string> Signal { get; }
      IViewableSet<LifeModel> Set { get; }
      IViewableList<LifeModel> List { get; }
      IViewableMap<LifeModel, int> Counter { get; }
    }

    [RdExt]
    public class PropertiesTest : RdExtReflectionBindableBase, IPropertiesTest
    {
      public IViewableProperty<string> RdProperty { get; private set; }

      public ISignal<string> Signal { get; }
      public IViewableSet<LifeModel> Set { get; }
      public IViewableList<LifeModel> List { get; }
      public IViewableMap<LifeModel, int> Counter { get; }
    }

    [RdModel]
    public sealed class LifeModel : RdReflectionBindableBase
    {
      public IViewableProperty<string> StrProperty { get; }

      public LifeModel()
      {
        StrProperty = new RdProperty<string>(Serializers.ReadString, Serializers.WriteString);
      }
    }

    [Test]
    public void TestProperties()
    {
      // SaveGeneratedAssembly();

      var client = CFacade.Activator.ActivateBind<PropertiesTest>(TestLifetime, ClientProtocol);
      var proxy = SFacade.ActivateProxy<IPropertiesTest>(TestLifetime, ServerProtocol);
      Assertion.Assert(((RdExtReflectionBindableBase)proxy).Connected.Value, "((RdReflectionBindableBase)proxy).Connected.Value");

      AddType(typeof(LifeModel));
      // test signals
      bool raised = false;
      proxy.Signal.Advise(TestLifetime, s => raised = true);
      client.Signal.Fire("test");
      Assertion.Assert(raised, "!raised");

      // test life models
      client.List.Add(new LifeModel());
      Assert.True(proxy.List.Count == 1, "client.List.Count == 1");
      client.List[0].StrProperty.Value = "Hello From Rd";
      Assert.AreEqual(proxy.List[0].StrProperty.Value, client.List[0].StrProperty.Value);
    }


    public interface IPartSync
    {
      IViewableProperty<string> RdProperty { get; }
    }

    [RdExt(typeof(IPartSync))]
    public class PartSync : RdExtReflectionBindableBase, IPartSync
    {
      public IViewableProperty<string> RdProperty { get; private set; }
      
      // this signal should throw exception about useless bindable member
      public ISignal<string> Signal { get; }
    }

    [Test]
    public void TestOnlyRpcAreSynchronized()
    {
      var client = CFacade.InitBind(new PartSync(), TestLifetime, ClientProtocol);
      var proxy = SFacade.ActivateProxy<IPartSync>(TestLifetime, ServerProtocol);
      Assertion.Assert(((RdExtReflectionBindableBase)proxy).Connected.Value, "((RdReflectionBindableBase)proxy).Connected.Value");
    }
  }
}