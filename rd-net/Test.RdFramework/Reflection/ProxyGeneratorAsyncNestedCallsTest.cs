using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  /// <summary>
  /// Factorial calculator using tail recursion and independent protocols.
  /// Based on a single scheduler as there is only one process in tests.
  /// Illustration for nested call support for sync calls in reflection-based Rd.
  /// </summary>
  [TestFixture]
  public class ProxyGeneratorAsyncNestedCallsTest : ProxyGeneratorTestBase
  {
    protected override bool IsAsync => true;
    private IScheduler myCommonScheduler;
    protected override IScheduler CreateScheduler(bool isServer)
    {
      return myCommonScheduler ?? (myCommonScheduler = base.CreateScheduler(isServer));
    }

    [RdRpc]
    public interface IClientFactorial
    {
      int Fac(int n);
    }

    [RdRpc]
    public interface IServerFactorial
    {
      int Fac(int n);
    }

    [RdExt]
    internal class ClientFactorialExt : RdExtReflectionBindableBase, IClientFactorial
    {
      private readonly IServerFactorial myOtherSide;
      public ClientFactorialExt(IServerFactorial otherSide) => myOtherSide = otherSide;
      public int Fac(int n) => n <= 1 ? 1 : myOtherSide.Fac(n - 1) * n;
    }

    [RdExt]
    internal class ServerFactorialExt : RdExtReflectionBindableBase, IServerFactorial
    {
      private readonly IClientFactorial myOtherSide;
      public ServerFactorialExt(IClientFactorial otherSide) => myOtherSide = otherSide;
      public int Fac(int n) => n <= 1 ? 1 : myOtherSide.Fac(n - 1) * n;
    }

    [Test]
    public async Task Test()
    {
      await YieldToClient();
      var serverProxy = CFacade.ActivateProxy<IServerFactorial>(TestLifetime, ClientProtocol);
      var client = CFacade.InitBind(new ClientFactorialExt(serverProxy), TestLifetime, ClientProtocol);

      var clientProxy = CFacade.ActivateProxy<IClientFactorial>(TestLifetime, ServerProtocol);
      var server = CFacade.InitBind(new ServerFactorialExt(clientProxy), TestLifetime, ServerProtocol);
      await Wait();
      await YieldToClient();

      var fac = server.Fac(6);
      Assert.AreEqual(720, fac);

      await Wait();
    }
  }
}