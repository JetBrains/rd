using System.Collections.Generic;
using System.Linq;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdSetTest : RdFrameworkTestBase
  {
    private static readonly int ourKey = 1;

    [Test]
    public void Test1()
    {
      var serverSet = BindToServer(LifetimeDefinition.Lifetime, new RdSet<int>(), ourKey);
      var clientSet = BindToClient(LifetimeDefinition.Lifetime, new RdSet<int>(), ourKey);

      ServerWire.AutoTransmitMode = true;
      ClientWire.AutoTransmitMode = true;

      Assert.True(serverSet.Count == 0);
      Assert.True(clientSet.Count == 0);

      var log = new List<int>();
      serverSet.Advise(LifetimeDefinition.Lifetime, (kind, v) => log.Add(kind == AddRemove.Add ? v : -v));

      clientSet.Add(1);
      clientSet.Add(1);
      clientSet.Add(2);
      clientSet.Add(3);

      Assert.AreEqual(new List<int> {1, 2, 3}, log);

      Assert.True(clientSet.Remove(3));
      Assert.AreEqual(new List<int> { 1, 2, 3, -3 }, log);

      serverSet.Remove(3);
      clientSet.Remove(3);
      Assert.AreEqual(new List<int> { 1, 2, 3, -3 }, log);

      clientSet.Remove(1);
      Assert.AreEqual(new List<int> { 1, 2, 3, -3, -1 }, log);

      clientSet.Clear();
      Assert.AreEqual(new List<int> { 1, 2, 3, -3, -1, -2 }, log);
    }

    [Test]
    public void TestNullability()
    {
      var serverSet = BindToServer(LifetimeDefinition.Lifetime, new RdSet<string> {IsMaster = true}, ourKey);
      var clientSet = BindToClient(LifetimeDefinition.Lifetime, new RdSet<string> {IsMaster = false}, ourKey);

      serverSet.Add("Value");
      Assert.Throws<Assertion.AssertionException>(() => { serverSet.Add(null); });
      ServerWire.TransmitAllMessages();
      ClientWire.TransmitAllMessages();

      Assert.AreEqual(1, serverSet.Count);
      Assert.AreEqual(serverSet.First(), "Value");
      
      Assert.AreEqual(1, clientSet.Count);
      Assert.AreEqual(clientSet.First(), "Value");
    }
  }
}