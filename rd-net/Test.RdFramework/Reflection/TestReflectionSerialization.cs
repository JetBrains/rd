using System.Linq;
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
      public IViewableProperty<bool> Primitive { get; }
      public IViewableProperty<EmptyOK> EmptyOK { get; }
      public IViewableProperty<ModelSample> ModelProp { get; }
      public IViewableProperty<FieldsNullableOk> FieldsNullableOk { get; }
      public IViewableProperty<PropertiesNotNullOk> PropertiesNotNullOk { get; }
      public IViewableProperty<Animal> PolyProperty { get; }
    }

    public override void SetUp()
    {
      base.SetUp();
      AddType(typeof(Animal));
      AddType(typeof(Bear));
      AddType(typeof(EmptyOK));
    }

    [Test]
    public void Test1()
    {
      var s = SFacade.InitBind(new RootModel(), TestLifetime, ClientProtocol);
      var c = CFacade.InitBind(new RootModel(), TestLifetime, ServerProtocol);

      s.EmptyOK.Value = new EmptyOK();
      Assert.IsNotNull(c.EmptyOK.Value);
    }

    [Test]
    public void TestPolymorphicProperty()
    {
      var s = SFacade.Activator.ActivateBind<RootModel>(TestLifetime, ClientProtocol);
      var c = CFacade.Activator.ActivateBind<RootModel>(TestLifetime, ServerProtocol);

      s.Primitive.Value = true;
      c.Primitive.Value = false;
      Assert.AreEqual(s.Primitive.Value, c.Primitive.Value);

      var requestBear = CFacade.Activator.Activate<Bear>();

      c.PolyProperty.Value = requestBear; // (nameof(Bear), requestBear);
      var result = s.PolyProperty.Value;

      Assert.AreEqual(nameof(Bear), result.GetType().Name);
      Assert.AreNotSame(requestBear, result);
    }

    [Test]
    public void TestNestedRdModels()
    {
      var s = SFacade.InitBind(new RootModel(), TestLifetime, ClientProtocol);
      var c = CFacade.InitBind(new RootModel(), TestLifetime, ServerProtocol);

      var animal = CFacade.Activator.Activate<Animal>();
      var cm = animal.NestedRdModel;
      cm.IList.Add(2);
      cm.Prop.Value = "val";
      cm.List.Add("val2");
      cm.Set.Add("val3");
      cm.Map["x"] = "val";
      cm.RegularFieldInModel = "f";
      
      c.PolyProperty.Value = animal;
      var sm = s.PolyProperty.Value.NestedRdModel;
      Assert.NotNull(sm);
      Assert.AreNotSame(sm, cm);

      CollectionAssert.AreEqual(cm.IList, sm.IList);
      CollectionAssert.AreEqual(cm.List, sm.List);
      CollectionAssert.AreEqual(cm.Set, sm.Set);
      CollectionAssert.AreEqual(cm.Map, sm.Map);
      Assert.AreEqual(cm.Prop.Value, sm.Prop.Value);
      Assert.AreEqual(cm.RegularFieldInModel, sm.RegularFieldInModel);
    }

    [Test]
    public void TestNestedRdModelsCircular()
    {
      var animal = CFacade.Activator.Activate<Animal>();
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