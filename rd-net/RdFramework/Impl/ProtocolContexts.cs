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
    
    
    internal struct SendWithoutContextsCookie : IDisposable
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

    public override SerializationCtx SerializationContext { get; }

    internal SendWithoutContextsCookie CreateSendWithoutContextsCookie() => new SendWithoutContextsCookie(this);
    public bool IsSendWithoutContexts => mySendWithoutContexts.Value; 

    public ProtocolContexts(SerializationCtx serializationCtx)
    {
      Async = true;
      SerializationContext = serializationCtx;
    }
    
    public ICollection<RdContextBase> RegisteredContexts => myHandlersMap.Keys;

    internal ISingleContextHandler<T> GetHandlerForContext<T>(RdContext<T> context)
    {
      return (ISingleContextHandler<T>) myHandlersMap[context];
    }
    
    public override void OnWireReceived(UnsafeReader reader)
    {
      var contextBase = RdContextBase.Read(SerializationContext, reader);

      contextBase.RegisterOn(this);
      
      myCounterpartHandlers.Add(myHandlersMap[contextBase]);
    }

    private void DoAddHandler<T>(RdContext<T> context, ISingleContextHandler<T> handler)
    {
      if (myHandlersMap.TryAdd(context, handler))
      {
        context.RegisterOn(SerializationContext.Serializers);
        lock (myOrderingLock) 
          myHandlerOrder.Add(handler);
      }
    }

    private void BindHandler(Lifetime lifetime, string key, ISingleContextHandler handler)
    {
      if (handler is RdBindableBase bindableHandler)
      {
        bindableHandler.RdId = RdId.Mix(key);
        Proto.Scheduler.InvokeOrQueue(lifetime, () =>
        {
          using(CreateSendWithoutContextsCookie())
            bindableHandler.Bind(lifetime, this, key);
        });
      }
    }

    private void SendContextToRemote(RdContextBase context)
    {
      using(CreateSendWithoutContextsCookie())
        Wire.Send(RdId, writer =>
        {
          RdContextBase.Write(SerializationContext, writer, context);
        });
    }
    
    private void EnsureHeavyHandlerExists<T>(RdContext<T> context)
    {
      Assertion.Assert(context.IsHeavy, "key.IsHeavy");
      if (!myHandlersMap.ContainsKey(context)) 
        DoAddHandler(context, new HeavySingleContextHandler<T>(context, this));
    }
    
    private void EnsureLightHandlerExists<T>(RdContext<T> context)
    {
      Assertion.Assert(!context.IsHeavy, "!key.IsHeavy");
      if (!myHandlersMap.ContainsKey(context)) 
        DoAddHandler(context, new LightSingleContextHandler<T>(context));
    }

    /// <summary>
    /// Get a value set for a given key. The values are local relative to transform
    /// </summary>
    public IViewableSet<T> GetValueSet<T>(RdContext<T> context) where T : notnull
    {
      Assertion.Assert(context.IsHeavy, "Only heavy keys have value sets, key {0} is light", context.Key);
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
    

    public override IScheduler WireScheduler => InternRootScheduler.Instance;

    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      lock (myOrderingLock)
      {
        myHandlerOrder.View(lifetime, (handlerLt, _, handler) =>
        {
          BindAndSendHandler(handlerLt, handler);
        });
      }
      
      Wire.Advise(lifetime, this);
    }

    /// <summary>
    /// Reads context values from a message, sets current context to them, and returns a cookie to restore previous context
    /// </summary>
    public MessageContextCookie ReadContextsIntoCookie(UnsafeReader reader)
    {
      var numContextValues = reader.ReadShort();
      Assertion.Assert(numContextValues <= myCounterpartHandlers.Count, "We know of {0} other side keys, received {1} instead", myCounterpartHandlers.Count, numContextValues);
      var contextValueRestorers = new IDisposable[numContextValues];
      for (var i = 0; i < numContextValues; i++)
        contextValueRestorers[i] = myCounterpartHandlers[i].ReadValueIntoContext(SerializationContext, reader);
      return new MessageContextCookie(contextValueRestorers);
    }

    public readonly struct MessageContextCookie : IDisposable
    {
      private readonly IDisposable[] myEachContextValueRestorers;

      public MessageContextCookie(IDisposable[] eachContextValueRestorers)
      {
        myEachContextValueRestorers = eachContextValueRestorers;
      }

      public void Dispose()
      {
        foreach (var contextValueRestorer in myEachContextValueRestorers)
        {
          contextValueRestorer.Dispose();
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
        myHandlerOrder[i].WriteValue(SerializationContext, writer);
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

    private void BindAndSendHandler(Lifetime lifetime, ISingleContextHandler handler)
    {
      BindHandler(lifetime, handler.ContextBase.Key, handler);
      SendContextToRemote(handler.ContextBase);
    }
  }
}