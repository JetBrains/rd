using System;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection;

[TestFixture]
public class ProxyGeneratorComplexScalarsTest : RdReflectionTestBase
{
  [Test]
  public void RunTest()
  {
    var proxy = SFacade.ActivateProxy<IArgsCalls>(TestLifetime, ServerProtocol);
    var client = CFacade.Activator.ActivateBind<ArgsCalls>(TestLifetime, ClientProtocol);
    CollectionAssert.AreEqual(client.Test(null), proxy.Test(null));
  }

  [RdRpc] public interface IArgsCalls
  {
    Guid[] Test(Guid[] input);
  }

  [RdExt] public class ArgsCalls : RdExtReflectionBindableBase, IArgsCalls
  {
    public Guid[] Test(Guid[] input) { return new Guid[1]; }
  }
}
