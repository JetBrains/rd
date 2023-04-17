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
    private readonly CopyOnWriteList<ISingleContextHandler> myCounterpartHandlers = new CopyOnWriteList<ISingleContextHandler>();
    private readonly IViewableList<ISingleContextHandler> myHandlerOrder = new ViewableList<ISingleContextHandler>(new CopyOnWriteList<ISingleContextHandler>());
    private readonly ConcurrentDictionary<RdContextBase, ISingleContextHandler> myHandlersMap = new ConcurrentDictionary<RdContextBase, ISingleContextHandler>();
    private readonly object myOrderingLock = new object();
    private readonly ThreadLocal<bool> mySendWithoutContexts = new ThreadLocal<bool>(() => false);
    
    
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

    public override RdWireableContinuation OnWireReceived(Lifetime lifetime, IProtocol proto, SerializationCtx ctx, UnsafeReader reader)
    {
      var contextBase = RdContextBase.Read(mySerializationCtx, reader);

      contextBase.RegisterOn(this);
      
      myCounterpartHandlers.Add(myHandlersMap[contextBase]);
      return RdWireableContinuation.Empty;
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

    private void PreBindHandler(Lifetime lifetime, string key, ISingleContextHandler handler)
    {
      if (handler is RdBindableBase bindableHandler)
      {
        bindableHandler.RdId = RdId.Mix(key);
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
    

    protected override void PreInit(Lifetime lifetime, IProtocol parentProto)
    {
      base.PreInit(lifetime, parentProto);
      
      lock (myOrderingLock)
      {
        myHandlerOrder.View(lifetime, (handlerLt, _, handler) =>
        {
          PreBindHandler(handlerLt, handler.ContextBase.Key, handler);
        });
      }
      
      parentProto.Wire.Advise(lifetime, this);
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
    internal MessageContextCookie ReadContextsIntoCookie(UnsafeReader reader)
    {
      var numContextValues = reader.ReadShort();
      var handlers = myCounterpartHandlers;
      if (Mode.IsAssertion) Assertion.Assert(numContextValues <= handlers.Count, "We know of {0} other side keys, received {1} instead", myCounterpartHandlers.Count, numContextValues);

      var values = new object[numContextValues];
      for (var i = 0; i < numContextValues; i++)
        values[i] = handlers[i].ReadValueBoxed(mySerializationCtx, reader);

      return new MessageContextCookie(values, myCounterpartHandlers.GetStorageUnsafe());
    }

    internal readonly struct MessageContextCookie : IDisposable
    {
      private readonly object[] myValues;
      private readonly ISingleContextHandler[] myHandlers;

      public MessageContextCookie(object[] values, ISingleContextHandler[] handlers)
      {
        myValues = values;
        myHandlers = handlers;
      }

      public void Update()
      {
        if (myHandlers == null)
          return;
        
        for (var i = 0; i < myValues.Length; i++) 
          myValues[i] = myHandlers[i].ContextBase.UpdateValueBoxed(myValues[i]);
      }
      
      public void Dispose()
      {
        if (myValues is {} values)
        {
          foreach (var cookie in values)
            ((IDisposable)cookie).Dispose();
        }
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
      
      var count = myHandlerOrder.Count;
      writer.Write((short) count);
      for (var i = 0; i < count; i++) 
        myHandlerOrder[i].WriteValue(mySerializationCtx, writer);
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
      writer.Write((short) 0);
    } 

    private void BindAndSendHandler(ISingleContextHandler handler)
    {
      SendContextToRemote(handler.ContextBase);
      BindHandler(handler);
    }
  }
}