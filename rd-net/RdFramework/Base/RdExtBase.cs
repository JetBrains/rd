using System;
using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;

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
        () => { myExtProtocol = new Protocol(parentProtocol.Name, parentProtocol.Serializers, parentProtocol.Identities, parentProtocol.Scheduler, myExtWire, lifetime, SerializationContext, parentProtocol.ClientIdSet); },
        () => { myExtProtocol = null; }
        );
        
      parentWire.Advise(lifetime, this);
            
      
      SendState(parentWire, ExtState.Ready);
      lifetime.OnTermination(() => { SendState(parentWire, ExtState.Disconnected); });
      
      
      //protocol must be set first to allow bindable bind to it
      base.Init(lifetime);
      
      Protocol.InitTrace?.Log($"{this} :: bound");
    }


    public override void OnWireReceived(UnsafeReader reader)
    {
      var remoteState = (ExtState)reader.ReadInt();
      ReceiveTrace?.Log($"{{this}} : {remoteState}");

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
    
    
    private readonly Queue<KeyValuePair<RdId, byte[]>> mySendQ = new Queue<KeyValuePair<RdId, byte[]>>();
    


    public ExtWire()
    {
      Connected.WhenTrue(Lifetime.Eternal, _ =>
      {
        lock (mySendQ)
        {
          while (mySendQ.Count > 0)
          {
            var p = mySendQ.Dequeue();
            RealWire.Send(p.Key, writer => writer.WriteRaw(p.Value));
          }
        }               
      });
    }

    
    public void Send<TContext>(RdId id, TContext context, Action<TContext, UnsafeWriter> writer)
    {
      lock (mySendQ)
      {
        if (mySendQ.Count > 0 || !Connected.Value)
        {
          using (var cookie = UnsafeWriter.NewThreadLocalWriter())
          {
            writer(context, cookie.Writer);
            mySendQ.Enqueue(new KeyValuePair<RdId, byte[]>(id, cookie.CloneData()));
          }

          return;
        }
      }

      RealWire.Send(id, context, writer);
    }

    public void Advise(Lifetime lifetime, IRdWireable entity)
    {
      RealWire.Advise(lifetime, entity);
    }
  }
  
}