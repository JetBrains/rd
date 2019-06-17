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

    public override SerializationCtx SerializationContext
    {
      get { return mySerializationContext; }
    }

    protected override void Init(Lifetime lifetime)
    {
      Property.Bind(lifetime, this, "interningPropertyWrapper");
      base.Init(lifetime);
    }

    public override void Identify(IIdentities identities, RdId id)
    {
      Property.Identify(identities, id);
      base.Identify(identities, id);
    }
  }
}