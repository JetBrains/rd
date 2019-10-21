using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Tasks;
using JetBrains.Threading;
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


    [Test]
    public void TestCancellation()
    {
      ClientWire.AutoTransmitMode = true;
      ServerWire.AutoTransmitMode = true;
      
      var serverEntity = BindToServer(LifetimeDefinition.Lifetime, new RdCall<Unit, string>(), ourKey);
      var clientEntity = BindToClient(LifetimeDefinition.Lifetime, CreateEndpoint<Unit, string>(x => x.ToString()), ourKey);

      bool handlerFinished = false; 
      bool handlerCompletedSuccessfully = false; 
      clientEntity.Set(async (lf, req) =>
      {
        try
        {
          await Task.Delay(500, lf);
          handlerCompletedSuccessfully = true;
        }
        finally
        {
          handlerFinished = true;
        }

        return "";
      });

      //1. explicit cancellation
      var ld = new LifetimeDefinition();
      var task = serverEntity.Start(ld.Lifetime, Unit.Instance).AsTask();
      ld.Terminate();

      SpinWaitEx.SpinUntil(() => task.IsCompleted);
      Assert.True(task.IsOperationCanceled());
      
      SpinWaitEx.SpinUntil(() => handlerFinished);
      Assert.False(handlerCompletedSuccessfully);
      
      
      //2. no cancellation
      handlerFinished = false;
      handlerCompletedSuccessfully = false;
      task = serverEntity.Start(new LifetimeDefinition().Lifetime, Unit.Instance).AsTask();
      SpinWaitEx.SpinUntil(() => task.IsCompleted);
      Assert.False(task.IsOperationCanceled());
      
      SpinWaitEx.SpinUntil(() => handlerFinished);
      Assert.True(handlerCompletedSuccessfully);
      
      
      //3. cancellation from parent lifetime
      handlerFinished = false;
      handlerCompletedSuccessfully = false;
      task = serverEntity.Start(new LifetimeDefinition().Lifetime, Unit.Instance).AsTask();
      LifetimeDefinition.Terminate();
      
      SpinWaitEx.SpinUntil(() => task.IsCompleted);
      Assert.True(task.IsOperationCanceled());
      
      SpinWaitEx.SpinUntil(() => handlerFinished);
      Assert.False(handlerCompletedSuccessfully);
    }
    
  }
}