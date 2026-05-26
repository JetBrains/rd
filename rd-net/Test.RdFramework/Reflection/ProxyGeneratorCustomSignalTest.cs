using System;
using System.Drawing;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using JetBrains.Rd.Util;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  public class ProxyGeneratorCustomSignalTest : RdReflectionTestBase
  {
    [Test]
    public void TestCustomSignal()
    {
      var client = CFacade.InitBind(new ExtWithCustomSignal(), TestLifetime, ClientProtocol);
      var proxy = SFacade.ActivateProxy<IExtWithCustomSignal>(TestLifetime, ServerProtocol);

      int val = 0;
      proxy.Signal.Advise(TestLifetime, v => val = int.Parse(v.v1));
      
      proxy.Signal.Fire(new Payload("123", "456"));
      Assert.AreEqual(val, 123);

      client.Signal.Fire(new Payload("12", "34"));
      Assert.AreEqual(val, 12);
    }


    [RdRpc]
    public interface IExtWithCustomSignal
    {
      CustomProperty<Payload> Signal { get; }
    }

    [RdExt]
    public class ExtWithCustomSignal : RdExtReflectionBindableBase, IExtWithCustomSignal
    {
      public CustomProperty<Payload> Signal { get; set; }
    }

    public sealed class CustomProperty<T> : ISignal<T>, IRdBindable
    {
      private readonly string myId;
      private readonly RdSignal<T> myRdSignal;

      public CustomProperty(string id, CtxReadDelegate<T> read, CtxWriteDelegate<T> write)
      {
        myId = id;
        myRdSignal = new RdSignal<T>(read, write);
        myRdSignal.ValueCanBeNull = true;
      }

      public void Advise(Lifetime lifetime, Action<T> handler)
      {
        myRdSignal.Advise(lifetime, handler);
      }

      public IScheduler Scheduler
      {
        get => myRdSignal.Scheduler;
        set => myRdSignal.Scheduler = value;
      }

      public void Fire(T value)
      {
        myRdSignal.Fire(value);
      }

      public IProtocol TryGetProto() => myRdSignal.TryGetProto();

      public RName Location => myRdSignal.Location;
      public void Print(PrettyPrinter printer)
      {
        myRdSignal.Print(printer);
      }

      public RdId RdId
      {
        get => myRdSignal.RdId;
        set => myRdSignal.RdId = value;
      }

      public void PreBind(Lifetime lf, IRdDynamic parent, string name)
      {
        myRdSignal.PreBind(lf, parent, name);
      }
      
      public void Bind()
      {
        myRdSignal.Bind();
      }

      public bool TryGetSerializationContext(out SerializationCtx ctx) => myRdSignal.TryGetSerializationContext(out ctx);

      public void Identify(IIdentities identities, RdId id)
      {
        myRdSignal.Identify(identities, id);
      }
    }
  }

  public class Payload
  {
    public string v1;
    public string v2;

    public Payload(string v1, string v2)
    {
      this.v1 = v1;
      this.v2 = v2;

    }
  }
}