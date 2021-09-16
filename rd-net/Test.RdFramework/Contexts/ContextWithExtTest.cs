using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using NUnit.Framework;
using Test.RdFramework.Interning;

namespace Test.RdFramework.Contexts
{
  public class ContextWithExtTest : RdFrameworkTestBase
  {
    [Datapoint]
    public static bool TrueValue = true;

    [Datapoint]
    public static bool FalseValue = false;
    
    [Theory]
    public void TestExtPreserveContextOnLateConnect(bool useHeavyContext)
    {
      var context = useHeavyContext
        ? RdContextBasicTest.TestKeyHeavy.Instance
        : (RdContext<string>) RdContextBasicTest.TestKeyLight.Instance;
      
      context.RegisterOn(ClientProtocol.Serializers);
      context.RegisterOn(ServerProtocol.Contexts);

      var serverModel = new InterningRoot1(LifetimeDefinition.Lifetime, ServerProtocol);
      var clientModel = new InterningRoot1(LifetimeDefinition.Lifetime, ClientProtocol);

      var serverExt = serverModel.GetOrCreateExtension("test", () => new InterningExt());

      var fireValues = new[] { "a", "b", "c" };

      ServerWire.AutoTransmitMode = false;
      ClientWire.AutoTransmitMode = false;
      
      foreach (var fireValue in fireValues)
      {
        context.Value = fireValue;
        serverExt.Root.Value = new InterningExtRootModel();
        context.Value = null;
      }

      var numReceives = 0;
      var receivedContexts = new HashSet<string>();

      var clientExt = clientModel.GetOrCreateExtension("test", () => new InterningExt());
      clientExt.Root.AdviseNotNull(LifetimeDefinition.Lifetime, _ =>
      {
        numReceives++;
        receivedContexts.Add(context.Value);
      });

      while (ClientWire.HasMessages || ServerWire.HasMessages)
      {
        ClientWire.TransmitAllMessages();
        ServerWire.TransmitAllMessages();
      }

      Assert.AreEqual(3, numReceives);
      Assert.AreEqual(fireValues, receivedContexts);
    }
  }
}