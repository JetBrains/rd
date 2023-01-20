using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection;

/// <summary>
/// Live models can be returned from calls. Provided lifetime in method's parameters should define the lifetime of
/// "connection" of two sides.
/// </summary>
[TestFixture]
public class ProxyGeneratorPrimitiveCompositionTest : ProxyGeneratorTestBase
{
  protected override bool IsAsync => true;

  [RdRpc]
  public interface ITest
  {
    IViewableList<IViewableList<string>> Multilist { get; }

    // IViewableMap<short, IViewableSet<short>> SyncMoments { get; }
  }

  [RdExt]
  public class Test : RdExtReflectionBindableBase, ITest
  {
    // public IViewableMap<short, IViewableSet<short>> SyncMoments { get; }
    public IViewableList<IViewableList<string>> Multilist { get; }
  }

  [Test]
  public async Task TestAsync()
  {
    await YieldToClient();
    var client = CFacade.ActivateProxy<ITest>(TestLifetime, ClientProtocol);

    await YieldToServer();
    var server = SFacade.InitBind(new Test(), TestLifetime, ServerProtocol);
    await Wait();

    await YieldToClient();
    var vs = CFacade.Activator.Activate<IViewableList<string>>();
    vs.Add("123");
    client.Multilist.Add(vs);
    
    await Wait();
    //Assert.IsTrue(server.SyncMoments[123].Contains(123));
  }
}