using System;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection;

[TestFixture]
public class ProxyGeneratorOverloadsTest : RdReflectionTestBase
{
  [Test]
  public void RunTest()
  {
    var client = CFacade.Activator.ActivateBind<Calls>(TestLifetime, ClientProtocol);
    var proxy = SFacade.ActivateProxy<ICalls>(TestLifetime, ServerProtocol);
    var g = Guid.NewGuid();
    Assert.AreEqual(client.GetLangPreferences(g), proxy.GetLangPreferences(g));
    CollectionAssert.AreEqual(client.GetLangPreferences(new Guid[10]), proxy.GetLangPreferences(new Guid[10]));
  }

  [RdRpc] public interface ICalls
  {
    Guid GetLangPreferences(Guid langServiceGuid);
    Guid[] GetLangPreferences(Guid[] langServiceGuids);
  }

  [RdExt] public class Calls : RdExtReflectionBindableBase, ICalls
  {
    public Guid GetLangPreferences(Guid langServiceGuid) => new("3FB51DAC-EFAB-48D0-9535-D96B79E928B8");
    public Guid[] GetLangPreferences(Guid[] langServiceGuids) => new[] {new Guid("156F86F9-C90D-4D84-B3BB-4568BE3644D4")};
  }
}