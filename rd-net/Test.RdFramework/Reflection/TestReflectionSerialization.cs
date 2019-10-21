using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class TestReflectionSerialization : RdReflectionTestBase
  {
    [RdExt]
    public sealed class RootModel : RdExtReflectionBindableBase
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

    public override void SetUp()
    {
      base.SetUp();
      TestRdTypesCatalog.Register<Animal>();
      TestRdTypesCatalog.Register<Bear>();
      TestRdTypesCatalog.Register<EmptyOK>();
    }

    [Test]
    public void Test1()
    {
      var s = new RootModel().InitBind(ReflectionRdActivator, TestLifetime, ClientProtocol);;
      var c = new RootModel().InitBind(ReflectionRdActivator, TestLifetime, ServerProtocol);

      s.EmptyOK.Value = new EmptyOK();
      Assert.IsNotNull(c.EmptyOK.Value);
    }

    [Test]
    public void TestPolymorphicProperty()
    {
      var s = ReflectionRdActivator.ActivateBind<RootModel>(TestLifetime, ClientProtocol);
      var c = ReflectionRdActivator.ActivateBind<RootModel>(TestLifetime, ServerProtocol);

      s.Primitive.Value = true;
      c.Primitive.Value = false;
      Assert.AreEqual(s.Primitive.Value, c.Primitive.Value);

      var requestBear = ReflectionRdActivator.ActivateRdExt<Bear>();
      requestBear.arrays = new string[] {"test", "test2"};

      c.PolyProperty.Value = requestBear; // (nameof(Bear), requestBear);
      var result = s.PolyProperty.Value;

      Assert.AreEqual(nameof(Bear), result.GetType().Name);
      Assert.AreNotSame(requestBear, result);
    }
  }
}