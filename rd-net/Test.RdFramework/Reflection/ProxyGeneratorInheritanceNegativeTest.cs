using System;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ProxyGeneratorInheritanceNegativeTest : RdReflectionTestBase
  {
    public interface ITest1
    {
      ISignal<string> Signal { get; }
    }

    public interface ITest2
    {
      ISignal<string> Signal { get; }
    }

    [RdRpc]
    public interface IInheritanceTest : ITest2, ITest1
    {
    }

    [Test]
    public void TestNegative1()
    {
      Assert.Throws<ArgumentException>(() => SFacade.ActivateProxy<IInheritanceTest>(TestLifetime, ServerProtocol));
    }
  }
}