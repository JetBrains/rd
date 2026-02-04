using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Threading;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Serialization;
using JetBrains.Util;

namespace JetBrains.Rd.Impl
{
  /// <summary>
  /// This class handles RdContext on protocol level. It tracks existing contexts and allows access to their value sets (when present)
  /// </summary>
  public class ProtocolContexts : RdReactiveBase
  {
    private readonly CopyOnWriteList<ISingleContextHandler> myCounterpartHandlers = new();
    private readonly CopyOnWriteList<ISingleContextHandler> myHandlersToWrite = new();
    private readonly IViewableList<ISingleContextHandler> myHandlerOrder = new ViewableList<ISingleContextHandler>();
    private readonly ConcurrentDictionary<RdContextBase, ISingleContextHandler> myHandlersMap = new();
    private readonly object myOrderingLock = new();
    private readonly ThreadLocal<bool> mySendWithoutContexts = new(() => false);


    internal readonly struct SendWithoutContextsCookie : IDisposable
    {
      private readonly ProtocolContexts myContexts;
      private readonly bool myPrevValue;

      public SendWithoutContextsCookie(ProtocolContexts contexts)
      {
        myContexts = contexts;
        myPrevValue = contexts.mySendWithoutContexts.Value;
        contexts.mySendWithoutContexts.Value = true;
      }

      public void Dispose()
      {
        myContexts.mySendWithoutContexts.Value = myPrevValue;
      }
    }

    private readonly SerializationCtx mySerializationCtx;

    internal SendWithoutContextsCookie CreateSendWithoutContextsCookie() => new SendWithoutContextsCookie(this);
    public bool IsSendWithoutContexts => mySendWithoutContexts.Value;

    public ProtocolContexts(SerializationCtx serializationCtx)
    {
      Async = true;
      mySerializationCtx = serializationCtx;
    }

    public ICollection<RdContextBase> RegisteredContexts => myHandlersMap.Keys;

    internal ISingleContextHandler<T> GetHandlerForContext<T>(RdContext<T> context)
    {
      return (ISingleContextHandler<T>) myHandlersMap[context];
    }

    public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper)
    {
      var contextBase = RdContextBase.Read(mySerializationCtx, reader);

      contextBase.RegisterOn(this);

      myCounterpartHandlers.Add(myHandlersMap[contextBase]);
    }

    private void DoAddHandler<T>(RdContext<T> context, ISingleContextHandler<T> handler)
    {
      if (myHandlersMap.TryAdd(context, handler))
      {
        context.RegisterOn(mySerializationCtx.Serializers);
        lock (myOrderingLock)
          myHandlerOrder.Add(handler);
      }
    }

    private void PreBindHandler(Lifetime lifetime, string key, ISingleContextHandler handler, IProtocol proto)
    {
      if (handler is RdBindableBase bindableHandler)
      {
        bindableHandler.RdId = proto.Identities.Mix(RdId, key);
        bindableHandler.PreBind(lifetime, this, key);
      }
    }

    private void BindHandler(ISingleContextHandler handler)
    {
      if (handler is RdBindableBase bindableHandler)
      {
        using (CreateSendWithoutContextsCookie())
          bindableHandler.Bind();
      }
    }


    private void SendContextToRemote(RdContextBase context)
    {
      var wire = TryGetProto()?.Wire;
      if (wire == null)
        return;

      using(CreateSendWithoutContextsCookie())
        wire.Send(RdId, writer =>
        {
          RdContextBase.Write(mySerializationCtx, writer, context);
        });
    }

    private void EnsureHeavyHandlerExists<T>(RdContext<T> context)
    {
      if (Mode.IsAssertion) Assertion.Assert(context.IsHeavy, "key.IsHeavy");
      if (!myHandlersMap.ContainsKey(context))
        DoAddHandler(context, new HeavySingleContextHandler<T>(context, this));
    }

    private void EnsureLightHandlerExists<T>(RdContext<T> context)
    {
      if (Mode.IsAssertion) Assertion.Assert(!context.IsHeavy, "!key.IsHeavy");
      if (!myHandlersMap.ContainsKey(context))
        DoAddHandler(context, new LightSingleContextHandler<T>(context));
    }

    /// <summary>
    /// Get a value set for a given key. The values are local relative to transform
    /// </summary>
    public IAppendOnlyViewableConcurrentSet<T> GetValueSet<T>(RdContext<T> context) where T : notnull
    {
      if (Mode.IsAssertion) Assertion.Assert(context.IsHeavy, "Only heavy keys have value sets, key {0} is light", context.Key);
      return ((HeavySingleContextHandler<T>) GetHandlerForContext(context)).LocalValueSet;
    }

