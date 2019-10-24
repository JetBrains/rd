using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework.Contexts
{
  public class RdContextTransformerTest : RdFrameworkTestBase
  {
    public override void SetUp()
    {
      base.SetUp();
      ServerWire.AutoTransmitMode = true;
      ClientWire.AutoTransmitMode = true;
      
      new RdContext<string>("test-key", true, Serializers.ReadString, Serializers.WriteString).Value = null;
    }

    [Test]
    public void TestLateSet()
    {
      var key = new RdContext<string>("test-key", true, Serializers.ReadString, Serializers.WriteString);
      
      ClientProtocol.Contexts.RegisterContext(key);
      
      foreach (var s in new []{"1","2","3"}) ServerProtocol.Contexts.GetValueSet(key).Add(s);
      
      AssertSetsEqual(new []{"1", "2", "3"}, ClientProtocol.Contexts.GetValueSet(key));
      
      ServerProtocol.Contexts.SetTransformerForContext(key, (value, direction) =>
      {
        if (value == null) return null;
        switch (direction)
        {
          case ContextValueTransformerDirection.WriteToProtocol:
            return (int.Parse(value) + 3).ToString();
          case ContextValueTransformerDirection.ReadFromProtocol:
            return (int.Parse(value) - 3).ToString();
          default:
            throw new ArgumentOutOfRangeException(nameof(direction), direction, null);
        }
      });
      
      AssertSetsEqual(new []{"1", "2", "3"}, ClientProtocol.Contexts.GetValueSet(key));
      AssertSetsEqual(new []{"-2", "-1", "0"}, ServerProtocol.Contexts.GetValueSet(key));
    }

    [Test]
    public void TestWithContextMap()
    {
      var key = new RdContext<string>("test-key", true, Serializers.ReadString, Serializers.WriteString);
      key.Value = null;
      
      ServerProtocol.Contexts.RegisterContext(key);
      
      var serverMap = BindToServer(LifetimeDefinition.Lifetime, new RdPerContextMap<string, RdSignal<string>>(key, _ => new RdSignal<string>()), 1);
      var clientMap = BindToClient(LifetimeDefinition.Lifetime, new RdPerContextMap<string, RdSignal<string>>(key, _ => new RdSignal<string>()), 1);

      string Transformer(string value, ContextValueTransformerDirection direction)
      {
        if (value == null) return null;
        switch (direction)
        {
          case ContextValueTransformerDirection.WriteToProtocol:
            return (int.Parse(value) + 3).ToString();
          case ContextValueTransformerDirection.ReadFromProtocol:
            return (int.Parse(value) - 3).ToString();
          default:
            throw new ArgumentOutOfRangeException(nameof(direction), direction, null);
        }
      }

      ServerProtocol.Contexts.SetTransformerForContext(key, Transformer);
      
      foreach (var s in new []{"1","2","3"}) ServerProtocol.Contexts.GetValueSet(key).Add(s);

      var receives = 0;
      clientMap.View(LifetimeDefinition.Lifetime, (elt, k, v) => v.Advise(elt, s =>
      {
        Assert.AreEqual(k, Transformer(s, ContextValueTransformerDirection.WriteToProtocol));
        receives++;
      }));
      
      serverMap.View(LifetimeDefinition.Lifetime, (_, k, v) => v.Fire(k));
      
      Assert.AreEqual(3, receives);
    }

    [Test]
    public void TestWithTwoKeys()
    {
      var key1 = new RdContext<string>("test-key1", true, Serializers.ReadString, Serializers.WriteString);
      var key2 = new RdContext<string>("test-key2", true, Serializers.ReadString, Serializers.WriteString);
      
      ServerProtocol.Contexts.RegisterContext(key1);
      ServerProtocol.Contexts.RegisterContext(key2);
      
      ClientProtocol.Contexts.RegisterContext(key1);
      ClientProtocol.Contexts.RegisterContext(key2);
      
      ServerProtocol.Contexts.SetTransformerForContext(key1, (value, direction) =>
      {
        if (value == null) return null;
        switch (direction)
        {
          case ContextValueTransformerDirection.WriteToProtocol:
            return (int.Parse(value) + 3).ToString();
          case ContextValueTransformerDirection.ReadFromProtocol:
            return (int.Parse(value) - 3).ToString();
          default:
            throw new ArgumentOutOfRangeException(nameof(direction), direction, null);
        }
      });
      
      ServerProtocol.Contexts.SetTransformerForContext(key2, (value, direction) =>
      {
        if (value == null) return null;
        switch (direction)
        {
          case ContextValueTransformerDirection.WriteToProtocol:
            return (int.Parse(value) + 10).ToString();
          case ContextValueTransformerDirection.ReadFromProtocol:
            return (int.Parse(value) - 10).ToString();
          default:
            throw new ArgumentOutOfRangeException(nameof(direction), direction, null);
        }
      });

      key1.Value = "1";
      key2.Value = "2";
      
      foreach (var s in new []{"1","2","3"}) ServerProtocol.Contexts.GetValueSet(key1).Add(s);
      AssertSetsEqual(new[] {"4", "5", "6"}, ClientProtocol.Contexts.GetValueSet(key1));

      foreach (var s in new []{"1","2","3"}) ServerProtocol.Contexts.GetValueSet(key2).Add(s);
      AssertSetsEqual(new[] {"11", "12", "13"}, ClientProtocol.Contexts.GetValueSet(key2));
      
      foreach (var s in new []{"9","10","11"}) ServerProtocol.Contexts.GetValueSet(key1).Add(s);
      AssertSetsEqual(new[] {"4", "5", "6", "12", "13", "14"}, ClientProtocol.Contexts.GetValueSet(key1));
      
      foreach (var s in new []{"9","10","11"}) ServerProtocol.Contexts.GetValueSet(key2).Add(s);
      AssertSetsEqual(new[] {"11", "12", "13", "19", "20", "21"}, ClientProtocol.Contexts.GetValueSet(key2));

      ServerProtocol.Contexts.GetValueSet(key1).Clear();
      AssertSetsEqual(Array.Empty<string>(), ClientProtocol.Contexts.GetValueSet(key1));
    }

    private void AssertSetsEqual<T>(ICollection<T> expected, ICollection<T> actual)
    {
      Assert.AreEqual(expected.OrderBy(it => it).ToArray(), actual.OrderBy(it => it).ToArray());
    }
  }
}