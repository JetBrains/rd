using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using JetBrains.Threading;
using JetBrains.Util;

namespace JetBrains.Rd.Base
{
  public abstract class RdExtBase : RdReactiveBase
  {
    public enum ExtState
    {
      Ready,
      ReceivedCounterPart,
      Disconnected
    }

    
    
    private readonly ExtWire myExtWire = new ExtWire();
    private IProtocol? myExtProtocol;
    public sealed override IProtocol? TryGetProto() => myExtProtocol ?? base.TryGetProto();
    
    public readonly IReadonlyProperty<bool> Connected;
    protected RdExtBase()
    {
      Connected = myExtWire.Connected;
    }

    protected abstract Action<ISerializers> Register { get; }
    protected virtual long SerializationHash => 0L;

    protected override void PreInit(Lifetime lifetime, IProtocol parentProto)
    {
      Protocol.InitTrace?.Log($"{this} :: binding");

      var parentWire = parentProto.Wire;
      
      parentProto.Serializers.RegisterToplevelOnce(GetType(), Register);
      
      if (!TryGetSerializationContext(out var serializationContext))
        return;

      //todo ExtScheduler
      myExtWire.RealWire = parentWire;
      var result = lifetime.TryBracket(
        () => { myExtProtocol = new Protocol(parentProto.Name, parentProto.Serializers, parentProto.Identities, parentProto.Scheduler, myExtWire, lifetime, serializationContext, parentProto.Contexts, parentProto.ExtCreated, this.CreateExtSignal()); },
        () => { myExtProtocol = null; }
        );
      
      if (!result.Succeed)
        return;

      var bindableParent = Parent as RdBindableBase;
      var info = new ExtCreationInfo(Location, bindableParent?.ContainingExt?.RdId, SerializationHash, this);
      ((Protocol) parentProto).SubmitExtCreated(info);
      parentWire.Advise(lifetime, this);
            
      
      lifetime.OnTermination(() => { SendState(parentWire, ExtState.Disconnected); });
      
      //protocol must be set first to allow bindable bind to it
      base.PreInit(lifetime, parentProto);

      SendState(parentWire, ExtState.Ready);
      
      Protocol.InitTrace?.Log($"{this} :: bound");
    }

    protected override UnsynchronizedConcurrentAccessDetector.Cookie CreateAssertThreadingCookie(IScheduler? protoScheduler)
    {
      return default;
    }

    protected override void Init(Lifetime lifetime, IProtocol currentProto, SerializationCtx ctx)
    {
      base.Init(lifetime, currentProto, ctx);
    }


    public override RdWireableContinuation OnWireReceived(Lifetime lifetime, IProtocol proto, SerializationCtx ctx, UnsafeReader reader, UnsynchronizedConcurrentAccessDetector? _)
    {
      var remoteState = (ExtState)reader.ReadInt();
      ReceiveTrace?.Log($"Ext {Location} ({RdId}) : {remoteState}");

      switch (remoteState)
      {
        case ExtState.Ready:
          SendState(myExtWire.RealWire, ExtState.ReceivedCounterPart);
          myExtWire.Connected.Set(true);
          break;
          
        case ExtState.ReceivedCounterPart:
          myExtWire.Connected.Set(true); //don't set anything if already set
          break;
          
        case ExtState.Disconnected:
          myExtWire.Connected.Set(false);
          break;
          
        default:
          throw new ArgumentOutOfRangeException("Unsupported state: "+remoteState);
      }
      
      var counterpartSerializationHash = reader.ReadLong();
      if (counterpartSerializationHash != SerializationHash && base.TryGetProto() is {} parentProto)
      {
        parentProto.Scheduler.Queue(() => parentProto.OutOfSyncModels.Add(this));
        
        var message = $"{this} : SerializationHash doesn't match to counterpart: maybe you forgot to generate models?Our: `${SerializationHash}` counterpart: {counterpartSerializationHash}";
        if (parentProto is Protocol { ThrowErrorOnOutOfSyncModels: true })
        {
          Assertion.Fail(message);
        }
        else
        {
          ourLogReceived.Warn(message);
        }
      }
      
      return RdWireableContinuation.Empty;
    }

