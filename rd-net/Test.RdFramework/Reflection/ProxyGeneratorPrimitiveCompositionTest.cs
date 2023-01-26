using System.Runtime.CompilerServices;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
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

    IViewableMap<short, IViewableSet<short>> SyncMoments { get; }
  }

  [RdExt]
  public class Test : RdExtReflectionBindableBase, ITest
  {
    public IViewableMap<short, IViewableSet<short>> SyncMoments { get; }
    public IViewableList<IViewableList<string>> Multilist { get; }
  }

  [Test]
  public ConfiguredTaskAwaitable TestAsyncNet35_wrapper() => TestAsync().ConfigureAwait(false);
  public async Task TestAsync()
  {
    await YieldToClient();
    var client = CFacade.ActivateProxy<ITest>(TestLifetime, ClientProtocol);

    await YieldToServer();
    var server = SFacade.InitBind(new Test(), TestLifetime, ServerProtocol);
    await Wait();

    await YieldToClient();
    var vs = CFacade.Activator.Activate<IViewableSet<short>>();
    vs.Add(123);
    client.SyncMoments.Add(123, vs);
    
    await Wait();
    Assert.IsTrue(server.SyncMoments[123].Contains(123));
  }
}