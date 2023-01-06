using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd.Reflection;
using NUnit.Framework;
using Test.RdFramework.Reflection;

namespace Test.RdFramework
{
  [TestFixture]
  public class RdOuterLifetimeTest: RdReflectionTestBase
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
      public RdOuterLifetime OuterLifetime { get; }
    }

    public override void SetUp()
    {
      base.SetUp();
      myS = SFacade.InitBind(new Root(), TestLifetime, ClientProtocol);
      myC = CFacade.InitBind(new Root(), TestLifetime, ServerProtocol);
    }

    [Test]
    public void Test01()
    {
      Lifetime.Using(lifetime =>
      {
        bool sTerminated = false;
        var m = CFacade.Activator.Activate<Model>();
        myC.PolyProperty.Value = m;
        var ld2 = OuterLifetime.DefineIntersection(lifetime, myS.PolyProperty.Value.OuterLifetime);
        ld2.Lifetime.OnTermination(() => sTerminated = true);

        Lifetime.Using(outerLifetime => m.OuterLifetime.AttachToLifetime(outerLifetime));

        Assert.IsTrue(sTerminated, "sTerminated");
      });
    }

    [Test]
    public void TestTerminatedLifetime01()
    {
      Lifetime.Using(lifetime =>
      {
        bool sTerminated = false;
        var m = CFacade.Activator.Activate<Model>();
        myC.PolyProperty.Value = m;
        var ld2 = OuterLifetime.DefineIntersection(lifetime, myS.PolyProperty.Value.OuterLifetime);
        ld2.Lifetime.OnTermination(() => sTerminated = true);

        var lifetimeDefinition = Lifetime.Define(lifetime);
        lifetimeDefinition.Terminate();
        m.OuterLifetime.AttachToLifetime(lifetimeDefinition.Lifetime);
        Assert.IsTrue(sTerminated, "sTerminated");
      });
    }
  }
}