    private void SendState(IWire parentWire, ExtState state)
    {
      var parentProto = base.TryGetProto();
      if (parentProto == null)
        return;
      
      using(parentProto.Contexts.CreateSendWithoutContextsCookie())
        parentWire.Send(RdId, writer =>
        {
          SendTrace?.Log($"{this} : {state}");
          writer.Write((int)state);
          writer.Write(SerializationHash);
        });
    }

        

    protected override void PreInitBindableFields(Lifetime lifetime)
    {
      foreach (var pair in BindableChildren) 
        pair.Value?.PreBindPolymorphic(lifetime, this, pair.Key);
    }
    
    protected override void InitBindableFields(Lifetime lifetime)
    {
      foreach (var pair in BindableChildren)
      {
        if (pair.Value is RdPropertyBase reactive)
        {
          using (reactive.UsingLocalChange())
          {
            reactive.BindPolymorphic();
          } 
        }
        else
        {
          pair.Value?.BindPolymorphic();
        }
      }
    }

    protected override string ShortName => "ext";
  }

  
#nullable disable
  class ExtWire : IWire
  {


    internal readonly ViewableProperty<bool> Connected = new ViewableProperty<bool>(false);
    public IWire RealWire;
    
    private struct QueueItem
    {
      public readonly RdId Id;
      public readonly byte[] Bytes;
      public readonly KeyValuePair<RdContextBase, object>[] StoredContext;

      public QueueItem(RdId id, byte[] bytes, KeyValuePair<RdContextBase, object>[] storedContext)
      {
        Id = id;
        Bytes = bytes;
        StoredContext = storedContext;
      }
    }

    
    private readonly Queue<QueueItem> mySendQ = new Queue<QueueItem>();

    public bool IsStub => false;

    public ProtocolContexts Contexts
    {
      get => RealWire.Contexts;
      set => Assertion.Assert(RealWire.Contexts == value, "Can't change ProtocolContexts in ExtWire");
    }

    public ExtWire()
    {
      Connected.WhenTrue(Lifetime.Eternal, _ =>
      {
        var contextValueRestorers = new List<IDisposable>();

        var realWire = RealWire.NotNull();
        lock (mySendQ)
        {
          while (mySendQ.Count > 0)
          {
            var p = mySendQ.Dequeue();

            if (p.StoredContext.Length == 0)
            {
              using (Contexts.CreateSendWithoutContextsCookie())
                RealWire.Send(p.Id, writer => writer.WriteRaw(p.Bytes, 0, p.Bytes.Length));
              continue;
            }

            var storedContext = p.StoredContext;
            foreach (var (context, value) in storedContext)
            {
              contextValueRestorers.Add(context.UpdateValueBoxed(value));
            }
            
            try
            {
              RealWire.Send(p.Id, writer => writer.WriteRaw(p.Bytes, 0, p.Bytes.Length));
            }
            finally
            {
              foreach (var contextValueRestorer in contextValueRestorers)
                contextValueRestorer.Dispose();

              contextValueRestorers.Clear();
            }
          }
        }               
      });
    }

    
    public void Send<TContext>(RdId id, TContext param, Action<TContext, UnsafeWriter> writer)
    {
      if (RealWire.IsStub)
        return;

      lock (mySendQ)
      {
        if (mySendQ.Count > 0 || !Connected.Value)
        {
          using (var cookie = UnsafeWriter.NewThreadLocalWriter())
          {
            writer(param, cookie.Writer);
            var storedContext = Contexts.IsSendWithoutContexts 
              ? EmptyArray<KeyValuePair<RdContextBase, object>>.Instance 
              : Contexts.RegisteredContexts.Select(it => new KeyValuePair<RdContextBase, object>(it, it.ValueBoxed)).ToArray();
            mySendQ.Enqueue(new QueueItem(id, cookie.CloneData(), storedContext));
            if (!RealWire.Contexts.IsSendWithoutContexts)
              Contexts.RegisterCurrentValuesInValueSets();
          }

          return;
        }
      }

      RealWire.Send(id, param, writer);
    }

    public void Advise(Lifetime lifetime, IRdWireable entity)
    {
      RealWire.Advise(lifetime, entity);
    }

    public IRdWireable TryGetById(RdId rdId)
    {
      return RealWire.TryGetById(rdId);
    }
  }

}