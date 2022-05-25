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

    public class TestKeyHeavy : ThreadLocalRdContext<string>
    {
      private TestKeyHeavy() : base("test-key", true, Serializers.ReadString, Serializers.WriteString)
      {
      }
      
      public static readonly TestKeyHeavy Instance = new TestKeyHeavy();

      protected internal override void RegisterOn(ISerializers serializers)
      {
        serializers.Register((_, __) => Instance, (_, __, ___) => { });
      }
    }
    
    public class TestKeyLight : ThreadLocalRdContext<string>
    {
      private TestKeyLight() : base("test-key", false, Serializers.ReadString, Serializers.WriteString)
      {
      }
      
      public static readonly TestKeyLight Instance = new TestKeyLight();

      protected internal override void RegisterOn(ISerializers serializers)
      {
        serializers.Register((_, __) => Instance, (_, __, ___) => { });
      }
    }

    [Theory]
    public void TestLateAdd(bool heavy)
    {
      var key = heavy ? TestKeyHeavy.Instance : (RdContext<string>) TestKeyLight.Instance;
      
      var serverSignal = BindToServer(LifetimeDefinition.Lifetime, new RdSignal<string>(), 1);
      var clientSignal = BindToClient(LifetimeDefinition.Lifetime, new RdSignal<string>(), 1);

      key.RegisterOn(ClientProtocol.Serializers);
      ServerProtocol.Contexts.RegisterContext(key);

      key.Value = "1";

      Lifetime.Using(lt =>
      {
        var fired = false;
        clientSignal.Advise(lt, s =>
        {
          Assert.AreEqual("1", key.Value);
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
          Assert.AreEqual("1", key.Value);
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
          Assert.AreEqual("1", key.Value);
          fired = true;
        });
        clientSignal.Fire("");
        Assert.True(fired, "fired");
      });
      
      Assert.AreEqual("1", key.Value);
    }

    public class TestKey2 : ThreadLocalRdContext<string>
    {
      private TestKey2() : base("test-key", true, Serializers.ReadString, Serializers.WriteString)
      {
      }
      
      public static readonly TestKey2 Instance = new TestKey2();

      protected internal override void RegisterOn(ISerializers serializers)
      {
        serializers.Register((_, __) => Instance, (_, __, ___) => { });
      }
    }
  }
}