using System;
using JetBrains.Diagnostics;
using JetBrains.Rd;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class TestVerification
  {

    // These checks are disabled in release
#if DEBUG
    [TestCase(typeof(NotRdModelData))]
    // [TestCase(typeof(CantHavePrivateFieldError))]
    [TestCase(typeof(NotRdModelData))]
    [TestCase(typeof(MyEnum), Description = "Enum can't be toplevel types")]
//     [TestCase(typeof(NotSealedRdModelData))]
    [TestCase(typeof(NoBaseType))]

    // Nested RdExt not supported anymore.
    // Supporting of nested RdExt leads to complicated relations in ReSharper component container. One RdExt can be
    // injected into another, but the activation for each of RdExt should be processed independently.
    // [TestCase(typeof(CircularDependencyExtError))]
    // [TestCase(typeof(CircularDependencyExt2Error))]

    [TestCase(typeof(CircularDependencyModelError))]
    [TestCase(typeof(CircularDependencyNestedModel1Error))]
    public void TestError(Type type)
    {
      Assert.True(Mode.IsAssertion);
      var catalog = new SimpleTypesCatalog();
      var serializer = new ReflectionSerializers(catalog);
      var activator = new ReflectionRdActivator(serializer, catalog);
      var exception = Assert.Throws<Assertion.AssertionException>(() =>
      {
        var activate = activator.Activate(type);
        serializer.GetOrRegisterSerializerPair(type);
      });

      Console.WriteLine(exception);
    }
#endif

    [Test]
    public void TestActivation()
    {
      var serializer = new ReflectionSerializers(new SimpleTypesCatalog());
      var activator = new ReflectionRdActivator(serializer, null);
      var model = activator.Activate<ModelCalls>();
      Assert.NotNull(model);
      Assert.NotNull(model.Rpc1);
      Assert.NotNull(model.Rpc2);
    }

    [Test]
    public void TestActivation2()
    {
      var serializer = new ReflectionSerializers(new SimpleTypesCatalog());
      var activator = new ReflectionRdActivator(serializer, null);
      var model = activator.Activate<RootModel>();
      
      Assert.NotNull(model);
      Assert.NotNull(model.EmptyOK);
      Assert.NotNull(model.FieldsNotNullOk);
      Assert.NotNull(model.FieldsNullableOk);
      Assert.Null(model.Nested); // nested models not supported anymore
      Assert.NotNull(model.PropertiesNotNullOk);
    }
  }
}