    /// <summary>
    /// Registers a context to be used with this context handler. Must be invoked on protocol's scheduler
    /// </summary>
    public void RegisterContext<T>(RdContext<T> context)
    {
      if (myHandlersMap.ContainsKey(context)) return;
      if(context.IsHeavy)
        EnsureHeavyHandlerExists(context);
      else
        EnsureLightHandlerExists(context);
    }


    protected override void PreInit(Lifetime lifetime, IProtocol proto)
    {
      base.PreInit(lifetime, proto);

      lock (myOrderingLock)
      {
        myHandlerOrder.View(lifetime, (handlerLt, _, handler) =>
        {
          PreBindHandler(handlerLt, handler.ContextBase.Key, handler, proto);
        });
      }

      proto.Wire.Advise(lifetime, this);
    }

    protected override void Init(Lifetime lifetime, IProtocol proto, SerializationCtx ctx)
    {
      base.Init(lifetime, proto, ctx);
      lock (myOrderingLock)
      {
        myHandlerOrder.View(lifetime, (handlerLt, _, handler) =>
        {
          BindAndSendHandler(handler);
        });
      }

    }

    /// <summary>
    /// Reads context values from a message, sets current context to them, and returns a cookie to restore previous context
    /// </summary>
    internal MessageContext ReadContextsIntoCookie(UnsafeReader reader)
    {
      var numContextValues = reader.ReadShort();
      if (numContextValues == 0)
        return default;

      var handlers = myCounterpartHandlers;
      if (Mode.IsAssertion) Assertion.Assert(numContextValues <= handlers.Count, "We know of {0} other side keys, received {1} instead", handlers.Count, numContextValues);

      var values = new object[numContextValues];
      for (var i = 0; i < numContextValues; i++)
        values[i] = handlers[i].ReadValueBoxed(mySerializationCtx, reader);

      return new MessageContext(values, handlers.GetStorageUnsafe());
    }

    internal readonly ref struct MessageContextCookie
    {
      private readonly IDisposable[] myDisposables;

      public MessageContextCookie(IDisposable[] disposables)
      {
        myDisposables = disposables;
      }

      public void Dispose()
      {
        if (myDisposables is { } disposables)
        {
          foreach (var disposable in disposables)
            disposable.Dispose();
        }
      }
    }

    internal readonly struct MessageContext
    {
      private readonly object[] myValues;
      private readonly ISingleContextHandler[] myHandlers;

      public MessageContext(object[] values, ISingleContextHandler[] handlers)
      {
        myValues = values;
        myHandlers = handlers;
      }

      public MessageContextCookie UpdateCookie()
      {
        if (myHandlers == null)
          return default;

        var disposables = new IDisposable[myValues.Length];
        for (var i = 0; i < myValues.Length; i++)
          disposables[i] = myHandlers[i].ContextBase.UpdateValueBoxed(myValues[i]);

        return new MessageContextCookie(disposables);
      }
    }

    /// <summary>
    /// Writes the current context values
    /// </summary>
    [SuppressMessage("ReSharper", "InconsistentlySynchronizedField", Justification = "sync is for atomicity of write/send pairs, not access")]
    public void WriteContexts(UnsafeWriter writer)
    {
      if (IsSendWithoutContexts)
      {
        WriteEmptyContexts(writer);
        return;
      }
// all handlers in myHandlersToWrite have been sent to the remote side
      var count = myHandlersToWrite.Count;
      writer.WriteInt16((short) count);
      for (var i = 0; i < count; i++)
        myHandlersToWrite[i].WriteValue(mySerializationCtx, writer);
    }

    /// <summary>
    /// Adds current values of registered contexts to their respective value sets without writing them to the wire
    /// </summary>
    [SuppressMessage("ReSharper", "InconsistentlySynchronizedField", Justification = "sync is for atomicity of write/send pairs, not access")]
    public void RegisterCurrentValuesInValueSets()
    {
      var count = myHandlerOrder.Count;
      for (var i = 0; i < count; i++)
        myHandlerOrder[i].RegisterValueInValueSet();
    }

    /// <summary>
    /// Writes an empty context
    /// </summary>
    public static void WriteEmptyContexts(UnsafeWriter writer)
    {
      writer.WriteInt16((short) 0);
    }

    private void BindAndSendHandler(ISingleContextHandler handler)
    {
      SendContextToRemote(handler.ContextBase);
      BindHandler(handler);
      // add the handler to myHandlersToWrite only after sending the context to remote
      myHandlersToWrite.Add(handler);
    }
  }
}