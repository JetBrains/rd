using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ProxyGeneratorInheritanceTest : RdReflectionTestBase
  {
    public interface IBaseNonRpcInterace
    {
      ISignal<string> Signal { get; }
    }
    public interface IMiddle : IBaseNonRpcInterace
    {
    }

    [RdRpc]
    public interface IInheritanceTest : IMiddle
    {
    }

    [RdExt]
    public class InheritanceTest : RdExtReflectionBindableBase, IInheritanceTest
    {
      public ISignal<string> Signal { get; }
    }

    [Test]
    public void TestInheritance1()
    {
      SaveGeneratedAssembly();

      var client = CFacade.Activator.ActivateBind<InheritanceTest>(TestLifetime, ClientProtocol);
      var proxy = SFacade.ActivateProxy<IInheritanceTest>(TestLifetime, ServerProtocol);
      Assertion.Assert(((RdExtReflectionBindableBase)proxy).Connected.Value, "((RdReflectionBindableBase)proxy).Connected.Value");

      // test signals
      bool raised = false;
      proxy.Signal.Advise(TestLifetime, s => raised = true);
      client.Signal.Fire("test");
      Assertion.Assert(raised, "!raised");
    }
  }
}