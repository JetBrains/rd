using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class TestReflectionSerialization : RdFrameworkTestBase
  {
    [RdExt]
    public sealed class RootModel : RdReflectionBindableBase
    {
      // public NestedModel Nested { get; }

      public IViewableProperty<bool> Primitive { get; }
      public IViewableProperty<EmptyOK> EmptyOK { get; }
      public IViewableProperty<FieldsNotNullOk> FieldsNotNullOk { get; }
      public IViewableProperty<FieldsNullableOk> FieldsNullableOk { get; }
      public IViewableProperty<PropertiesNotNullOk> PropertiesNotNullOk { get; }
      public IViewableProperty<PropertiesNullOk> PropertiesNullOk { get; }
      public IViewableProperty<Animal> PolyProperty { get; }
    }
    
    private ReflectionRdActivator myReflectionRdActivator;
    private TestRdTypesCatalog myPolymorphicRdTypesCatalog;

    protected override Serializers CreateSerializers(bool isServer)
    {
      return new Serializers(myPolymorphicRdTypesCatalog);
    }

    public override void SetUp()
    {
      var reflectionSerializers = new ReflectionSerializers();
      myPolymorphicRdTypesCatalog = new TestRdTypesCatalog(reflectionSerializers);
      myPolymorphicRdTypesCatalog.Register<Animal>();
      myPolymorphicRdTypesCatalog.Register<Bear>();
      myPolymorphicRdTypesCatalog.Register<EmptyOK>();

      myReflectionRdActivator = new ReflectionRdActivator(reflectionSerializers, myPolymorphicRdTypesCatalog as IPolymorphicTypesCatalog);

      base.SetUp();
      ServerWire.AutoTransmitMode = true;
      ClientWire.AutoTransmitMode = true;
    }

    [Test]
    public void Test1()
    {
      var s = myReflectionRdActivator.ActivateBind<RootModel>(TestLifetime, ClientProtocol);
      var c = myReflectionRdActivator.ActivateBind<RootModel>(TestLifetime, ServerProtocol);

      s.EmptyOK.Value = new EmptyOK();
      Assert.IsNotNull(c.EmptyOK.Value);
    }
    
    [Test]
    public void TestPolymorphicProperty()
    {
      var s = myReflectionRdActivator.ActivateBind<RootModel>(TestLifetime, ClientProtocol);
      var c = myReflectionRdActivator.ActivateBind<RootModel>(TestLifetime, ServerProtocol);

      // s.BindCall(s.RdCall, req => (req.model.GetType().Name, req.model));

      s.Primitive.Value = true;
      c.Primitive.Value = false;
      Assert.AreEqual(s.Primitive.Value, c.Primitive.Value);

      var requestBear = new Bear()
      {
        arrays = new string[]{"test", "test2"},
        lists = new RdList<FieldsNotNullOk>()
      };
      requestBear.PublicMorozov.Add(new KeyValuePair<string, object>("lists", requestBear.lists));

      c.PolyProperty.Value = requestBear; // (nameof(Bear), requestBear);
      var result = s.PolyProperty.Value;

      Assert.AreEqual(nameof(Bear), result.GetType().Name);
      Assert.AreNotSame(requestBear, result);
    }
  }
}