using System;
using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdMapTest : RdFrameworkTestBase
  {
    private static readonly int ourKey = 1;

    [Test]
    public void Test1()
    {
      var serverMap = BindToServer(LifetimeDefinition.Lifetime, new RdMap<int, string> { IsMaster = true, OptimizeNested = true }, ourKey);
      var clientMap = BindToClient(LifetimeDefinition.Lifetime, new RdMap<int, string> { IsMaster = false, OptimizeNested = true }, ourKey);

      Assert.True(serverMap.Count == 0);
      Assert.True(clientMap.Count == 0);
      serverMap.Add(1, "Server value 1");
      serverMap.Add(2, "Server value 2");
      serverMap.Add(3, "Server value 3");
      ServerWire.TransmitOneMessage();
      ServerWire.TransmitOneMessage();
      ServerWire.TransmitOneMessage();
      
      ClientWire.TransmitOneMessage(); //ack
      ClientWire.TransmitOneMessage(); //ack
      ClientWire.TransmitOneMessage(); //ack
      Assert.AreEqual(3, clientMap.Count);

      serverMap.Add(4, "Server value 4");
      clientMap.Add(4, "Client value 4");
      ServerWire.TransmitOneMessage();
      ClientWire.TransmitOneMessage();  //ack
      ClientWire.TransmitOneMessage();
      Assert.AreEqual("Server value 4", clientMap[4]);
      Assert.AreEqual("Server value 4", serverMap[4]);

      serverMap.Add(5, "Server value 5");
      clientMap.Add(5, "Client value 5");

      ClientWire.TransmitOneMessage(); 
      ServerWire.TransmitOneMessage();
      ClientWire.TransmitOneMessage(); //ack
      Assert.AreEqual("Server value 5", clientMap[5]);
      Assert.AreEqual("Server value 5", serverMap[5]);
    }

    [Test]
    public void Test2()
    {
      var serverMap = BindToServer(LifetimeDefinition.Lifetime, new RdMap<int, string> {IsMaster = true, OptimizeNested = true}, ourKey);
      var clientMap = BindToClient(LifetimeDefinition.Lifetime, new RdMap<int, string> { IsMaster = false, OptimizeNested = true }, ourKey);

      var log = new List<string>();
      clientMap.Advise(LifetimeDefinition.Lifetime, (e) => log.Add(e.Kind + " " + e.Key + " " + e.NewValue));   

      serverMap.Add(1, "1");
      serverMap[1] = "2";
      serverMap[1] = "2"; //no value
      ServerWire.TransmitAllMessages();
      
      clientMap[1] = "1";
      ClientWire.TransmitAllMessages();

      serverMap.Remove(1);
      ServerWire.TransmitAllMessages();
      ClientWire.TransmitAllMessages(); //ack
         
      
      Assert.AreEqual(new List<string> {"Add 1 1", "Update 1 2", "Update 1 1", "Remove 1 "}, log);

    }

    [Test]
    public void TestLifetimes1()
    {
      var serverMap = BindToServer(LifetimeDefinition.Lifetime, new RdMap<int, string> { IsMaster = true, OptimizeNested = true }, ourKey);
      var clientMap = BindToClient(LifetimeDefinition.Lifetime, new RdMap<int, string> { IsMaster = false, OptimizeNested = true }, ourKey);

      var itemRemoved = "";

      serverMap.Add(1, "Server value");
      serverMap.View(LifetimeDefinition.Lifetime, (lifetime, i, s) => itemRemoved =  i+":"+s);
      ServerWire.TransmitOneMessage();

      clientMap.Remove(1);
      ClientWire.TransmitAllMessages();

      Assert.AreEqual("1:Server value", itemRemoved);
    }

    [Test]
    public void TestLifetimes2()
    {
      var serverMap = BindToServer(LifetimeDefinition.Lifetime, new RdMap<int, string> { IsMaster = true, OptimizeNested = true }, ourKey);
      var clientMap = BindToClient(LifetimeDefinition.Lifetime, new RdMap<int, string> { IsMaster = false, OptimizeNested = true }, ourKey);

      var itemRemovedServer = false;
      var itemRemovedClient = false;

      serverMap.View(Lifetime.Eternal, (lifetime, key, value) =>
      {
        lifetime.OnTermination(() => { itemRemovedServer = true; });
      });

      clientMap.View(Lifetime.Eternal, (lifetime, key, value) =>
      {
        lifetime.OnTermination(() => { itemRemovedClient = true; });
      });

      serverMap.Add(1, "Server value");
      ServerWire.TransmitOneMessage();

      clientMap.Remove(1);
      ClientWire.TransmitOneMessage(); //ack
      Assert.IsFalse(itemRemovedServer);
      
      ClientWire.TransmitOneMessage();

      Assert.IsTrue(itemRemovedServer);
      Assert.IsTrue(itemRemovedClient);
    }

    [Test]
    public void TestNullability()
    {
      var serverMap = BindToServer(LifetimeDefinition.Lifetime, new RdMap<string, string> {IsMaster = true, OptimizeNested = true}, ourKey);
      var clientMap = BindToClient(LifetimeDefinition.Lifetime, new RdMap<string, string> {IsMaster = false, OptimizeNested = true}, ourKey);

      Assert.Throws<Assertion.AssertionException>(() => { serverMap.Add("", null); });
      Assert.Throws<ArgumentNullException>(() => { serverMap.Add(null, ""); });
      ServerWire.TransmitAllMessages();
      ClientWire.TransmitAllMessages();
      Assert.AreEqual(0, serverMap.Count);
      Assert.AreEqual(0, clientMap.Count);
    }
  }
}