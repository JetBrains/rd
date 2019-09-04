using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdPropertyTest : RdFrameworkTestBase
  {
    private static readonly int ourKey = 1;

//    private static RdProperty<string> CreateProperty(IProtocol protocol, bool isMaster)
//    {
//      return new RdProperty<string>(Lifetime.Eternal, protocol, ourKey, isMaster);
//    }

    [Test]
    public void Test1()
    {
      var serverProperty = BindToServer(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = true }, ourKey);
      var clientProperty = BindToClient(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = false }, ourKey);

      // Everything is empty
      Assert.False(serverProperty.Maybe.HasValue);
      Assert.False(clientProperty.Maybe.HasValue);

      // Init server
      var serverValue1 = "Server value 1";
      serverProperty.SetValue(serverValue1);
      Assert.AreEqual(serverValue1, serverProperty.Value);
      Assert.False(clientProperty.Maybe.HasValue);

      // Server -> Client
      ServerWire.TransmitOneMessage();
      Assert.AreEqual(serverValue1, clientProperty.Value);

      // Client -> Server
      var clientValue1 = "Client value 1";
      clientProperty.SetValue(clientValue1);
      ClientWire.TransmitOneMessage();
      Assert.AreEqual(clientValue1, serverProperty.Value);
      Assert.AreEqual(clientValue1, clientProperty.Value);
    }

    [Test]
    public void Test2()
    {
      var serverProperty = BindToServer(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = true }, ourKey);
      var clientProperty = BindToClient(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = false }, ourKey);

      // Server -> Client
      serverProperty.SetValue("Server value 1");
      ServerWire.TransmitOneMessage();

      // Change client
      clientProperty.SetValue("Client value 1");

      // Server -> Client one more time (client already dirty, but should be updated)
      var serverValue2 = "Server value 2";
      serverProperty.SetValue(serverValue2);
      ServerWire.TransmitOneMessage();

      // Client -> Server one more time (server should not be changed)
      ClientWire.TransmitOneMessage();

      Assert.AreEqual(serverValue2, serverProperty.Value);
      Assert.AreEqual(serverValue2, clientProperty.Value);
    }

    [Test]
    public void Test3()
    {
      var serverProperty = BindToServer(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = true }, ourKey);
      var clientProperty = BindToClient(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = false }, ourKey);

      // Client -> Server
      clientProperty.SetValue("Client value 1");
      ClientWire.TransmitOneMessage();

      // Change server
      var serverValue1 = "Server value 1";
      serverProperty.SetValue(serverValue1);

      // Client -> Server one more time (server should not be updated)
      clientProperty.SetValue("Client value 2");
      ClientWire.TransmitOneMessage();

      // Server -> Client one more time (client should be updated)
      ServerWire.TransmitOneMessage();

      Assert.AreEqual(serverValue1, serverProperty.Value);
      Assert.AreEqual(serverValue1, clientProperty.Value);
    }

    [Test]
    public void Test4()
    {
      var serverProperty = BindToServer(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = true }, ourKey);
      var clientProperty = BindToClient(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = false }, ourKey);

      serverProperty.SetValue("Server value 1");
      ServerWire.TransmitOneMessage();

      clientProperty.SetValue("Client value 1");
      ClientWire.TransmitOneMessage();

      clientProperty.SetValue("Client value 2");
      ClientWire.TransmitOneMessage();

      clientProperty.SetValue("Client value 3");
      ClientWire.MissOneMessage();

      clientProperty.SetValue("Client value 4");
      ClientWire.TransmitOneMessage();

      Assert.AreEqual("Client value 4", serverProperty.Value);
      Assert.AreEqual("Client value 4", clientProperty.Value);
    }

    [Test]
    public void Test5()
    {
      var serverProperty = BindToServer(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = true }, ourKey);
      var clientProperty = BindToClient(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = false }, ourKey);

      serverProperty.SetValue("Server value 1");
      ServerWire.TransmitOneMessage();

      clientProperty.SetValue("Client value 1");
      ClientWire.TransmitOneMessage();

      clientProperty.SetValue("Client value 2");
      ClientWire.MissOneMessage();

      clientProperty.SetValue("Client value 3");
      ClientWire.TransmitOneMessage();

      serverProperty.SetValue("Server value 2");
      ServerWire.MissOneMessage();

      serverProperty.SetValue("Server value 3");
      ServerWire.TransmitOneMessage();

      Assert.AreEqual("Server value 3", serverProperty.Value);
      Assert.AreEqual("Server value 3", clientProperty.Value);
    }
    
    [Test]
    public void TestNullability()
    {
      var serverProperty = BindToServer(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = true }, ourKey);
      var clientProperty = BindToClient(LifetimeDefinition.Lifetime, new RdProperty<string> { IsMaster = false }, ourKey);

      serverProperty.SetValue("Server value 1");
      Assert.Throws<Assertion.AssertionException>(() => { serverProperty.SetValue(null); });
      ServerWire.TransmitAllMessages();
      ClientWire.TransmitAllMessages();
      Assert.AreEqual("Server value 1", serverProperty.Value);
      Assert.AreEqual("Server value 1", clientProperty.Value);
    }
  }
}