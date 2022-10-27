using System;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdTaskTest : RdFrameworkTestBase
  {
    #if NET35
    static TaskHack Task = new TaskHack();
    #endif

    private static readonly int ourKey = 1;


    private RdCall<TIn, TOut> CreateEndpoint<TIn, TOut>(Func<TIn, TOut> handler, IScheduler cancellationScheduler = null, IScheduler handlerScheduler = null)
    {
      var res = NewRdCall<TIn, TOut>();
      res.Set(handler, cancellationScheduler, handlerScheduler);
      return res;
    }
    
    
    
    [Test]
    public void TestStatic()
    {
      ClientWire.AutoTransmitMode = true;
      ServerWire.AutoTransmitMode = true;

      var serverEntity = BindToServer(LifetimeDefinition.Lifetime, NewRdCall<int, string>(), ourKey);
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

      var serverEntity = BindToServer(LifetimeDefinition.Lifetime, NewRdCall<string, string>(), ourKey);
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
      
      var serverEntity = BindToServer(LifetimeDefinition.Lifetime, NewRdCall<Unit, string>(), ourKey);
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
      {
        var ld = new LifetimeDefinition();
        var task = serverEntity.Start(ld.Lifetime, Unit.Instance).AsTask();
        ld.Terminate();

        SpinWaitEx.SpinUntil(() => task.IsCompleted);
        Assert.True(task.IsOperationCanceled());
      
        SpinWaitEx.SpinUntil(() => handlerFinished);
        Assert.False(handlerCompletedSuccessfully);
      }
      
      
      //2. no cancellation
      {
        handlerFinished = false;
        handlerCompletedSuccessfully = false;
        var task = serverEntity.Start(new LifetimeDefinition().Lifetime, Unit.Instance).AsTask();
        SpinWaitEx.SpinUntil(() => task.IsCompleted);
        Assert.False(task.IsOperationCanceled());
      
        SpinWaitEx.SpinUntil(() => handlerFinished);
        Assert.True(handlerCompletedSuccessfully);
      }
      
      //3. terminatedLifetime
      {
        var task = serverEntity.Start(Lifetime.Terminated, Unit.Instance).AsTask();
        Assert.IsTrue(task.IsCanceled);
      }
      
      //4. cancellation from parent lifetime
      {
        handlerFinished = false;
        handlerCompletedSuccessfully = false;
        var task = serverEntity.Start(new LifetimeDefinition().Lifetime, Unit.Instance).AsTask();
        LifetimeDefinition.Terminate();
      
        SpinWaitEx.SpinUntil(() => task.IsCompleted);
        Assert.True(task.IsOperationCanceled());
      
        SpinWaitEx.SpinUntil(() => handlerFinished);
        Assert.False(handlerCompletedSuccessfully);
      }
    }

    
    
    [Test]
    public void TestBindable()
    {
      ClientWire.AutoTransmitMode = true;
      ServerWire.AutoTransmitMode = true;
      
      
      var call1 = new RdCall<Unit, RdSignal<int>>(Serializers.ReadVoid, Serializers.WriteVoid, RdSignal<int>.Read, RdSignal<int>.Write);
      var call2 = new RdCall<Unit, RdSignal<int>>(Serializers.ReadVoid, Serializers.WriteVoid, RdSignal<int>.Read, RdSignal<int>.Write);

      var respSignal = NewRdSignal<int>();
      var endpointLfTerminated = false;
      call2.Set((endpointLf, _) =>
      {
        endpointLf.OnTermination(() => endpointLfTerminated = true);
        return RdTask<RdSignal<int>>.Successful(respSignal);
      });
      
      var serverEntity = BindToServer(LifetimeDefinition.Lifetime, call1, ourKey);
      var clientEntity = BindToClient(LifetimeDefinition.Lifetime, call2, ourKey);

      var ld = new LifetimeDefinition();
      var lf = ld.Lifetime;
      var signal = call1.Start(lf, Unit.Instance).AsTask().GetOrWait(lf);
      var log = new List<int>();
      
      signal.Advise(Lifetime.Eternal, v =>
      {
        log.Add(v);
        Console.WriteLine(v);
      });
      
      respSignal.Fire(1);
      respSignal.Fire(2);
      respSignal.Fire(3);
      ld.Terminate();
      Assert.False(respSignal.IsBound);

      SpinWaitEx.SpinUntil(() => log.Count >= 3);
      Thread.Sleep(100);
      Assert.AreEqual(new [] {1, 2, 3}, log.ToArray());
      
      Assert.True(endpointLfTerminated);
    }
    
    [Test]
    public void TestOverriddenHandlerScheduler()
    {
      ClientWire.AutoTransmitMode = true;
      ServerWire.AutoTransmitMode = true;

      var scheduler = SingleThreadScheduler.RunOnSeparateThread(TestLifetime, "Background scheduler");

      var callsite = BindToServer(LifetimeDefinition.Lifetime, NewRdCall<int, string>(), ourKey);
      var endpoint = BindToClient(LifetimeDefinition.Lifetime, NewRdCall<int, string>(), ourKey);
      
      Assert.IsFalse(scheduler.IsActive);

      var point1 = false;
      var point2 = false;
      var thread = Thread.CurrentThread;
      endpoint.Set(i =>
      {
        Assert.IsTrue(scheduler.IsActive);
        Assert.AreNotEqual(thread, Thread.CurrentThread);
        
        Volatile.Write(ref point1, true);
        
        SpinWaitEx.SpinUntil(TestLifetime, TimeSpan.FromSeconds(10), () => point2);
        Assert.IsTrue(point2);
        
        return i.ToString();
      }, handlerScheduler: scheduler);

      Assert.IsFalse(scheduler.IsActive);
      
      var task = callsite.Start(0);
      var result = task.Result;
      
      Assert.IsFalse(result.Maybe.HasValue);
      
      SpinWaitEx.SpinUntil(TestLifetime, TimeSpan.FromSeconds(10), () => point1);
      Assert.IsTrue(point1);
      Assert.IsFalse(result.Maybe.HasValue);
      
      Volatile.Write(ref point2, true);
      SpinWaitEx.SpinUntil(TestLifetime, () => result.Maybe.HasValue);
      
      Assert.AreEqual("0", result.Value.Result);
    }
  }
}