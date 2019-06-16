using System;
using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdListTest : RdFrameworkTestBase
  {
    private static readonly int ourKey = 1;

    [Test]
    public void Test1()
    {
      var serverList = BindToServer(TestLifetime, new RdList<string> { OptimizeNested = true }, ourKey);
      var clientList = BindToClient(TestLifetime, new RdList<string> { OptimizeNested = true }, ourKey);

      Assert.True(serverList.Count == 0);
      Assert.True(clientList.Count == 0);
      serverList.Add("Server value 1");
      serverList.Add("Server value 2");
      serverList.Add("Server value 3");
      ServerWire.TransmitOneMessage();
      ServerWire.TransmitOneMessage();
      ServerWire.TransmitOneMessage();      
      Assert.AreEqual(3, clientList.Count);
      
      serverList.Add("Server value 4");
      ServerWire.TransmitOneMessage();
      clientList[3] = "Client value 4";
      ClientWire.TransmitOneMessage();
      
      Assert.AreEqual("Client value 4", clientList[3]);
      Assert.AreEqual("Client value 4", serverList[3]);
      
      serverList.RemoveAt(0);

      ServerWire.TransmitOneMessage();       
      Assert.AreEqual("Server value 2", clientList[0]);
      Assert.AreEqual("Server value 2", serverList[0]);
    }

    [Test]
    public void Test2()
    {
      var serverList = BindToServer(TestLifetime, new RdList<string> { OptimizeNested = true}, ourKey);
      var clientList = BindToClient(TestLifetime, new RdList<string> { OptimizeNested = true }, ourKey);

      var log = new List<string>();
      clientList.Advise(TestLifetime, (e) => log.Add(e.Kind + " " + e.Index + " " + e.NewValue));   

      serverList.Add("1");
      serverList[0] = "2";
      serverList[0] = "2"; //no value
      ServerWire.TransmitAllMessages();
      
      clientList[0] = "1";
      ClientWire.TransmitAllMessages();

      serverList.Clear();
      ServerWire.TransmitAllMessages();
      
      Assert.AreEqual(new List<string> {"Add 0 1", "Update 0 2", "Update 0 1", "Remove 0 "}, log);

    }

    [Test]
    public void TestLifetimes1()
    {
      var serverList = BindToServer(TestLifetime, new RdList<string> { OptimizeNested = true }, ourKey);
      var clientList = BindToClient(TestLifetime, new RdList<string> { OptimizeNested = true }, ourKey);

      var itemRemoved = "";

      serverList.Add("Server value");
      serverList.View(TestLifetime, (lifetime, i, s) => itemRemoved =  i+":"+s);
      ServerWire.TransmitOneMessage();

      clientList.Remove("Server value");
      clientList.Remove("Server value");
      ClientWire.TransmitAllMessages();

      Assert.AreEqual("0:Server value", itemRemoved);
    }

    [Test]
    public void TestLifetimes2()
    {
      var serverList = BindToServer(LifetimeDefinition.Lifetime, new RdList<string> { OptimizeNested = true }, ourKey);
      var clientList = BindToClient(LifetimeDefinition.Lifetime, new RdList<string> { OptimizeNested = true }, ourKey);

      var itemRemovedServer = false;
      var itemRemovedClient = false;

      serverList.View(Lifetime.Eternal, (lifetime, key, value) =>
      {
        lifetime.AddAction(() => { itemRemovedServer = true; });
      });

      clientList.View(Lifetime.Eternal, (lifetime, key, value) =>
      {
        lifetime.AddAction(() => { itemRemovedClient = true; });
      });

      serverList.Add("Server value");
      ServerWire.TransmitOneMessage();

      clientList.RemoveAt(0);
      Assert.IsFalse(itemRemovedServer);
      
      ClientWire.TransmitOneMessage();

      Assert.IsTrue(itemRemovedServer);
      Assert.IsTrue(itemRemovedClient);
    }

    [Test]
    public void TestNullability()
    {
      var serverList = BindToServer(LifetimeDefinition.Lifetime, new RdList<string> {OptimizeNested = true}, ourKey);
      var clientList = BindToClient(LifetimeDefinition.Lifetime, new RdList<string> {OptimizeNested = true}, ourKey);

      Assert.Throws<ArgumentNullException>(() => { serverList.Add(null); });
      ServerWire.TransmitAllMessages();
      Assert.AreEqual(0, serverList.Count);
      Assert.AreEqual(0, clientList.Count);
    }
  }
}