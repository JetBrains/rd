using System;
using JetBrains.Diagnostics;
using JetBrains.Rd.Tasks;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdTaskTest : RdFrameworkTestBase
  {
    private static readonly int ourKey = 1;


    private RdCall<TIn, TOut> CreateEndpoint<TIn, TOut>(Func<TIn, TOut> handler)
    {
      var res = new RdCall<TIn, TOut>();
      res.Set(handler);
      return res;
    }
    
    
    [Test]
    public void TestStatic()
    {
      ClientWire.AutoTransmitMode = true;
      ServerWire.AutoTransmitMode = true;

      var serverEntity = BindToServer(LifetimeDefinition.Lifetime, new RdCall<int, string>(), ourKey);
      var clientEntity = BindToClient(LifetimeDefinition.Lifetime, CreateEndpoint<int, string>(x => x.ToString()), ourKey);


      Assert.AreEqual("0", serverEntity.Sync(0));
      Assert.AreEqual("1", serverEntity.Sync(1));

      var task = serverEntity.Start(0);
      Assert.AreEqual(RdTaskStatus.Success, task.Result.Value.Status);
    }
    

    [Test]
    public void TestNullability()
    {
      ClientWire.AutoTransmitMode = true;
      ServerWire.AutoTransmitMode = true;

      var serverEntity = BindToServer(LifetimeDefinition.Lifetime, new RdCall<string, string>(), ourKey);
      var clientEntity = BindToClient(LifetimeDefinition.Lifetime, CreateEndpoint<string, string>(x => x.ToString()), ourKey);
      clientEntity.Set((lf, req) => RdTask<string>.Successful(req == null ? "NULL" : null));

      Assert.Throws<Assertion.AssertionException>(() =>
      {
        serverEntity.Sync(null);
      });

      Assert.Throws<Assertion.AssertionException>(() =>
      {
        using (Log.UsingLogFactory(new TestThrowingLogFactory()))
        {
          var task = serverEntity.Start("Value");
          Assert.AreEqual(RdTaskStatus.Faulted, task.Result.Value.Status);
        }
      });
    }
  }
}