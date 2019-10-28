using System.Collections.Concurrent;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
  internal class HeavySingleContextHandler<T> : RdReactiveBase, ISingleContextHandler<T>
  {
    private readonly ProtocolContexts myHandler;
    private readonly InternRoot myInternRoot = new InternRoot();
    private readonly RdSet<T> myProtocolValueSet;
    private readonly ModificationCookieViewableSet<T, ProtocolContexts.OwnMessagesCookie> myModificationCookieValueSet;

    internal IViewableSet<T> LocalValueSet => myModificationCookieValueSet;

    public HeavySingleContextHandler(RdContext<T> key, ProtocolContexts handler)
    {
      myHandler = handler;
      Context = key;
      myProtocolValueSet = new RdSet<T>(key.ReadDelegate, key.WriteDelegate, new ViewableSet<T>(new ConcurrentDictionary<T, bool>().MutableKeySet(false)));
      myModificationCookieValueSet = new ModificationCookieViewableSet<T, ProtocolContexts.OwnMessagesCookie>(myHandler.CreateOwnMessageCookie, myProtocolValueSet);
    }

    public RdContext<T> Context { get; }

    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      using (myHandler.CreateOwnMessageCookie())
      {
        myInternRoot.RdId = RdId.Mix("InternRoot");
        myProtocolValueSet.RdId = RdId.Mix("ValueSet");

        myInternRoot.Bind(lifetime, this, "InternRoot");
        myProtocolValueSet.Bind(lifetime, this, "ValueSet");
      }

      myProtocolValueSet.Advise(lifetime, HandleProtocolSetEvent);
    }

    private void HandleProtocolSetEvent(SetEvent<T> obj)
    {
      var value = obj.Value;
      using(myHandler.CreateOwnMessageCookie())
        if (obj.Kind == AddRemove.Add)
          myInternRoot.Intern(value);
        else
          myInternRoot.Remove(value);
    }


    public void WriteValue(SerializationCtx context, UnsafeWriter writer)
    {
      Assertion.Assert(!myHandler.IsWritingOwnMessages, "!myHandler.IsWritingOwnMessages");
      var value = Context.Value;
      if (value == null)
      {
        writer.Write(-1);
        writer.Write(false);
      }
      else
      {
        using (myHandler.CreateOwnMessageCookie())
        {
          if (!myProtocolValueSet.Contains(value))
          {
            Assertion.Require(Proto.Scheduler.IsActive, "Attempting to use previously unused context value {0} on a background thread for key {1}", value, Context.Key);
            Assertion.AssertNotNull(Context.Value, "Can't perform an implicit add with null local context value for key {0}", Context.Key);
            myProtocolValueSet.Add(Context.Value);
          }

          var internedId = myInternRoot.Intern(value);
          InternId.Write(writer, internedId);
          if (!internedId.IsValid)
          {
            writer.Write(true);
            Context.WriteDelegate(context, writer, value);
          }
        }
      }
    }

    public T ReadValue(SerializationCtx context, UnsafeReader reader)
    {
      var id = InternId.Read(reader);
      if (!id.IsValid)
      {
        var hasValue = reader.ReadBool();
        if (hasValue)
          return Context.ReadDelegate(context, reader);
        return default;
      }
      return myInternRoot.UnIntern<T>(id);
    }

    public override void OnWireReceived(UnsafeReader reader)
    {
      Assertion.Fail("InterningProtocolContextHandler can't receive messages");
    }

    public void ReadValueAndPush(SerializationCtx context, UnsafeReader reader)
    {
      Context.PushContext(ReadValue(context, reader));
    }

    public void PopValue()
    {
      Context.PopContext();
    }
  }
}