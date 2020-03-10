using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ProxyGeneratorModelTest : ProxyGeneratorTestBase
  {
    [RdExt]
    public class ModelOwner : RdExtReflectionBindableBase
    {
      public IViewableProperty<ModelWithRpc> Model { get; }
    }


    public interface IModelWithRpc
    {
      int Double(int x);
    }

    [RdModel]
    public class ModelWithRpc : RdReflectionBindableBase, IModelWithRpc
    {
      public int Double(int x) => x * 2;
    }

    [Test]
    public void Test()
    {

    }
  }
}