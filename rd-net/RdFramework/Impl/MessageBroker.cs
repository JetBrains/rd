using System;
using System.Collections.Generic;
using System.Diagnostics;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
  public class MessageBroker
  {
    private readonly ILog myLogger = Log.GetLog("protocol.Mq");

    public bool BackwardsCompatibleWireFormat = false;

    private readonly object myLock = new();
    private readonly Dictionary<RdId, ValueLifetimed<IRdWireable>> mySubscriptions = new();
    private Queue<byte[]>? myUnprocessedMessages;
    
    public MessageBroker(bool withholdMessageDeliveryInitially = false)
    {
      myUnprocessedMessages = withholdMessageDeliveryInitially ? new() : null;
    }

    public void StartDeliveringMessages()
    {
      while (true)
      {
        Queue<byte[]>? queue;
        lock (myLock)
        {
          queue = myUnprocessedMessages;
          Assertion.Require(queue != null, "Already started delivering messages");

          if (queue.Count == 0)
          {
            myUnprocessedMessages = null;
            return;
          }

          myUnprocessedMessages = new Queue<byte[]>();
        }

        foreach (var message in queue)
          DispatchImpl(message);
      }
    }

    //on poller thread
    public void Dispatch(byte[] msg)
    {
      if (myUnprocessedMessages != null)
      {
        lock (myLock)
        {
          if (myUnprocessedMessages is { } queue)
          {
            queue.Enqueue(msg);
            return;
          }
        }
      }

      DispatchImpl(msg);
    }

    private unsafe void DispatchImpl(byte[] msg)
    {
      fixed (byte* p = msg)
      {
        var reader = UnsafeReader.CreateReader(p, msg.Length);
        var id = RdId.Read(reader);

        if (!TryGetById(id, out var lifetimed))
        {
          myLogger.Trace($"Handler is not found for {id}");
          return;
        }

        var reactive = lifetimed.Value.NotNull();
        var proto = reactive.TryGetProto();
        if (proto == null)
        {
          myLogger.Trace($"proto is null for {id}");
          return;
        }

        using (AllowBindCookie.Create())
        {
          using var _ = UsingDebugInfoCookie(reactive);
          var messageContext = BackwardsCompatibleWireFormat ? default : proto.Contexts.ReadContextsIntoCookie(reader);
          var dispatcher = new RdWireableDispatchHelper(lifetimed.Lifetime, myLogger, reactive, id, proto, messageContext);
          reactive.OnWireReceived(reader, dispatcher);
          
        }

        reader.Reset(null, 0);
      }
    }

    private class RdWireableDispatchHelper : IRdWireableDispatchHelper
    {
      private readonly ILog myLog;
      private readonly IRdWireable myWireable;
      private readonly IProtocol myProtocol;
      private readonly ProtocolContexts.MessageContext myMessageContext;
      
      public Lifetime Lifetime { get; }
      public RdId RdId { get; }

      internal RdWireableDispatchHelper(Lifetime lifetime, ILog log, IRdWireable wireable, RdId rdId, IProtocol protocol, ProtocolContexts.MessageContext messageContext)
      {
        Lifetime = lifetime;
        myLog = log;
        myWireable = wireable;
        RdId = rdId;
        myProtocol = protocol;
        myMessageContext = messageContext;
      }

      public void Dispatch(IScheduler? scheduler, Action action)
      {
        DoDispatch(Lifetime, scheduler ?? myProtocol.Scheduler, action);
      }
      
      private void DoDispatch(Lifetime lifetime, IScheduler scheduler, Action action)
      {
        if (lifetime.IsNotAlive)
        {
          myLog.Trace($"Lifetime: {lifetime} is not alive for {RdId}");
          return;
        }
        
        myLog.Trace($"Schedule continuation for {RdId}");
        
        
        scheduler.Queue(() =>
        {
          if (lifetime.IsNotAlive)
          {
            myLog.Trace($"Lifetime: {lifetime} is not alive for {RdId}");
            return;
          }
          
          using (UsingDebugInfoCookie(myWireable))
          using (myMessageContext.UpdateCookie())
          {
            action();
          }
        });
      }
    }

    private static FirstChanceExceptionInterceptor.ThreadLocalDebugInfo UsingDebugInfoCookie(IRdWireable reactive)
    {
      return new FirstChanceExceptionInterceptor.ThreadLocalDebugInfo(reactive);
    }

    public void Advise(Lifetime lifetime, IRdWireable reactive)
    {
      var rdId = reactive.RdId;
      if (rdId.IsNil)
      {
        if (lifetime.IsNotAlive)
          return;
        
        Assertion.Fail($"!id.IsNil: {reactive}");
      }
      
      mySubscriptions.BlockingAddUnique(lifetime, myLock, rdId, new(lifetime, reactive));
    }

    public bool TryGetById(RdId rdId, out ValueLifetimed<IRdWireable> subscription)
    {
      lock (myLock)
        return mySubscriptions.TryGetValue(rdId, out subscription) && subscription.Lifetime.IsAlive;
    }
  }
}