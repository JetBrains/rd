using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;

namespace Test.RdFramework.Interning
{
  public class InterningTestPropertyWrapper<T> : RdBindableBase
  {
    internal SerializationCtx mySerializationContext;
    public InterningTestPropertyWrapper(RdProperty<T> property, SerializationCtx serializationContext)
    {
      Property = property;
      mySerializationContext = serializationContext;
    }

    public RdProperty<T> Property { get; private set; }

    public override bool TryGetSerializationContext(out SerializationCtx ctx) { ctx = mySerializationContext; return true; }

    protected override void PreInit(Lifetime lifetime, IProtocol proto)
    {
      Property.PreBind(lifetime, this, "interningPropertyWrapper");
      base.PreInit(lifetime, proto);
    }

    protected override void Init(Lifetime lifetime, IProtocol proto, SerializationCtx ctx)
    {
      Property.Bind();
      base.Init(lifetime, proto, ctx);
    }

    public override void Identify(IIdentities identities, RdId id)
    {
      Property.Identify(identities, id);
      base.Identify(identities, id);
    }
  }
}