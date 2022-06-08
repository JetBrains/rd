using System;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

#nullable disable

namespace JetBrains.Rd.Impl
{
  internal class HeavySingleContextHandler<T> : RdReactiveBase, ISingleContextHandler<T>
  {
    private readonly ProtocolContexts myHandler;
    private readonly InternRoot<T> myInternRoot;
    private readonly RdSet<T> myProtocolValueSet;
    private readonly ModificationCookieViewableSet<T, ProtocolContexts.SendWithoutContextsCookie> myModificationCookieValueSet;

    internal IViewableSet<T> LocalValueSet => myModificationCookieValueSet;

    public HeavySingleContextHandler(RdContext<T> context, ProtocolContexts handler)
    {
      myHandler = handler;
      Context = context;
      myInternRoot = new InternRoot<T>(context.ReadDelegate, context.WriteDelegate);
      myProtocolValueSet = new RdSet<T>(context.ReadDelegate, context.WriteDelegate, new ViewableSet<T>(new ConcurrentSet<T>()));
      myModificationCookieValueSet = new ModificationCookieViewableSet<T, ProtocolContexts.SendWithoutContextsCookie>(myHandler.CreateSendWithoutContextsCookie, myProtocolValueSet);
    }

    public RdContextBase ContextBase => Context;

    public RdContext<T> Context { get; }

    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      Assertion.Assert(myHandler.IsSendWithoutContexts, "Must bind context handler without sending contexts to prevent reentrancy");

      myInternRoot.RdId = RdId.Mix("InternRoot");
      myProtocolValueSet.RdId = RdId.Mix("ValueSet");

      myInternRoot.Bind(lifetime, this, "InternRoot");
      myProtocolValueSet.Bind(lifetime, this, "ValueSet");

      myProtocolValueSet.Advise(lifetime, HandleProtocolSetEvent);
    }

    private void HandleProtocolSetEvent(SetEvent<T> obj)
    {
      var value = obj.Value;
      using(myHandler.CreateSendWithoutContextsCookie())
        if (obj.Kind == AddRemove.Add)
          myInternRoot.Intern(value);
        else
          myInternRoot.Remove(value);
    }


    public void WriteValue(SerializationCtx context, UnsafeWriter writer)
    {
      Assertion.Assert(!myHandler.IsSendWithoutContexts, "!myHandler.IsWritingOwnMessages");
      var value = Context.Value;
      if (value == null)
      {
        InternId.Write(writer, InternId.Invalid);
        writer.Write(false);
      }
      else
      {
        using (myHandler.CreateSendWithoutContextsCookie())
        {
          AddValueToProtocolValueSetImpl(value);

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

    private void AddValueToProtocolValueSetImpl(T value)
    {
      if (myProtocolValueSet.Contains(value)) return;
      
      Assertion.Require(Proto.Scheduler.IsActive, "Attempting to use previously unused context value {0} on a background thread for key {1}", value, Context.Key);
      myProtocolValueSet.Add(Context.Value);
    }

    public void RegisterValueInValueSet()
    {
      var value = Context.Value;
      if (value == null) return;
      using (myHandler.CreateSendWithoutContextsCookie()) 
        AddValueToProtocolValueSetImpl(value);
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
      Assertion.Fail("HeavySingleContextHandler can't receive messages");
    }

    public IDisposable ReadValueIntoContext(SerializationCtx context, UnsafeReader reader)
    {
      return Context.UpdateValue(ReadValue(context, reader));
    }
  }
}