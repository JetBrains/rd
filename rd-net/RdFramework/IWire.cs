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
    void Send<TParam>(RdId id, TParam param, [NotNull, InstantHandle] Action<TParam, UnsafeWriter> writer);
    void Advise([NotNull] Lifetime lifetime, [NotNull] IRdWireable entity);
    
    [NotNull] ProtocolContexts Contexts { get; set; }
  }

  public abstract class WireBase : IWire
  {    
    protected readonly MessageBroker MessageBroker;
    protected IScheduler Scheduler { get; }
    private ProtocolContexts myContexts;
    
    private bool myBackwardsCompatibleWireFormat = false;

    public ProtocolContexts Contexts
    {
      get => myContexts;
      set
      {
        Assertion.Assert(myContexts == null || ReferenceEquals(myContexts, value), "May not replace contexts in IWire");
        myContexts = value;
      }
    }

    public bool BackwardsCompatibleWireFormat
    {
      get => myBackwardsCompatibleWireFormat;
      set
      {
        myBackwardsCompatibleWireFormat = value;
        MessageBroker.BackwardsCompatibleWireFormat = value;
      }
    }


    protected WireBase([NotNull] IScheduler scheduler)
    {
      if (scheduler == null) throw new ArgumentNullException(nameof(scheduler));

      Scheduler = scheduler;
      MessageBroker = new MessageBroker(scheduler);
    }

    
    protected unsafe void Receive(byte[] msg)
    {
      RdId id;
      fixed (byte* p = msg)
      {
        var reader = UnsafeReader.CreateReader(p, msg.Length);
        id = RdId.Read(reader);
      }
      Log.Root.Catch(() =>
      {
        MessageBroker.Dispatch(id, msg);
      });
    }

    /// <summary>
    /// Actual send package: len(4 bytes) + id (8 bytes) + payload
    /// </summary>
    /// <param name="pkg">Package to transmit</param>
    protected abstract void SendPkg(UnsafeWriter.Cookie pkg);

    public void Send<TParam>(RdId id, TParam param, Action<TParam, UnsafeWriter> writer)
    {
      Assertion.Require(!id.IsNil, "!id.IsNil");
      Assertion.AssertNotNull(writer, "writer != null");

      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        var bookmark = new UnsafeWriter.Bookmark(cookie.Writer);
        cookie.Writer.Write(0); //placeholder for length

        id.Write(cookie.Writer);
        if (!myBackwardsCompatibleWireFormat)
          this.WriteContext(cookie.Writer);
        writer(param, cookie.Writer);
        bookmark.WriteIntLength();

        SendPkg(cookie);
      }
    }

    public void Advise(Lifetime lifetime, IRdWireable reactive)
    {
      MessageBroker.Advise(lifetime, reactive);
    }
  }
}