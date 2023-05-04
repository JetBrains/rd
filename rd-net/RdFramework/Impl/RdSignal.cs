using System;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Serialization;
using JetBrains.Threading;

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

    public static RdSignal<T> Read(SerializationCtx _, UnsafeReader reader, CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
    {
      var id = reader.ReadRdId();
      return new RdSignal<T>(readValue, writeValue).WithId(id);
    }

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdSignal<T> value)
    {
      writer.Write(value.RdId);
    }

    #endregion

    private readonly Signal<T> mySignal = new();

    public IScheduler? Scheduler { get; set; }


    public RdSignal(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
    {
      ReadValueDelegate = readValue;
      WriteValueDelegate = writeValue;
    }

    protected override void PreInit(Lifetime lifetime, IProtocol parentProto)
    {
      base.PreInit(lifetime, parentProto);

      parentProto.Wire.Advise(lifetime, this);
    }

    public override RdWireableContinuation OnWireReceived(Lifetime lifetime, IProtocol proto, SerializationCtx ctx, UnsafeReader reader, UnsynchronizedConcurrentAccessDetector? _)
    {
      var value = ReadValueDelegate(ctx, reader);
      ReceiveTrace?.Log($"{this} :: value = {value.PrintToString()}");
      var scheduler = Scheduler ?? proto.Scheduler;
      return new RdWireableContinuation(lifetime, scheduler, null, () =>
      {
        using (UsingDebugInfo())
          mySignal.Fire(value);
      });
    }

    public void Fire(T value)
    {
      AssertThreading();
      
      var proto = TryGetProto();
      if (!Async || proto is { Scheduler.IsActive: true })
        AssertBound();

      AssertNullability(value);

      var wire = proto?.Wire;
      if (wire == null && Async)
        return;
      
      if (!TryGetSerializationContext(out var serializationCtx))
        return;

      //local change
      wire.NotNull(this).Send(RdId, SendContext.Of(serializationCtx, value, this), static (sendContext, stream) =>
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
      mySignal.Advise(lifetime, handler);
      
      if (IsBound)
        AssertThreading();
    }

    protected override string ShortName => "signal";
  }
}