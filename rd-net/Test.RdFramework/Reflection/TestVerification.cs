using System;
using JetBrains.Diagnostics;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class TestVerification
  {
    [Ignore("Fails")]
    [TestCase(typeof(NotRdModelData))]
    [TestCase(typeof(CantHaveNonRdError))]
    // [TestCase(typeof(CantHavePrivateFieldError))]
    [TestCase(typeof(NotRdModelData))]
    [TestCase(typeof(MyEnum), Description = "Enum can't be toplevel types")]
    [TestCase(typeof(ModelCantHaveNullableError))]
    [TestCase(typeof(ModelCantHaveFieldPropError))]
//     [TestCase(typeof(NotSealedRdModelData))]
    [TestCase(typeof(NoBaseType))]
    [TestCase(typeof(CircularDependencyExtError))]
    [TestCase(typeof(CircularDependencyExt2Error))]
    [TestCase(typeof(ModelCalls.ModelInvalidCalls))]
    // [TestCase(typeof(CircularDependencyInModelError))]
    public void TestError(Type type)
    {
      var serializer = new ReflectionSerializersFactory();
      var activator = new ReflectionRdActivator(serializer, null);
      var exception = Assert.Throws<Assertion.AssertionException>(() => activator.Activate(type));

      Console.WriteLine(exception);
    }

    [TestCase(typeof(RootModel))]
    [TestCase(typeof(ModelCalls))]
    public void TestActivation(Type type)
    {
      var serializer = new ReflectionSerializersFactory();
      var activator = new ReflectionRdActivator(serializer, null);
      var activateRdModel = activator.Activate(type);
    }
  }
}