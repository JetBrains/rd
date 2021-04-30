using System;
using System.Linq;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class CustomGenericPrimitiveTest : RdReflectionTestBase
  {
    private Root myS;
    private Root myC;

    [RdExt]
    public sealed class Root : RdExtReflectionBindableBase
    {
      public IViewableProperty<Model> PolyProperty { get; }
    }

    [RdModel]
    public sealed class Model : RdReflectionBindableBase
    {
      public CustomReactive<int, string> Custom { get; }
    }

    public override void SetUp()
    {
      base.SetUp();
      myS = SFacade.InitBind(new Root(), TestLifetime, ClientProtocol);
      myC = CFacade.InitBind(new Root(), TestLifetime, ServerProtocol);
    }

    [Test]
    public void Test1()
    {
      var m = CFacade.Activator.Activate<Model>();
      m.Custom.t1 = 12;
      m.Custom.t2 = "test";
      myC.PolyProperty.Value = m;
      Assert.AreEqual("True:12:test",myS.PolyProperty.Value.Custom.ToString());
    }
  }
}