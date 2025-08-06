using System;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;

namespace JetBrains.Rd
{
  public interface IWire
  {
    /// <summary>
    /// Used to indicate that the wire implementation is not supposed to be used with the remote counterpart.
    /// These special wires can be used for local protocols instances to support working with the same models both from
    /// reactive-distributed way and regular in-process synchronous reactive models.
    /// </summary>
    bool IsStub { get; }

    void Send<TParam>(RdId id, TParam param, [InstantHandle] Action<TParam, UnsafeWriter> writer);
    void Advise(Lifetime lifetime, IRdWireable entity);
    
    ProtocolContexts Contexts { get; set; }

    IRdWireable? TryGetById(RdId rdId);
  }

  public interface IWireWithDelayedDelivery : IWire
  {
    void StartDeliveringMessages();
  }

  public abstract class WireBase : IWireWithDelayedDelivery
  {
    protected readonly MessageBroker MessageBroker;
    private ProtocolContexts myContexts;
    
    private bool myBackwardsCompatibleWireFormat = false;

    public bool IsStub => false;
    
    // The same value as com.jetbrains.rd.framework.SocketWire#default_max_msg_len on the Kotlin Side
    public static long DefaultMaxMsgLen = 300_000_000;
    
    private long myMaxMsgLen = DefaultMaxMsgLen;

    public long MaxMsgLen
    {
      get => myMaxMsgLen;
      set => myMaxMsgLen = value;
    }

    public ProtocolContexts Contexts
    {
      get => myContexts;
      set
      {
        Assertion.Assert(myContexts == null || ReferenceEquals(myContexts, value), "May not replace contexts in IWire");
        myContexts = value;
      }
    }

    public void StartDeliveringMessages() => MessageBroker.StartDeliveringMessages();

    public bool BackwardsCompatibleWireFormat
    {
      get => myBackwardsCompatibleWireFormat;
      set
      {
        myBackwardsCompatibleWireFormat = value;
        MessageBroker.BackwardsCompatibleWireFormat = value;
      }
    }


    [Obsolete]
    protected WireBase(IScheduler scheduler) : this()
    {
    }
    
    protected WireBase()
    {
      // contexts is initialized when protocol is created.
      myContexts = null!;
      MessageBroker = new MessageBroker(true);
    }

    
    protected void Receive(byte[] msg)
    {
      Log.Root.Catch(() =>
      {
        MessageBroker.Dispatch(msg);
      });
    }

    /// <summary>
    /// Actual send package: len(4 bytes) + id (8 bytes) + payload
    /// </summary>
    /// <param name="pkg">Package to transmit</param>
    protected abstract void SendPkg(UnsafeWriter.Cookie pkg);

    public void Send<TParam>(RdId id, TParam param, Action<TParam, UnsafeWriter> writer)
    {
      Assertion.Require(!id.IsNil);
      Assertion.AssertNotNull(writer);

      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        var bookmark = new UnsafeWriter.Bookmark(cookie.Writer);
        cookie.Writer.WriteInt32(0); //placeholder for length

        id.Write(cookie.Writer);
        if (!myBackwardsCompatibleWireFormat)
          this.WriteContext(cookie.Writer);
        writer(param, cookie.Writer);
        bookmark.WriteIntLength();

        if (cookie.Count > MaxMsgLen)
        {
          var subscription = TryGetById(id);
          Log.Root.Error($"Too long message: {cookie.Count} bytes, Subscription: {subscription?.ToString() ?? "<NULL>"}");
        }
        
        SendPkg(cookie);
      }
    }

    public void Advise(Lifetime lifetime, IRdWireable reactive)
    {
      MessageBroker.Advise(lifetime, reactive);
    }

    public IRdWireable? TryGetById(RdId rdId)
    {
      return MessageBroker.TryGetById(rdId, out var lifetimed) && lifetimed.Lifetime.IsAlive ? lifetimed.Value : null;
    }
  }
}