using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection;

[TestFixture]
public class ReflectionSerializersPrimitivesTest
{
  [Test]
  public void TestNonPolymorphicForPrimitive()
  {
    var s = new ReflectionSerializers(new SimpleTypesCatalog());
    var pair = s.GetOrRegisterSerializerPair(typeof(IViewableList<string>), true);

    Assert.False(pair.IsPolymorphic);
  }
}