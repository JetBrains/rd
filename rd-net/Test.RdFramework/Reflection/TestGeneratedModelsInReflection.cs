using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;
using NUnit.Framework;
using Test.RdFramework.Reflection.Generated;

namespace Test.RdFramework.Reflection
{
  /// <summary>
  /// In Reflection RdExt root it is possible to use generated models from DSL-based generated models
  /// </summary>
  public class TestGeneratedModelsInReflection : RdReflectionTestBase
  {
    [RdExt]
    public class ReflectionRoot : RdExtReflectionBindableBase
    {
      /// <summary>
      /// Open class is defined in Kotlin DSL and imported from `Generated` folder
      /// </summary>
      public IViewableProperty<OpenClass> Val { get; }
    }

    [Test]
    public void TestLiveModels()
    {
      WithExts<ReflectionRoot>((c, s) =>
      {
        c.Val.Value = new OpenClass("testField");
        c.Val.Value.String.Value = "Test live models"; // live propoperty

        Assert.AreEqual(c.Val.Value.String.Value, s.Val.Value.String.Value);
        Assert.AreEqual(c.Val.Value.Field, s.Val.Value.Field);
      });
    }

    /// <summary> 
    /// When using polymorphic models it is your responsibility to register all inheritors.
    /// Registration for statically used classes in Ext is not required.
    /// </summary>
    [Test]
    public void TestLiveModelsPolymorphic()
    {
      AddType(typeof(OpenClass_Unknown));

      WithExts<ReflectionRoot>((c, s) =>
      {
        c.Val.Value = new OpenClass_Unknown("testField");
        c.Val.Value.String.Value = "Test live models";
        Assert.AreEqual(c.Val.Value.GetType(), s.Val.Value.GetType());
        Assert.AreEqual(c.Val.Value.String.Value, s.Val.Value.String.Value);
        Assert.AreEqual(c.Val.Value.Field, s.Val.Value.Field);
      });
    }
  }
}