using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using JetBrains.Util;

#nullable disable

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
    private IProtocol myExtProtocol;
    public override IProtocol Proto => myExtProtocol ?? base.Proto;
    
    
    public readonly IReadonlyProperty<bool> Connected;
    protected RdExtBase()
    {
      Connected = myExtWire.Connected;
    }

    protected abstract Action<ISerializers> Register { get; }
    protected virtual long SerializationHash => 0L;

    public override IScheduler WireScheduler => SynchronousScheduler.Instance;

    protected override void Init(Lifetime lifetime)
    {
      Protocol.InitTrace?.Log($"{this} :: binding");

      var parentProtocol = base.Proto;
      var parentWire = parentProtocol.Wire;
      
      parentProtocol.Serializers.RegisterToplevelOnce(GetType(), Register);
      

      //todo ExtScheduler
      myExtWire.RealWire = parentWire;
      lifetime.Bracket(
        () => { myExtProtocol = new Protocol(parentProtocol.Name, parentProtocol.Serializers, parentProtocol.Identities, parentProtocol.Scheduler, myExtWire, lifetime, SerializationContext, parentProtocol.Contexts, parentProtocol.ExtCreated, this.CreateExtSignal()); },
        () => { myExtProtocol = null; }
        );

      var bindableParent = Parent is RdBindableBase bindable ? bindable : null;
      var info = new ExtCreationInfo(Location, bindableParent?.ContainingExt?.RdId, SerializationHash);
      ((Protocol) parentProtocol).SubmitExtCreated(info);
      parentWire.Advise(lifetime, this);
            
      
      lifetime.OnTermination(() => { SendState(parentWire, ExtState.Disconnected); });
      
      //protocol must be set first to allow bindable bind to it
      base.Init(lifetime);

      SendState(parentWire, ExtState.Ready);
      
      Protocol.InitTrace?.Log($"{this} :: bound");
    }


    public override void OnWireReceived(UnsafeReader reader)
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
      if (counterpartSerializationHash != SerializationHash)
      {                                    
        base.Proto.Scheduler.Queue(() => { base.Proto.OutOfSyncModels.Add(this); } );
        
        if (base.Proto is Protocol p && p.ThrowErrorOnOutOfSyncModels)
          Assertion.Fail($"{this} : SerializationHash doesn't match to counterpart: maybe you forgot to generate models?" +
                         $"Our: `${SerializationHash}` counterpart: {counterpartSerializationHash}");
      }
    }

    private void SendState(IWire parentWire, ExtState state)
    {
      using(base.Proto.Contexts.CreateSendWithoutContextsCookie())
        parentWire.Send(RdId, writer =>
        {
          SendTrace?.Log($"{this} : {state}");
          writer.Write((int)state);
          writer.Write(SerializationHash);
        });
    }

        

    protected override void InitBindableFields(Lifetime lifetime)
    {
      foreach (var pair in BindableChildren)
      {
        if (pair.Value is RdPropertyBase reactive)
        {
          using (reactive.UsingLocalChange())
          {
            reactive.BindPolymorphic(lifetime, this, pair.Key);
          } 
        }
        else
        {
          pair.Value?.BindPolymorphic(lifetime, this, pair.Key);
        }
      }
      
    }

    protected override string ShortName => "ext";
  }

  
  
  class ExtWire : IWire
  {


    internal readonly ViewableProperty<bool> Connected = new ViewableProperty<bool>(false);
    internal IWire RealWire;
    
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