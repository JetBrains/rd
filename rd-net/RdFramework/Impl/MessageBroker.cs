using System.Collections.Generic;
using System.Linq;
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

    class Mq
    {
      internal readonly List<byte[]> DefaultSchedulerMessages = new List<byte[]>();
      internal readonly List<byte[]> CustomSchedulerMessages = new List<byte[]>();
    }

    public bool BackwardsCompatibleWireFormat = false;

    private readonly IScheduler myScheduler;
    private readonly object myLock = new object();
    private readonly Dictionary<RdId, IRdWireable> mySubscriptions = new Dictionary<RdId, IRdWireable>();
    private readonly Dictionary<RdId, Mq> myBroker = new Dictionary<RdId, Mq>();

    private bool myIsQueueingAllMessages;

    public MessageBroker(IScheduler scheduler)
    {
      myScheduler = scheduler;
      myIsQueueingAllMessages = false;
    }
    
    public MessageBroker(IScheduler scheduler, bool withholdMessageDeliveryInitially)
    {
      myScheduler = scheduler;
      myIsQueueingAllMessages = withholdMessageDeliveryInitially;
    }


    private void Invoke(IRdWireable reactive, byte[] msg, bool sync = false)
    {
      if (sync)
      {
        Execute(reactive, msg);
        return;
      } 
      
      reactive.WireScheduler.Queue(() =>
      {
        if (ShouldProcess(reactive))
          Execute(reactive, msg);
        else
          myLogger.Trace("Handler for entity {0} dissapeared", reactive);
      });
      
    }

    private bool ShouldProcess(IRdWireable reactive)
    {
      if (!reactive.IsBound) return false;

      lock (myLock)
      {
        return mySubscriptions.ContainsKey(reactive.RdId);
      }
    }
    
    private unsafe void Execute(IRdWireable reactive, byte[] msg)
    {
      fixed (byte* p = msg)
      {
        var reader = UnsafeReader.CreateReader(p, msg.Length);
        var rdid0 = RdId.Read(reader);
        Assertion.Assert(reactive.RdId.Equals(rdid0), "Not equals: {0}, {1}", reactive.RdId, rdid0);

        if (BackwardsCompatibleWireFormat)
          reactive.OnWireReceived(reader);
        else
          using (reactive.Proto.Contexts.ReadContextsIntoCookie(reader))
            reactive.OnWireReceived(reader);
      }
    }

    public void StartDeliveringMessages()
    {
      Assertion.Require(myIsQueueingAllMessages, "Already started delivering messages");

      lock (myLock)
      {
        myIsQueueingAllMessages = false;

        var entries = myBroker.ToList();
        myBroker.Clear();
        
        foreach (var keyValuePair in entries)
        {
          Assertion.Assert(keyValuePair.Value.CustomSchedulerMessages.Count == 0, "Unexpected custom scheduler messages");
          
          foreach (var messageBytes in keyValuePair.Value.DefaultSchedulerMessages)
            Dispatch(keyValuePair.Key, messageBytes);
        }
      }
    }

    //on poller thread
    public void Dispatch(RdId id, byte[] msg)
    {
      Assertion.Require(!id.IsNil, "!id.IsNil");

      lock (myLock)
      {
        var s = mySubscriptions.GetOrDefault(id);
        if (s == null || myIsQueueingAllMessages)
        {
          var currentBroker = myBroker.GetOrCreate(id, () => new Mq());
          currentBroker.DefaultSchedulerMessages.Add(msg);

          if (myIsQueueingAllMessages) return;
          
          myScheduler.Queue(() =>
          {
            byte[] msg1;

            IRdWireable subscription;
            bool hasSubscription;

            lock (myLock)
            {
              if (currentBroker.DefaultSchedulerMessages.Count > 0)
              {
                msg1 = currentBroker.DefaultSchedulerMessages[0];
                currentBroker.DefaultSchedulerMessages.RemoveAt(0);
              }
              else
                msg1 = null;

              hasSubscription = mySubscriptions.TryGetValue(id, out subscription);
            }

            if (!hasSubscription)
            {
              myLogger.Trace("No handler for id: {0}", id);
            }
            else if (msg1 != null)
            {
              Invoke(subscription, msg1, sync: subscription.WireScheduler == myScheduler);
            }

            lock (myLock)
            { 
              if (currentBroker.DefaultSchedulerMessages.Count == 0)
              {
                if (myBroker.Remove(id))
                {
                  if (subscription != null)
                  {
                    foreach (var m in currentBroker.CustomSchedulerMessages)
                    {
                      Assertion.Assert(subscription.WireScheduler != myScheduler,
                        "subscription.Scheduler != myScheduler for {0}", subscription);
                      Invoke(subscription, m);
                    }
                  }
                }
              }
            }

          });
        }


        else // s != null
        {
          if (s.WireScheduler == myScheduler || s.WireScheduler.OutOfOrderExecution)
          {
            Invoke(s, msg);
          }
          else
          {
            var mq = myBroker.GetOrDefault(id);
            if (mq != null)
            {
              mq.CustomSchedulerMessages.Add(msg);
            }
            else
            {
              Invoke(s, msg);
            }
          }

        }

      }
    }


    
    public void Advise(Lifetime lifetime, IRdWireable reactive)
    {
      Assertion.Require(!reactive.RdId.IsNil, "!id.IsNil: {0}", reactive);

      //todo commented because of WiredRdTask
//      myScheduler.AssertThread(reactive);

      // ReSharper disable once InconsistentlySynchronizedField
      mySubscriptions.BlockingAddUnique(lifetime, myLock, reactive.RdId, reactive);

      if (reactive.WireScheduler.OutOfOrderExecution)
        lifetime.TryExecute(() =>
        {
          lock(myLock) {
            if (myBroker.TryGetValue(reactive.RdId, out var mq))
            {
              myBroker.Remove(reactive.RdId);
              foreach (var msg in mq.DefaultSchedulerMessages)
              {
                Invoke(reactive, msg);
              }

              mq.DefaultSchedulerMessages.Clear(); // clear it here because it is captured by default scheduler queueing
              Assertion.Assert(mq.CustomSchedulerMessages.Count == 0, "Custom scheduler messages for an entity with outOfOrder scheduler {0}", reactive);
            }
          }
        });
    }


  }
}