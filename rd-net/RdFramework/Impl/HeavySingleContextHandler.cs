using System;
using System.Collections;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

#nullable disable

namespace JetBrains.Rd.Impl
{
  internal class HeavySingleContextHandler<T> : RdReactiveBase, ISingleContextHandler<T>
  {
    private readonly ProtocolContexts myHandler;
    private readonly InternRoot<T> myInternRoot;
    private readonly ConcurrentRdSet<T> myProtocolValueSet;

    internal IAppendOnlyViewableConcurrentSet<T> LocalValueSet => myProtocolValueSet;

    public HeavySingleContextHandler(RdContext<T> context, ProtocolContexts handler)
    {
      myHandler = handler;
      Context = context;
      myInternRoot = new InternRoot<T>(context.ReadDelegate, context.WriteDelegate);
      myProtocolValueSet = new ConcurrentRdSet<T>(context.ReadDelegate, context.WriteDelegate, myHandler);
    }

    public object ReadValueBoxed(SerializationCtx context, UnsafeReader reader) => ReadValue(context, reader);

    public RdContextBase ContextBase => Context;

    public RdContext<T> Context { get; }

    protected override void PreInit(Lifetime lifetime, IProtocol proto)
    {
      base.PreInit(lifetime, proto);

      myInternRoot.RdId = proto.Identities.Mix(RdId, "InternRoot");;
      myProtocolValueSet.RdId = proto.Identities.Mix(RdId, "ValueSet");

      myInternRoot.PreBind(lifetime, this, "InternRoot");
      myProtocolValueSet.PreBind(lifetime, this, "ValueSet");
    }

    protected override void Init(Lifetime lifetime, IProtocol proto, SerializationCtx ctx)
    {
      base.Init(lifetime, proto, ctx);
      Assertion.Assert(myHandler.IsSendWithoutContexts,"Must bind context handler without sending contexts to prevent reentrancy");

      myInternRoot.Bind();
      myProtocolValueSet.Bind();

      myProtocolValueSet.View(lifetime, HandleProtocolSetEvent);
    }

    private void HandleProtocolSetEvent(Lifetime lifetime, T value)
    {
      using (myHandler.CreateSendWithoutContextsCookie())
      {
        lifetime.TryBracket(() =>
        {
          myInternRoot.Intern(value);
        }, () =>
        {
          myInternRoot.Remove(value);
        });
      }
    }


    public void WriteValue(SerializationCtx context, UnsafeWriter writer)
    {
      Assertion.Assert(!myHandler.IsSendWithoutContexts, "!myHandler.IsWritingOwnMessages");
      var value = Context.Value;
      if (value == null)
      {
        InternId.Write(writer, InternId.Invalid);
        writer.WriteBoolean(false);
      }
      else
      {
        using (myHandler.CreateSendWithoutContextsCookie())
        {
          AddValueToProtocolValueSetImpl(value);

          var internedId = myInternRoot.Intern(value);
          InternId.Write(writer, internedId);
          if (!internedId.IsValid)
          {
            writer.WriteBoolean(true);
            Context.WriteDelegate(context, writer, value);
          }
        }
      }
    }

    private void AddValueToProtocolValueSetImpl(T value)
    {
      myProtocolValueSet.Add(value);
    }

    public void RegisterValueInValueSet()
    {
      var value = Context.Value;
      if (value == null) return;

      using (myHandler.CreateSendWithoutContextsCookie())
      {
        AddValueToProtocolValueSetImpl(value);
      }
    }

    public T ReadValue(SerializationCtx context, UnsafeReader reader)
    {
      var id = InternId.Read(reader);
      if (!id.IsValid)
      {
        var hasValue = reader.ReadBool();
        if (hasValue)
          return Context.ReadDelegate(context, reader);
        return default;
      }

      return myInternRoot.UnIntern<T>(id);
    }

    public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper)
    {
      var message = "HeavySingleContextHandler can't receive messages";
      Assertion.Fail(message);
    }
  }

  internal class ConcurrentRdSet<T> : RdReactiveBase, IAppendOnlyViewableConcurrentSet<T>, IRdWireable
  {
    private readonly ProtocolContexts myProtocolContexts;
    private readonly ViewableConcurrentSet<T> mySet;
    private readonly ThreadLocal<bool> myIsThreadLocal = new();

    public int Count => mySet.Count;

    public CtxReadDelegate<T> ReadValueDelegate { get; }
    public CtxWriteDelegate<T> WriteValueDelegate { get; }

    public ConcurrentRdSet(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue, ProtocolContexts protocolContexts, IEqualityComparer<T> comparer = null)
    {
      myProtocolContexts = protocolContexts;
      ReadValueDelegate = readValue;
      WriteValueDelegate = writeValue;
      mySet = new ViewableConcurrentSet<T>(comparer);
      Async = true;
    }

    protected override void PreInit(Lifetime lifetime, IProtocol proto)
    {
      base.PreInit(lifetime, proto);
      proto.Wire.Advise(lifetime, this);
    }

    protected override void Init(Lifetime bindLifetime, IProtocol proto, SerializationCtx ctx)
    {
      View(bindLifetime, (_, value) =>
      {
        if (!myIsThreadLocal.Value) return;

        SendAdd(proto.Wire, SendContext.Of(ctx, value, this));
      });
    }

    private void SendAdd(IWire wire, SendContext<T, ConcurrentRdSet<T>> context)
    {
      wire.Send(RdId, context, static (sendContext, stream) =>
      {
        var value = sendContext.Event;
        const AddRemove kind = AddRemove.Add;
        stream.Write((int)kind);
        sendContext.This.WriteValueDelegate(sendContext.SzrCtx, stream, value);

        ourLogSend.Trace($"{sendContext.This} :: {kind} :: {value.PrintToString()}");
      });
    }

    public override void Print(PrettyPrinter printer)
    {
      base.Print(printer);
      if (!printer.PrintContent) return;

      printer.Print(" [");
      if (Count > 0) printer.Println();

      using (printer.IndentCookie())
      {
        foreach (var v in mySet)
        {
          v.PrintEx(printer);
          printer.Println();
        }
      }
      printer.Println("]");
    }

    public bool Add(T value)
    {
      Assertion.Assert(!myIsThreadLocal.Value);

      myIsThreadLocal.Value = true;
      try
      {
        using (UsingDebugInfo())
        using (myProtocolContexts.CreateSendWithoutContextsCookie())
          return mySet.Add(value);
      }
      finally
      {
        myIsThreadLocal.Value = false;
      }
    }

    public bool Contains(T value) => mySet.Contains(value);
    public void View(Lifetime lifetime, Action<Lifetime, T> action) => mySet.View(lifetime, action);

    public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper)
    {
      var kind = (AddRemove)reader.ReadInt();
      var value = ReadValueDelegate(ctx, reader);
      ReceiveTrace?.Log($"{this} :: {kind} :: {value.PrintToString()}");
      switch (kind)
      {
        case AddRemove.Add:
          mySet.Add(value);
          break;

        case AddRemove.Remove:
          mySet.Remove(value);
          break;

        default:
          throw new ArgumentOutOfRangeException();
      }
    }

    public IEnumerator<T> GetEnumerator() => mySet.GetEnumerator();
    IEnumerator IEnumerable.GetEnumerator() => GetEnumerator();
  }
}