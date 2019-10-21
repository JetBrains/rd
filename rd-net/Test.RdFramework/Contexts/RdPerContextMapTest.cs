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
      
      new RdContextKey<string>("test-key", true, Serializers.ReadString, Serializers.WriteString).Value = null;
    }

    [Test]
    public void TestOnStructMap()
    {
      var key = new RdContextKey<string>("test-key", true, Serializers.ReadString, Serializers.WriteString);

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());

      var server1Cid = "Server-1";
      var client1Cid = "Client-1";
      
      ServerProtocol.ContextHandler.RegisterKey(key);
      ClientProtocol.ContextHandler.RegisterKey(key);

      ServerProtocol.ContextHandler.GetValueSet(key).Add(server1Cid);

      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);

      serverMap[server1Cid][1] = "test";
      
      Assert.True(clientMap.TryGetValue(server1Cid, out var map) && map != null);
      Assert.AreEqual("test", clientMap[server1Cid][1]);

      ClientProtocol.ContextHandler.GetValueSet(key).Add(client1Cid);
      
      Assert.True(serverMap.TryGetValue(client1Cid, out var map1) && map1 != null);
      Assert.False(serverMap[client1Cid].ContainsKey(1));
    }

    [Test]
    public void TestLateBind01()
    {
      var key = new RdContextKey<string>("test-key", true, Serializers.ReadString, Serializers.WriteString);

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());

      var server1Cid = "Server-1";
      var client1Cid = "Client-1";
      
      ServerProtocol.ContextHandler.RegisterKey(key);
      ClientProtocol.ContextHandler.RegisterKey(key);

      ServerProtocol.ContextHandler.GetValueSet(key).Add(server1Cid);
      
      serverMap[server1Cid][1] = "test";
      ClientProtocol.ContextHandler.GetValueSet(key).Add(client1Cid);
      
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
      var key = new RdContextKey<string>("test-key", true, Serializers.ReadString, Serializers.WriteString);

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());

      var server1Cid = "Server-1";
      var client1Cid = "Client-1";
      
      ServerProtocol.ContextHandler.RegisterKey(key);
      ClientProtocol.ContextHandler.RegisterKey(key);

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
      var key = new RdContextKey<string>("test-key", true, Serializers.ReadString, Serializers.WriteString);

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());

      var server1Cid = "Server-1";
      var client1Cid = "Client-1";
      
      ServerProtocol.ContextHandler.RegisterKey(key);
      ClientProtocol.ContextHandler.RegisterKey(key);

      ServerProtocol.ContextHandler.GetValueSet(key).Add(server1Cid);
      ServerProtocol.ContextHandler.GetValueSet(key).Add(client1Cid);

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
      var key = new RdContextKey<string>("test-key", true, Serializers.ReadString, Serializers.WriteString);

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());

      var server1Cid = "Server-1";
      var client1Cid = "Client-1";
      
      ServerProtocol.ContextHandler.RegisterKey(key);
      ClientProtocol.ContextHandler.RegisterKey(key);

      ServerProtocol.ContextHandler.GetValueSet(key).Add(server1Cid);

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
      var key = new RdContextKey<string>("test-key", true, Serializers.ReadString, Serializers.WriteString);

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());

      var server1Cid = "Server-1";

      ServerProtocol.ContextHandler.RegisterKey(key);
      ClientProtocol.ContextHandler.RegisterKey(key);
      
      var log = new List<string>();
      
      serverMap.View(LifetimeDefinition.Lifetime, (lifetime, s, _) =>
      {
        log.Add("Add " + s);
        lifetime.OnTermination(() => log.Add("Remove " + s));
      });
      
      ServerProtocol.ContextHandler.GetValueSet(key).Add(server1Cid);

      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);
      
      serverMap[server1Cid][1] = "test";
      
      Assert.AreEqual(new []{"Add " + server1Cid}, log);
    }
    
    [Test]
    public void TestLateBind06()
    {
      var key = new RdContextKey<string>("test-key", true, Serializers.ReadString, Serializers.WriteString);

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key, _ => new RdMap<int, string>());

      var server1Cid = "Server-1";

      var log = new List<string>();
      
      serverMap.View(LifetimeDefinition.Lifetime, (lifetime, s, _) =>
      {
        log.Add("Add " + s);
        lifetime.OnTermination(() => log.Add("Remove " + s));
      });
      
      ServerProtocol.ContextHandler.RegisterKey(key);
      ClientProtocol.ContextHandler.RegisterKey(key);

      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);

      key.Value = server1Cid;
      ServerProtocol.Wire.Send(RdId.Nil.Mix(10), _ => { });
      key.Value = null;
      
      Assert.True(ServerProtocol.ContextHandler.GetValueSet(key).Contains(server1Cid));
      
      Assert.AreEqual(new []{"Add " + server1Cid}, log);
    }
    
    [Test]
    public void TestValueSetChangesInContext()
    {
      var key1 = new RdContextKey<string>("test-key1", true, Serializers.ReadString, Serializers.WriteString);
      var key2 = new RdContextKey<string>("test-key2", true, Serializers.ReadString, Serializers.WriteString);

      var serverMap = new RdPerContextMap<string, RdMap<int, string>>(key1, _ => new RdMap<int, string>());
      var clientMap = new RdPerContextMap<string, RdMap<int, string>>(key1, _ => new RdMap<int, string>());

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
      
      key1.Value = server1Cid;
      key2.Value = server1Cid;
      
      ServerProtocol.ContextHandler.RegisterKey(key1);
      ServerProtocol.ContextHandler.RegisterKey(key2);
      ClientProtocol.ContextHandler.RegisterKey(key1);

      BindToClient(LifetimeDefinition.Lifetime, clientMap, 1);
      BindToServer(LifetimeDefinition.Lifetime, serverMap, 1);

      ServerProtocol.ContextHandler.GetValueSet(key1).Add(server2Cid);
      key1.Value = server4Cid;
      ServerProtocol.ContextHandler.GetValueSet(key1).Add(server3Cid);
      
      
      key1.Value = null;
      key2.Value = null;
      
      Assert.False(ServerProtocol.ContextHandler.GetValueSet(key1).Contains(server1Cid));
      Assert.False(ServerProtocol.ContextHandler.GetValueSet(key2).Contains(server1Cid));
      
      Assert.AreEqual(new []{"Add " + server2Cid, "Add " + server3Cid}, log);
    }
  }
}