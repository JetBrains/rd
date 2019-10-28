using JetBrains.Collections.Viewable;
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
      TestRdTypesCatalog.AddType(typeof(Animal));
      TestRdTypesCatalog.AddType(typeof(Bear));
      TestRdTypesCatalog.AddType(typeof(EmptyOK));
    }

    [Test]
    public void Test1()
    {
      var s =  Facade.InitBind(new RootModel(), TestLifetime, ClientProtocol);
      var c = Facade.InitBind(new RootModel(), TestLifetime, ServerProtocol);

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

      var requestBear = ReflectionRdActivator.Activate<Bear>();

      c.PolyProperty.Value = requestBear; // (nameof(Bear), requestBear);
      var result = s.PolyProperty.Value;

      Assert.AreEqual(nameof(Bear), result.GetType().Name);
      Assert.AreNotSame(requestBear, result);
    }

    [Test]
    public void TestEnum()
    {
      WithExts<RootModel>((c, s) =>
      {
        c.PropertiesNotNullOk.Value = new PropertiesNotNullOk("First", "Second", MyEnum.Second);
        Assert.AreEqual(s.PropertiesNotNullOk.Value.First, "First");
        Assert.AreEqual(s.PropertiesNotNullOk.Value.Second, "Second");
        Assert.AreEqual(s.PropertiesNotNullOk.Value.Enum, MyEnum.Second);
      });
    }
  }
}