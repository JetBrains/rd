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
  public delegate T ContextValueTransformer<T>(T value, ContextValueTransformerDirection direction);

  public enum ContextValueTransformerDirection
  {
    WriteToProtocol,
    ReadFromProtocol
  }
  
  public class ProtocolContextHandler : RdReactiveBase
  {
    private readonly CopyOnWriteList<string> myOtherSideKeys = new CopyOnWriteList<string>();
    private readonly CopyOnWriteList<ISingleKeyProtocolContextHandler> myKeyHandlerOrdering = new CopyOnWriteList<ISingleKeyProtocolContextHandler>();
    private readonly ConcurrentDictionary<string, ISingleKeyProtocolContextHandler> myKeyHandlers = new ConcurrentDictionary<string, ISingleKeyProtocolContextHandler>();
    private Lifetime? myBindLifetime;
    private readonly object myOrderingLock = new object();
    private readonly ThreadLocal<bool> myIsWritingOwnMessages = new ThreadLocal<bool>(() => false);
    
    
    internal struct OwnMessagesCookie : IDisposable
    {
      private readonly ProtocolContextHandler myHandler;
      private readonly bool myPrevValue;

      public OwnMessagesCookie(ProtocolContextHandler handler)
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

    public ProtocolContextHandler()
    {
      Async = true;
    }

    internal ISingleKeyProtocolContextHandler<T> GetHandlerForKey<T>(RdContextKey<T> key)
    {
      return (ISingleKeyProtocolContextHandler<T>) myKeyHandlers[key.Key];
    }
    
    public override void OnWireReceived(UnsafeReader reader)
    {
      var keyId = reader.ReadString();
      var isHeavy = reader.ReadBoolean();
      myOtherSideKeys.Add(keyId);
      var serializerId = reader.ReadRdId();
      var actualType = Proto.Serializers.GetTypeForId(serializerId);
      if (isHeavy)
      {
        new Action<string>(EnsureHeavyKeyHandlerExists<object>).Method.GetGenericMethodDefinition().MakeGenericMethod(actualType).Invoke(this, new []{ keyId });
      }
      else
      {
        new Action<string, RdId>(CreateSimpleHandlerWithTypeId<object>).Method.GetGenericMethodDefinition()
          .MakeGenericMethod(actualType).Invoke(this, new object[] {keyId, serializerId});
      }
    }

    private void CreateSimpleHandlerWithTypeId<T>(string keyId, RdId serializerId)
    {
      if (!myKeyHandlers.ContainsKey(keyId))
      {
        var key = new RdContextKey<T>(keyId, false, Proto.Serializers.GetReaderForId<T>(serializerId), Proto.Serializers.GetWriterForId<T>(serializerId));
        DoAddHandler(key, new SimpleProtocolContextHandler<T>(key));
      }
    }

    private void DoAddHandler<T>(RdContextKey<T> key, ISingleKeyProtocolContextHandler<T> handler)
    {
      if (myKeyHandlers.TryAdd(key.Key, handler))
      {
        lock (myOrderingLock)
        {
          SendKeyToRemote(key);
          myKeyHandlerOrdering.Add(handler);
        }

        var lifetime = myBindLifetime;
        if (lifetime != null) 
          BindHandler(lifetime.Value, key.Key, handler);
      }
    }

    private void BindHandler(Lifetime lifetime, string key, ISingleKeyProtocolContextHandler handler)
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

    private void SendKeyToRemote<T>(RdContextKey<T> key)
    {
      using(CreateOwnMessageCookie())
        Wire.Send(RdId, writer =>
        {
          writer.Write(key.Key);
          writer.Write(key.IsHeavy);
          writer.Write(Proto.Serializers.GetIdForType(typeof(T)));
        });
    }

    private void EnsureHeavyKeyHandlerExists<T>(string keyId)
    {
      var key = new RdContextKey<T>(keyId, true, null, null);
      EnsureHeavyKeyHandlerExists<T>(key);
    }

    private void EnsureHeavyKeyHandlerExists<T>(RdContextKey<T> key)
    {
      Assertion.Assert(key.IsHeavy, "key.IsHeavy");
      if (!myKeyHandlers.TryGetValue(key.Key, out _)) 
        DoAddHandler(key, new InterningProtocolContextHandler<T>(key, this));
    }
    
    private void EnsureLightKeyHandlerExists<T>(RdContextKey<T> key)
    {
      Assertion.Assert(!key.IsHeavy, "!key.IsHeavy");
      if (!myKeyHandlers.TryGetValue(key.Key, out _)) 
        DoAddHandler(key, new SimpleProtocolContextHandler<T>(key));
    }

    public IViewableSet<T> GetValueSet<T>(RdContextKey<T> key)
    {
      Assertion.Assert(key.IsHeavy, "Only heavy keys have value sets, key {0} is light", key.Key);
      return ((InterningProtocolContextHandler<T>) GetHandlerForKey(key)).LocalValueSet;
    }

    internal IViewableSet<T> GetProtocolValueSet<T>(RdContextKey<T> key)
    {
      Assertion.Assert(key.IsHeavy, "Only heavy keys have value sets, key {0} is light", key.Key);
      return ((InterningProtocolContextHandler<T>) GetHandlerForKey(key)).ProtocolValueSet;
    }

    public void SetTransformerForKey<T>(RdContextKey<T> key, ContextValueTransformer<T> transformer)
    {
      GetHandlerForKey(key).ValueTransformer = transformer;
    }

    public void RegisterKey<T>(RdContextKey<T> key)
    {
      if (myKeyHandlers.ContainsKey(key.Key)) return;
      if(key.IsHeavy)
        EnsureHeavyKeyHandlerExists(key);
      else
        EnsureLightKeyHandlerExists(key);
    }
    

    public override IScheduler WireScheduler => InternRootScheduler.Instance;

    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      Wire.Advise(lifetime, this);

      lock (myOrderingLock)
      {
        foreach (var handler in myKeyHandlerOrdering)
        {
          new Action<Lifetime, ISingleKeyProtocolContextHandler<object>>(BindAndSendHandler).Method
            .GetGenericMethodDefinition().MakeGenericMethod(handler.GetType().GetGenericArguments()[0])
            .Invoke(this, new object[] { lifetime, handler });
        }
      }
      
      myBindLifetime = lifetime;
      lifetime.OnTermination(() => myBindLifetime = null);
    }

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
      private readonly ProtocolContextHandler myHandler;
      private readonly int myNumContextValues;

      public MessageContextCookie(ProtocolContextHandler handler, int numContextValues)
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

    [SuppressMessage("ReSharper", "InconsistentlySynchronizedField", Justification = "sync is for atomicity of write/send pairs, not access")]
    public void WriteContext(UnsafeWriter writer)
    {
      if (IsWritingOwnMessages)
      {
        WriteContextStub(writer);
        return;
      }
      
      var count = myKeyHandlerOrdering.Count;
      writer.Write((short) count);
      for (var i = 0; i < count; i++) 
        myKeyHandlerOrdering[i].WriteValue(SerializationContext, writer);
    }

    public static void WriteContextStub(UnsafeWriter writer)
    {
      writer.Write((short) 0);
    } 

    private void BindAndSendHandler<T>(Lifetime lifetime, ISingleKeyProtocolContextHandler<T> handler)
    {
      BindHandler(lifetime, handler.Key.Key, handler);
      SendKeyToRemote(handler.Key);
    }
  }
}