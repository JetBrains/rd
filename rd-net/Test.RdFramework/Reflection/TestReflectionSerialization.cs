using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using JetBrains.Rd.Tasks;
using JetBrains.Serialization;
using NUnit.Framework;
using System.Collections.Generic;
using static Test.RdFramework.Reflection.PolymorphicScalarTest;

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
      public IViewableProperty<ModelLight> ModelLight { get; }

      public ModelSample ModelMember { get; }
    }

    public override void SetUp()
    {
      base.SetUp();
      AddType(typeof(Animal));
      AddType(typeof(Bear));
      AddType(typeof(EmptyOK));
      AddType(typeof(AbstractModel));
      AddType(typeof(ConcreteModel));
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
      cm.Prop.Value = "val";
      cm.RegularFieldInModel = "f";

      c.PolyProperty.Value = animal;
      var sm = s.PolyProperty.Value.NestedRdModel;
      Assert.NotNull(sm);
      Assert.AreNotSame(sm, cm);

      cm.IList.Add(2);
      cm.List.Add("val2");
      cm.Set.Add("val3");
      cm.Map["x"] = "val";

      CollectionAssert.AreEqual(cm.IList, sm.IList);
      CollectionAssert.AreEqual(cm.List, sm.List);
      CollectionAssert.AreEqual(cm.Set, sm.Set);
      CollectionAssert.AreEqual(cm.Map, sm.Map);
      Assert.AreEqual(cm.Prop.Value, sm.Prop.Value);
      Assert.AreEqual(cm.RegularFieldInModel, sm.RegularFieldInModel);
    }

    [Test]
    public void TestReactiveListOfReactiveLists()
    {
      var s = SFacade.InitBind(new RootModel(), TestLifetime, ClientProtocol);
      var c = CFacade.InitBind(new RootModel(), TestLifetime, ServerProtocol);

      var cm = c.ModelMember;
      var sm = s.ModelMember;

      cm.Multilist.Add(CFacade.Activator.Activate<RdList<string>>());
      cm.Multilist[0].Add("123");
      cm.MultimapNonReactive["x"] = new List<string> { "123" };
      CollectionAssert.AreEqual(cm.Multilist[0], sm.Multilist[0]);
      CollectionAssert.AreEqual(cm.MultimapNonReactive["x"], sm.MultimapNonReactive["x"]);
    }

    [Test]
    public void TestNestedPolyModels()
    {
      var s = SFacade.InitBind(new RootModel(), TestLifetime, ClientProtocol);
      var c = CFacade.InitBind(new RootModel(), TestLifetime, ServerProtocol);

      var model = new ModelLight
      {
        AbstractModelField = new ConcreteModel()
      };
      c.ModelLight.Value = model;
      Assert.IsTrue(s.ModelLight.Value.AbstractModelField is ConcreteModel);
    }


    [Test]
    public void TestOptimizeNested()
    {
      var list = CFacade.Activator.Activate<RdList<string>>();
      Assert.AreEqual(true, list.OptimizeNested);

      var listInts = CFacade.Activator.Activate<RdList<int>>();
      Assert.AreEqual(true, listInts.OptimizeNested);
      var listPoly = CFacade.Activator.Activate<IViewableList<Animal>>();
      Assert.AreEqual(false, ((RdList<Animal>)listPoly).OptimizeNested);
      var listNested = CFacade.Activator.Activate<IViewableList<IViewableList<int>>>();
      Assert.AreEqual(false, ((RdList<IViewableList<int>>)listNested).OptimizeNested);

      var propertyInts = CFacade.Activator.Activate<RdProperty<int>>();
      Assert.AreEqual(true, propertyInts.OptimizeNested);
      var propertyPoly = CFacade.Activator.Activate<IViewableProperty<Animal>>();
      Assert.AreEqual(false, ((RdProperty<Animal>)propertyPoly).OptimizeNested);

      var map = CFacade.Activator.Activate<IViewableMap<int, int>>();
      Assert.AreEqual(true, ((RdMap<int, int>)map).OptimizeNested);
      var mapPoly = CFacade.Activator.Activate<RdMap<int, Animal>>();
      Assert.AreEqual(false, mapPoly.OptimizeNested);

      var set = CFacade.Activator.Activate<IViewableSet<int>>();
      Assert.AreEqual(true, ((RdSet<int>)set).OptimizeNested);
      var setPoly = CFacade.Activator.Activate<RdSet<Animal>>();
      Assert.AreEqual(false, setPoly.OptimizeNested);
    }

    [Test]
    public void TestScalarsInRdModels()
    {
      var s = SFacade.InitBind(new RootModel(), TestLifetime, ClientProtocol);
      var c = CFacade.InitBind(new RootModel(), TestLifetime, ServerProtocol);

      var serverAnimal = CFacade.Activator.Activate<Animal>();
      serverAnimal.arraysNonSerialized = new[] { "initial" };
      serverAnimal.arrays2 = new[] { "initial2" };

      s.PolyProperty.Value = serverAnimal;
      serverAnimal.arraysNonSerialized[0] = "null";
      serverAnimal.arrays2[0] = "null";

      var clientAnimal = c.PolyProperty.Value;
      Assert.AreEqual(clientAnimal.arraysNonSerialized, null); /* NonSerialized filed should not be initialized*/
      CollectionAssert.AreEqual(new[] { "initial2" }, clientAnimal.arrays2); /* other fields should serialize their content only once */
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

    [Test]
    public void TestSerializeRoundTrip()
    {
      var source = CFacade.Activator.Activate<Animal>();
      source.Identify(ClientProtocol.Identities, RdId.TestValue, true);
      source.arraysNonSerialized = new[] { "etest" };
      source.arrays2 = new[] { "serialized" };
      var recovered = SerializationRoundTrip(source);

      Assert.IsNull(recovered.arraysNonSerialized, nameof(Animal.arraysNonSerialized));
      AssertEqualNullability(source.arrays2, recovered.arrays2);
      AssertEqualNullability(source.scalar, recovered.scalar);
      AssertEqualNullability(source.NestedRdModel, recovered.NestedRdModel);
      AssertEqualNullability(source.LiveList, recovered.LiveList);
      AssertEqualNullability(source.Maps, recovered.Maps);
      AssertEqualNullability(source.Sets, recovered.Sets);
    }

    [RdModel]
    public class SingleCallBase : RdReflectionBindableBase
    {
    }

    [RdModel]
    public class SingleCall : SingleCallBase
    {
      public RdCall<ProjectInfo, ProjectInfo> Call;
    }


    [Test]
    public void TestSerializeRoundTripLigthModel()
    {
      AddType(typeof(SingleCall));
      var source = CFacade.Activator.Activate<SingleCall>();
      source.Identify(ClientProtocol.Identities, RdId.TestValue, true);
      var recovered = (SingleCall)SerializationRoundTrip<SingleCallBase>(source);

      AssertEqualNullability(source.Call, recovered.Call);
    }

    private static void AssertEqualNullability<T>(T expected, T actual)
      where T : class
    {
      Assert.AreEqual(expected == null, actual == null);
    }

    private unsafe T SerializationRoundTrip<T>(T value)
    {
      var clientSer = CFacade.Serializers.GetOrRegisterSerializerPair(typeof(T), true);
      var serverSer = SFacade.Serializers.GetOrRegisterSerializerPair(typeof(T), true);
      var ctx = new SerializationCtx(ClientProtocol);
      using var cookie = UnsafeWriter.NewThreadLocalWriter();
      var startBookmark = cookie.Writer.MakeBookmark();
      var ctxWriteDelegate = clientSer.GetWriter<T>();
      ctxWriteDelegate(ctx, cookie.Writer, value);

      var written = UnsafeReader.CreateReader(startBookmark.Data, cookie.Count);
      var recovered = serverSer.GetReader<T>()(ctx, written);

      return recovered;
    }
  }
}
