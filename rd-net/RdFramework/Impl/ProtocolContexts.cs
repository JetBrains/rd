using System;
using System.Collections.Concurrent;
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
  /// A callback to transform values between protocol and local values. Must be a bijection (one-to-one map)
  /// </summary>
  public delegate T ContextValueTransformer<T>(T value, ContextValueTransformerDirection direction);

  /// <summary>
  /// Indicates transformation direction for a value transformer
  /// </summary>
  public enum ContextValueTransformerDirection
  {
    WriteToProtocol,
    ReadFromProtocol
  }
  
  /// <summary>
  /// This class handles RdContext on protocol level. It tracks existing context keys and allows access to their value sets (when present)
  /// </summary>
  public class ProtocolContexts : RdReactiveBase
  {
    private readonly CopyOnWriteList<string> myOtherSideKeys = new CopyOnWriteList<string>();
    private readonly CopyOnWriteList<ISingleContextHandler> myKeyHandlerOrder = new CopyOnWriteList<ISingleContextHandler>();
    private readonly ConcurrentDictionary<string, ISingleContextHandler> myKeyHandlers = new ConcurrentDictionary<string, ISingleContextHandler>();
    private Lifetime? myBindLifetime;
    private readonly object myOrderingLock = new object();
    private readonly ThreadLocal<bool> myIsWritingOwnMessages = new ThreadLocal<bool>(() => false);
    
    
    internal struct OwnMessagesCookie : IDisposable
    {
      private readonly ProtocolContexts myHandler;
      private readonly bool myPrevValue;

      public OwnMessagesCookie(ProtocolContexts handler)
      {
        myHandler = handler;
        myPrevValue = handler.myIsWritingOwnMessages.Value;
        handler.myIsWritingOwnMessages.Value = true;
      }

      public void Dispose()
      {
        myHandler.myIsWritingOwnMessages.Value = myPrevValue;
      }
    }

    internal OwnMessagesCookie CreateOwnMessageCookie() => new OwnMessagesCookie(this);
    public bool IsWritingOwnMessages => myIsWritingOwnMessages.Value; 

    public ProtocolContexts()
    {
      Async = true;
    }

    internal ISingleContextHandler<T> GetHandlerForContext<T>(RdContext<T> context)
    {
      return (ISingleContextHandler<T>) myKeyHandlers[context.Key];
    }
    
    public override void OnWireReceived(UnsafeReader reader)
    {
      var contextBase = RdContextBase.Read(SerializationContext, reader);
      myOtherSideKeys.Add(contextBase.Key);

      new Action<RdContext<object>>(RegisterContext).Method.GetGenericMethodDefinition()
        .MakeGenericMethod(contextBase.GetType().GetGenericArguments()[0]).Invoke(this, new[] {contextBase});
    }

    private void DoAddHandler<T>(RdContext<T> context, ISingleContextHandler<T> handler)
    {
      if (myKeyHandlers.TryAdd(context.Key, handler))
      {
        lock (myOrderingLock)
        {
          SendContextToRemote(context);
          myKeyHandlerOrder.Add(handler);
        }

        var lifetime = myBindLifetime;
        if (lifetime != null) 
          BindHandler(lifetime.Value, context.Key, handler);
      }
    }

    private void BindHandler(Lifetime lifetime, string key, ISingleContextHandler handler)
    {
      if (handler is RdBindableBase bindableHandler)
      {
        bindableHandler.RdId = RdId.Mix(key);
        Proto.Scheduler.InvokeOrQueue(lifetime, () =>
        {
          using(CreateOwnMessageCookie())
            bindableHandler.Bind(lifetime, this, key);
        });
      }
    }

    private void SendContextToRemote<T>(RdContext<T> context)
    {
      using(CreateOwnMessageCookie())
        Wire.Send(RdId, writer =>
        {
          RdContext<T>.Write(SerializationContext, writer, context);
        });
    }
    
    private void EnsureHeavyHandlerExists<T>(RdContext<T> context)
    {
      Assertion.Assert(context.IsHeavy, "key.IsHeavy");
      if (!myKeyHandlers.ContainsKey(context.Key)) 
        DoAddHandler(context, new HeavySingleContextHandler<T>(context, this));
    }
    
    private void EnsureLightHandlerExists<T>(RdContext<T> context)
    {
      Assertion.Assert(!context.IsHeavy, "!key.IsHeavy");
      if (!myKeyHandlers.ContainsKey(context.Key)) 
        DoAddHandler(context, new LightSingleContextHandler<T>(context));
    }

    /// <summary>
    /// Get a value set for a given key. The values are local relative to transform
    /// </summary>
    public IViewableSet<T> GetValueSet<T>(RdContext<T> context)
    {
      Assertion.Assert(context.IsHeavy, "Only heavy keys have value sets, key {0} is light", context.Key);
      return ((HeavySingleContextHandler<T>) GetHandlerForContext(context)).LocalValueSet;
    }

    internal IViewableSet<T> GetProtocolValueSet<T>(RdContext<T> context)
    {
      Assertion.Assert(context.IsHeavy, "Only heavy keys have value sets, key {0} is light", context.Key);
      return ((HeavySingleContextHandler<T>) GetHandlerForContext(context)).ProtocolValueSet;
    }

    /// <summary>
    /// Sets a transform for a given key. The transform must be a bijection (one-to-one map). This will regenerate the local value set based on the protocol value set
    /// </summary>
    public void SetTransformerForContext<T>(RdContext<T> context, ContextValueTransformer<T> transformer)
    {
      GetHandlerForContext(context).ValueTransformer = transformer;
    }

    /// <summary>
    /// Registers a context key to be used with this context handler. Must be invoked on protocol's scheduler
    /// </summary>
    public void RegisterContext<T>(RdContext<T> context)
    {
      if (myKeyHandlers.ContainsKey(context.Key)) return;
      if(context.IsHeavy)
        EnsureHeavyHandlerExists(context);
      else
        EnsureLightHandlerExists(context);
    }
    

    public override IScheduler WireScheduler => InternRootScheduler.Instance;

    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      Wire.Advise(lifetime, this);

      lock (myOrderingLock)
      {
        foreach (var handler in myKeyHandlerOrder)
        {
          new Action<Lifetime, ISingleContextHandler<object>>(BindAndSendHandler).Method
            .GetGenericMethodDefinition().MakeGenericMethod(handler.GetType().GetGenericArguments()[0])
            .Invoke(this, new object[] { lifetime, handler });
        }
      }
      
      myBindLifetime = lifetime;
      lifetime.OnTermination(() => myBindLifetime = null);
    }

    /// <summary>
    /// Reads context values from a message, sets current context to them, and returns a cookie to restore previous context
    /// </summary>
    public MessageContextCookie ReadContextIntoCookie(UnsafeReader reader)
    {
      var numContextValues = reader.ReadShort();
      Assertion.Assert(numContextValues <= myOtherSideKeys.Count, "We know of {0} other side keys, received {1} instead", myOtherSideKeys.Count, numContextValues);
      for (var i = 0; i < numContextValues; i++)
        myKeyHandlers[myOtherSideKeys[i]].ReadValueAndPush(SerializationContext, reader);
      return new MessageContextCookie(this, numContextValues);
    }

    public struct MessageContextCookie : IDisposable
    {
      private readonly ProtocolContexts myHandler;
      private readonly int myNumContextValues;

      public MessageContextCookie(ProtocolContexts handler, int numContextValues)
      {
        myHandler = handler;
        myNumContextValues = numContextValues;
      }

      public void Dispose()
      {
        for(var i = 0; i < myNumContextValues; i++)
          myHandler.myKeyHandlers[myHandler.myOtherSideKeys[i]].PopValue();
      }
    }

    /// <summary>
    /// Writes the current context values
    /// </summary>
    [SuppressMessage("ReSharper", "InconsistentlySynchronizedField", Justification = "sync is for atomicity of write/send pairs, not access")]
    public void WriteContext(UnsafeWriter writer)
    {
      if (IsWritingOwnMessages)
      {
        WriteContextStub(writer);
        return;
      }
      
      var count = myKeyHandlerOrder.Count;
      writer.Write((short) count);
      for (var i = 0; i < count; i++) 
        myKeyHandlerOrder[i].WriteValue(SerializationContext, writer);
    }

    /// <summary>
    /// Writes an empty context
    /// </summary>
    public static void WriteContextStub(UnsafeWriter writer)
    {
      writer.Write((short) 0);
    } 

    private void BindAndSendHandler<T>(Lifetime lifetime, ISingleContextHandler<T> handler)
    {
      BindHandler(lifetime, handler.Context.Key, handler);
      SendContextToRemote(handler.Context);
    }
  }
}