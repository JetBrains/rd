using System;
using JetBrains.Diagnostics;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class TestVerification
  {
    [TestCase(typeof(NotRdModelData))]
    [TestCase(typeof(CantHaveNonRdError))]
    // [TestCase(typeof(CantHavePrivateFieldError))]
    [TestCase(typeof(NotRdModelData))]
    [TestCase(typeof(MyEnum), Description = "Enum can't be toplevel types")]
//     [TestCase(typeof(NotSealedRdModelData))]
    [TestCase(typeof(NoBaseType))]
    // [TestCase(typeof(CircularDependencyExtError))]
    // [TestCase(typeof(CircularDependencyExt2Error))]
    [TestCase(typeof(ModelCalls.ModelInvalidCalls))]
    // [TestCase(typeof(CircularDependencyInModelError))]
    public void TestError(Type type)
    {
      var serializer = new ReflectionSerializersFactory(new SimpleTypesCatalog());
      var activator = new ReflectionRdActivator(serializer, null);
      var exception = Assert.Throws<Assertion.AssertionException>(() => activator.Activate(type));

      Console.WriteLine(exception);
    }


    public void TestActivation()
    {
      var serializer = new ReflectionSerializersFactory(new SimpleTypesCatalog());
      var activator = new ReflectionRdActivator(serializer, null);
      var model = activator.Activate<ModelCalls>();
      Assert.NotNull(model);
      Assert.NotNull(model.Rpc1);
      Assert.NotNull(model.Rpc2);
    }

    public void TestActivation2()
    {
      var serializer = new ReflectionSerializersFactory(new SimpleTypesCatalog());
      var activator = new ReflectionRdActivator(serializer, null);
      var model = activator.Activate<RootModel>();
      
      Assert.NotNull(model);
      Assert.NotNull(model.EmptyOK);
      Assert.NotNull(model.FieldsNotNullOk);
      Assert.NotNull(model.FieldsNullableOk);
      Assert.Null(model.Nested); // nested models not supported anymore
      Assert.NotNull(model.PropertiesNotNullOk);
      Assert.NotNull(model.PropertiesNullOk);
    }
  }
}