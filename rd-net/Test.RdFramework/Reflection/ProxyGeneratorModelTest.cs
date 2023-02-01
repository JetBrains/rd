using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  /// <summary>
  /// Live models can be returned from calls. Provided lifetime in method's parameters should define the lifetime of
  /// "connection" of two sides.
  /// </summary>
  [TestFixture]
  public class ProxyGeneratorModelTest : ProxyGeneratorTestBase
  {
#if NET35
    private static TaskHack Task = new TaskHack();
#endif

    protected override bool IsAsync => true;

    [RdRpc]
    public interface IModelOwner
    {
      Task<LiveModel> QueryModel(Lifetime m);
      LiveModel QueryModelSync(Lifetime m);
    }

    [RdExt]
    public class ModelOwner : RdExtReflectionBindableBase, IModelOwner
    {
      public string State;

      private readonly ReflectionRdActivator myActivator;

      public ModelOwner(ReflectionRdActivator activator)
      {
        myActivator = activator;
      }

      public Task<LiveModel> QueryModel(Lifetime m)
      {
        return Task.FromResult(QueryModelSync(m));
      }

      public LiveModel QueryModelSync(Lifetime m)
      {
        var model = myActivator.Activate<LiveModel>();
        model.Values.Advise(m, e => State = e.NewValue);
        return model;
      }
    }

    [RdModel]
    public class LiveModel : RdReflectionBindableBase
    {
      public IViewableList<string> Values { get; }
    }

    [Test]
    public async Task TestAsync()
    {
      await YieldToClient();
      var client = CFacade.ActivateProxy<IModelOwner>(TestLifetime, ClientProtocol);

      await YieldToServer();
      var server = SFacade.InitBind(new ModelOwner(SFacade.Activator), TestLifetime, ServerProtocol);

      await Wait();

      await YieldToClient();
      var liveModel = await client.QueryModel(TestLifetime);
      await YieldToClient();
      liveModel.Values.Add("Test");

      await Wait();
      Assert.AreEqual("Test", server.State);
    }

    [Test, Repeat(10), Description("Repeat test as it reveal cancellation race")]
    public async Task TestSyncCall()
    {
      SwitchingScheduler.Disable(TestLifetime);

      await YieldToClient();
      var client = CFacade.ActivateProxy<IModelOwner>(TestLifetime, ClientProtocol);
      
      await YieldToServer();
      var server = SFacade.InitBind(new ModelOwner(SFacade.Activator), TestLifetime, ServerProtocol);

      await Wait();

      LiveModel liveModel;
      using (var modelLifetimeDef = new LifetimeDefinition(TestLifetime))
      {
        await YieldToClient();
        liveModel = client.QueryModelSync(modelLifetimeDef.Lifetime);
        liveModel.Values.Add("proper set");
      }
      liveModel.Values.Add("unbound set");
      await Wait();
      Assert.AreEqual("proper set", server.State);
    }
  }
}