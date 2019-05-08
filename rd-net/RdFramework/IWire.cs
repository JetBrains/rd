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
    void Send<TContext>(RdId id, TContext context, [NotNull, InstantHandle] Action<TContext, UnsafeWriter> writer);
    void Advise([NotNull] Lifetime lifetime, [NotNull] IRdReactive entity);
  }

  public abstract class WireBase : IWire
  {    
    protected readonly MessageBroker MessageBroker;
    private IScheduler myScheduler;

    protected WireBase([NotNull] IScheduler scheduler)
    {
      if (scheduler == null) throw new ArgumentNullException(nameof(scheduler));

      myScheduler = scheduler;
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

    public void Send<TContext>(RdId id, TContext context, Action<TContext, UnsafeWriter> writer)
    {
      Assertion.Require(!id.IsNil, "!id.IsNil");
      Assertion.AssertNotNull(writer, "writer != null");

      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        cookie.Writer.Write(0); //placeholder for length

        id.Write(cookie.Writer);
        writer(context, cookie.Writer);
        cookie.WriteIntLengthToCookieStart();

        SendPkg(cookie);
      }
    }

    public void Advise(Lifetime lifetime, IRdReactive reactive)
    {
      MessageBroker.Advise(lifetime, reactive);
    }    
  }
}