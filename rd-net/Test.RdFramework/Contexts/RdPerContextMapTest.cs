using System.Collections.Generic;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework.Contexts
{
  public class RdPerContextMapTest : RdFrameworkTestBase
  {
    public override void SetUp()
    {
      base.SetUp();
      ServerWire.AutoTransmitMode = true;
      ClientWire.AutoTransmitMode = true;
    }

    [Test]
    public void TestOnStructMap()
    {
      var key = RdContextBasicTest.TestKeyHeavy.Instance;

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());

      var server1Cid = "Server-1";
      var client1Cid = "Client-1";
      
      key.RegisterOn(ClientProtocol.Serializers);
      ServerProtocol.Contexts.RegisterContext(key);
      ClientProtocol.Contexts.RegisterContext(key);

      ServerProtocol.Contexts.GetValueSet(key).Add(server1Cid);

      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);

      serverMap[server1Cid][1] = "test";
      
      Assert.True(clientMap.TryGetValue(server1Cid, out var map) && map != null);
      Assert.AreEqual("test", clientMap[server1Cid][1]);

      ClientProtocol.Contexts.GetValueSet(key).Add(client1Cid);
      
      Assert.True(serverMap.TryGetValue(client1Cid, out var map1) && map1 != null);
      Assert.False(serverMap[client1Cid].ContainsKey(1));
    }

    [Test]
    public void TestLateBind01()
    {
      var key = RdContextBasicTest.TestKeyHeavy.Instance;

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());

      var server1Cid = "Server-1";
      var client1Cid = "Client-1";
      
      key.RegisterOn(ClientProtocol.Serializers);
      ServerProtocol.Contexts.RegisterContext(key);
      ClientProtocol.Contexts.RegisterContext(key);

      ServerProtocol.Contexts.GetValueSet(key).Add(server1Cid);
      
      serverMap[server1Cid][1] = "test";
      ClientProtocol.Contexts.GetValueSet(key).Add(client1Cid);
      
      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);
      
      Assert.True(clientMap.TryGetValue(server1Cid, out var map) && map != null);
      Assert.AreEqual("test", clientMap[server1Cid][1]);

      Assert.True(serverMap.TryGetValue(client1Cid, out var map1) && map1 != null);
      Assert.False(serverMap[client1Cid].ContainsKey(1));
    }
    
    [Test]
    public void TestLateBind02()
    {
      var key = RdContextBasicTest.TestKeyHeavy.Instance;

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());

      var server1Cid = "Server-1";
      var client1Cid = "Client-1";
      
      key.RegisterOn(ClientProtocol.Serializers);
      ServerProtocol.Contexts.RegisterContext(key);
      ClientProtocol.Contexts.RegisterContext(key);

      // no protocol value set value - pre-bind value will be lost
      
      serverMap[server1Cid][1] = "test";

      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);
      
      Assert.False(clientMap.TryGetValue(server1Cid, out _));

      Assert.False(serverMap.TryGetValue(client1Cid, out _));
    }
    
    [Test]
    public void TestLateBind03()
    {
      var key = RdContextBasicTest.TestKeyHeavy.Instance;

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());

      var server1Cid = "Server-1";
      var client1Cid = "Client-1";
      
      key.RegisterOn(ClientProtocol.Serializers);
      ServerProtocol.Contexts.RegisterContext(key);
      ClientProtocol.Contexts.RegisterContext(key);

      ServerProtocol.Contexts.GetValueSet(key).Add(server1Cid);
      ServerProtocol.Contexts.GetValueSet(key).Add(client1Cid);

      var log = new List<string>();
      
      serverMap.View(LifetimeDefinition.Lifetime, (lifetime, s, _) =>
      {
        log.Add("Add " + s);
        lifetime.OnTermination(() => log.Add("Remove " + s));
      });

      serverMap[server1Cid][1] = "test";
      serverMap[client1Cid][1] = "test";

      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);
      
      Assert.AreEqual(new []{"Add " + server1Cid, "Add " + client1Cid}, log);
    }
    
    [Test]
    public void TestLateBind04()
    {
      var key = RdContextBasicTest.TestKeyHeavy.Instance;

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());

      var server1Cid = "Server-1";
      var client1Cid = "Client-1";
      
      key.RegisterOn(ClientProtocol.Serializers);
      ServerProtocol.Contexts.RegisterContext(key);
      ClientProtocol.Contexts.RegisterContext(key);

      ServerProtocol.Contexts.GetValueSet(key).Add(server1Cid);

      var log = new List<string>();
      
      serverMap.View(LifetimeDefinition.Lifetime, (lifetime, s, _) =>
      {
        log.Add("Add " + s);
        lifetime.OnTermination(() => log.Add("Remove " + s));
      });

      serverMap[server1Cid][1] = "test";
      serverMap[client1Cid][1] = "test";

      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);
      
      Assert.AreEqual(new []{"Add " + server1Cid, "Add " + client1Cid, "Remove " + client1Cid}, log);
    }
    
    [Test]
    public void TestLateBind05()
    {
      var key = RdContextBasicTest.TestKeyHeavy.Instance;

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());

      var server1Cid = "Server-1";

      key.RegisterOn(ClientProtocol.Serializers);
      ServerProtocol.Contexts.RegisterContext(key);
      ClientProtocol.Contexts.RegisterContext(key);
      
      var log = new List<string>();
      
      serverMap.View(LifetimeDefinition.Lifetime, (lifetime, s, _) =>
      {
        log.Add("Add " + s);
        lifetime.OnTermination(() => log.Add("Remove " + s));
      });
      
      ServerProtocol.Contexts.GetValueSet(key).Add(server1Cid);

      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);
      
      serverMap[server1Cid][1] = "test";
      
      Assert.AreEqual(new []{"Add " + server1Cid}, log);
    }
    
    [Test]
    public void TestLateBind06()
    {
      var key = RdContextBasicTest.TestKeyHeavy.Instance;

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => NewRdMap<int, string>());

      var server1Cid = "Server-1";

      var log = new List<string>();
      
      serverMap.View(LifetimeDefinition.Lifetime, (lifetime, s, _) =>
      {
        log.Add("Add " + s);
        lifetime.OnTermination(() => log.Add("Remove " + s));
      });
      
      key.RegisterOn(ClientProtocol.Serializers);
      ServerProtocol.Contexts.RegisterContext(key);
      ClientProtocol.Contexts.RegisterContext(key);

      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);

      using (key.UpdateValue(server1Cid))
      {
        ServerProtocol.Wire.Send(ServerProtocol.Identities.Mix(RdId.Nil, 10), _ => { });
      }
      
      Assert.True(ServerProtocol.Contexts.GetValueSet(key).Contains(server1Cid));
      
      Assert.AreEqual(new []{"Add " + server1Cid}, log);
    }
    
    [Test]
    public void TestValueSetChangesInContext()
    {
      var key1 = RdContextBasicTest.TestKeyHeavy.Instance;
      var key2 = RdContextBasicTest.TestKey2.Instance;

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key1, _ => NewRdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key1, _ => NewRdMap<int, string>());

      var server1Cid = "Server-1";
      var server2Cid = "Server-2";
      var server3Cid = "Server-3";
      var server4Cid = "Server-4";

      var log = new List<string>();
      
      serverMap.View(LifetimeDefinition.Lifetime, (lifetime, s, _) =>
      {
        log.Add("Add " + s);
        lifetime.OnTermination(() => log.Add("Remove " + s));
      });

      using (key1.UpdateValue(server1Cid))
      using (key2.UpdateValue(server1Cid))
      {
        key1.RegisterOn(ClientProtocol.Serializers);
        key2.RegisterOn(ClientProtocol.Serializers);
        ServerProtocol.Contexts.RegisterContext(key1);
        ServerProtocol.Contexts.RegisterContext(key2);
        ClientProtocol.Contexts.RegisterContext(key1);

        BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
        BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);

        ServerProtocol.Contexts.GetValueSet(key1).Add(server2Cid);
        using (key1.UpdateValue(server4Cid))
        {
          ServerProtocol.Contexts.GetValueSet(key1).Add(server3Cid);
        }
      }

      Assert.False(ServerProtocol.Contexts.GetValueSet(key1).Contains(server1Cid));
      Assert.False(ServerProtocol.Contexts.GetValueSet(key2).Contains(server1Cid));
      
      Assert.AreEqual(new []{"Add " + server2Cid, "Add " + server3Cid}, log);
    }
  }
}