using System;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework.Contexts
{
  public class RdContextBasicTest : RdFrameworkTestBase
  {
    [Datapoint]
    public static bool TrueDataPoint = true;
    [Datapoint]
    public static bool FalseDataPoint = false;


    public override void SetUp()
    {
      base.SetUp();
      ServerWire.AutoTransmitMode = true;
      ClientWire.AutoTransmitMode = true;
    }

    [Theory]
    public void TestLateAdd(bool heavy)
    {
      var key = new RdContext<string>("test-key", true, Serializers.ReadString, Serializers.WriteString);
      
      var serverSignal = BindToServer(LifetimeDefinition.Lifetime, new RdSignal<string>(), 1);
      var clientSignal = BindToClient(LifetimeDefinition.Lifetime, new RdSignal<string>(), 1);

      ServerProtocol.Contexts.RegisterContext(key);
      
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

      key.Value = "1";

      Lifetime.Using(lt =>
      {
        var fired = false;
        clientSignal.Advise(lt, s =>
        {
          Assert.AreEqual("4", key.Value);
          fired = true;
        });
        serverSignal.Fire("");
        Assert.True(fired, "fired");
      });
      
      ClientProtocol.Contexts.RegisterContext(key);
      
      Lifetime.Using(lt =>
      {
        var fired = false;
        clientSignal.Advise(lt, s =>
        {
          Assert.AreEqual("4", key.Value);
          fired = true;
        });
        serverSignal.Fire("");
        Assert.True(fired, "fired");
      });

      Assert.AreEqual("1", key.Value);
      
      Lifetime.Using(lt =>
      {
        var fired = false;
        serverSignal.Advise(lt, s =>
        {
          Assert.AreEqual("-2", key.Value);
          fired = true;
        });
        clientSignal.Fire("");
        Assert.True(fired, "fired");
      });
      
      Assert.AreEqual("1", key.Value);
    }
  }
}