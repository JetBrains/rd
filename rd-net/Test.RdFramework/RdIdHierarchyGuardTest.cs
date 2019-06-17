using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using NUnit.Framework;
using Lifetime = JetBrains.Lifetimes.Lifetime;

namespace Test.RdFramework
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdIdHierarchyGuardTest : RdFrameworkTestBase
  {
    private static readonly int ourMapKey = 1;

    [Test]    
    public void Test()
    {
      var serverMap = BindToServer(LifetimeDefinition.Lifetime, new RdMap<int, Model> { IsMaster = true }, ourMapKey);
      var clientMap = BindToClient(LifetimeDefinition.Lifetime, new RdMap<int, Model> { IsMaster = false }, ourMapKey);      

      ServerProtocol.Serializers.Register(Model.Read, Model.Write);
      ClientProtocol.Serializers.Register(Model.Read, Model.Write);

      // create model on client and transfer
      clientMap.Add(1, new Model());
      ClientWire.TransmitAllMessages();
      

      // capture model
      Model capturedModel = null;
      serverMap.View(LifetimeDefinition.Lifetime, (modelLifetime, key, model) =>
      {
        capturedModel = model;
      });
      


      Assert.NotNull(capturedModel);

      
      
    }

    private class Model : RdBindableBase
    {
      private readonly RdProperty<int> myValue;

      public Model()
      {
        myValue = new RdProperty<int>();
      }

      private Model(RdProperty<int> modelProperty)
      {
        myValue = modelProperty;
      }

      public RdId Id { get { return myValue.RdId; } }

      public static Model Read(SerializationCtx ctx, UnsafeReader reader)
      {
        var modelProperty = RdProperty<int>.Read(ctx, reader);
        return new Model(modelProperty);
      }

      public static void Write(SerializationCtx ctx, UnsafeWriter writer, object value)
      {
        RdProperty<int>.Write(ctx, writer, ((Model)value).myValue);
      }

      protected override void Init(Lifetime lifetime)
      {
        myValue.Bind(lifetime, this, "myValue");
      }

      public override void Identify(IIdentities identities, RdId id)
      {
        myValue.Identify(identities, id);
      }
    }
  }
}