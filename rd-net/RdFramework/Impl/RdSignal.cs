using System;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
  public class RdSignal<T> : RdReactiveBase, ISignal<T>
  {
    #region Serializers

    public CtxReadDelegate<T> ReadValueDelegate { get; }
    public CtxWriteDelegate<T> WriteValueDelegate { get; }

    public static RdSignal<T> Read(SerializationCtx ctx, UnsafeReader reader)
    {
      return Read(ctx, reader, Polymorphic<T>.Read, Polymorphic<T>.Write);
    }

    public static RdSignal<T> Read(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
    {
      var id = reader.ReadRdId();
      return new RdSignal<T>(readValue, writeValue).WithId(id);
    }

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdSignal<T> value)
    {
      writer.Write(value.RdId);
    }

    #endregion

    private readonly Signal<T> mySignal = new Signal<T>();

    public new SerializationCtx SerializationContext { get; private set; }
    
    public override IScheduler WireScheduler => Scheduler ?? DefaultScheduler;

    public IScheduler Scheduler { get; set; }


    public RdSignal() : this(Polymorphic<T>.Read, Polymorphic<T>.Write)
    {
    }

    public RdSignal(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
    {
      ReadValueDelegate = readValue;
      WriteValueDelegate = writeValue;
    }


    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      SerializationContext = base.SerializationContext; //caching context because of we could listen on
      Wire.Advise(lifetime, this);
    }

    public override void OnWireReceived(UnsafeReader reader)
    {
      var value = ReadValueDelegate(SerializationContext, reader);
      ReceiveTrace?.Log($"{this} :: value = {value.PrintToString()}");
      using (UsingDebugInfo())
        mySignal.Fire(value);
    }


    public void Fire(T value)
    {
//      AssertBound(); // todo: smart assert: fail if fired before bind; this allows not bound signals in UI
      if (!Async) AssertThreading();

      AssertNullability(value);

      var wire = Parent?.Proto.Wire;

      if (wire == null && Async)
        return;

      //local change
      wire.NotNull(this).Send(RdId, SendContext.Of(SerializationContext, value, this), (sendContext, stream) =>
      {
        var me = sendContext.This;
        SendTrace?.Log($"{me} :: value = {sendContext.Event.PrintToString()}");

        me.WriteValueDelegate(sendContext.SzrCtx, stream, sendContext.Event);
      });
      
      using (UsingDebugInfo())
        mySignal.Fire(value);
    }


    public void Advise(Lifetime lifetime, Action<T> handler)
    {
      if (IsBound) AssertThreading();

      mySignal.Advise(lifetime, handler);
    }

    protected override string ShortName => "signal";
  }
}