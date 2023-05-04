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

    [Obsolete]
    public MessageBroker(IScheduler scheduler) : this()
    {
    }
    
    [Obsolete]
    public MessageBroker(IScheduler scheduler, bool withholdMessageDeliveryInitially) : this(withholdMessageDeliveryInitially)
    {
    }
    
    public MessageBroker() : this(false) { }
    public MessageBroker(bool withholdMessageDeliveryInitially)
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
      RdWireableContinuation continuation;
      ProtocolContexts.MessageContextCookie messageContextCookie = default;
      IProtocol? proto;
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
        proto = reactive.TryGetProto();
        if (proto == null)
        {
          myLogger.Trace($"proto is null for {id}");
          return;
        }

        using (AllowBindCookie.Create())
        {
          if (BackwardsCompatibleWireFormat)
            continuation = reactive.OnWireReceived(lifetimed.Lifetime, reader);
          else
          {
            messageContextCookie = proto.Contexts.ReadContextsIntoCookie(reader);
            continuation = reactive.OnWireReceived(lifetimed.Lifetime, reader);
          }
        }

        if (continuation == RdWireableContinuation.NotBound)
        {
          myLogger.Trace($"entity is not bound to protocol for {id}");
          return;
        }

        reader.Reset(null, 0);
      }

      continuation.RunAsync(proto, messageContextCookie);
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