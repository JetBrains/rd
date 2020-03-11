using System;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ProxyGeneratorModelTest : ProxyGeneratorTestBase
  {
    private static string ourState = null;

    protected override bool IsAsync => true;

    [RdRpc]
    public interface IModelOwner
    {
      Task<LiveModel> M(Lifetime m);
    }

    [RdExt]
    public class ModelOwner : RdExtReflectionBindableBase, IModelOwner
    {
      private readonly ReflectionRdActivator myActivator;

      public ModelOwner(ReflectionRdActivator activator)
      {
        myActivator = activator;
      }

      public Task<LiveModel> M(Lifetime m)
      {
        var model = myActivator.Activate<LiveModel>();
        model.Values.Advise(m, e => ourState = e.NewValue);
        return Task.FromResult(model);
      }
    }

    [RdModel]
    public class LiveModel : RdReflectionBindableBase
    {
      public IViewableList<string> Values { get; }
    }

    [Test]
    public async Task Test()
    {
      await YieldToClient();
      var client = CFacade.ActivateProxy<IModelOwner>(TestLifetime, ClientProtocol);

      await YieldToServer();
      var server = SFacade.InitBind(new ModelOwner(SFacade.Activator), TestLifetime, ServerProtocol);

      await Wait();

      await YieldToClient();
      var liveModel = await client.M(TestLifetime);
      await YieldToClient();
      liveModel.Values.Add("Test");

      await Wait();
      Assert.AreEqual("Test", ourState);
    }
  }
}