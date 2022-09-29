using System.Collections.Generic;
using JetBrains.Diagnostics;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdSignalTest : RdFrameworkTestBase
  {
    private static readonly int ourKey = 1;

    [Test]
    public void TestFireSignal()
    {
      var serverSignal = BindToServer(LifetimeDefinition.Lifetime, NewRdSignal<string>(), ourKey);
      var clientSignal = BindToClient(LifetimeDefinition.Lifetime, NewRdSignal<string>(), ourKey);

      var results = new List<string>();
      clientSignal.Advise(LifetimeDefinition.Lifetime, value => results.Add(value));

      serverSignal.Fire("server value");
      ServerWire.TransmitOneMessage();

      Assert.AreEqual(1, results.Count);
      Assert.AreEqual("server value", results[0]);
    }

    [Test]
    public void TestNullability()
    {
      var serverSignal = BindToServer(LifetimeDefinition.Lifetime, NewRdSignal<string>(), ourKey);
      var clientSignal = BindToClient(LifetimeDefinition.Lifetime, NewRdSignal<string>(), ourKey);

      var results = new List<string>();
      clientSignal.Advise(LifetimeDefinition.Lifetime, value => results.Add(value));

      Assert.Throws<Assertion.AssertionException>(() => { clientSignal.Fire(null); });
      ServerWire.TransmitAllMessages();

      serverSignal.Fire("server value");
      ServerWire.TransmitAllMessages();

      Assert.Throws<Assertion.AssertionException>(() => { clientSignal.Fire(null); });
      ServerWire.TransmitAllMessages();

      Assert.AreEqual(1, results.Count);
      Assert.AreEqual("server value", results[0]);
    }
  }
}