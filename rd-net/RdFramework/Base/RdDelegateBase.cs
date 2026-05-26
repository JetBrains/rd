using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Base
{
  public class RdDelegateBase<T> : IRdBindable where T : class, IRdBindable
  {
    
    public T Delegate { get; }

    public RdDelegateBase(T @delegate)
    {
      Delegate = @delegate;
    }


    #region Delegation
    public IProtocol? TryGetProto() => Delegate.TryGetProto();

    public bool TryGetSerializationContext(out SerializationCtx ctx) => Delegate.TryGetSerializationContext(out ctx);

    public RName Location => Delegate.Location;

    public void Print(PrettyPrinter printer)
    {
      printer.Print($"{GetType().Name} delegated by ");
      Delegate.Print(printer);
    }

    public virtual void PreBind(Lifetime lf, IRdDynamic parent, string name) => Delegate.PreBind(lf, parent, name);
    public virtual void Bind() => Delegate.Bind();

    public void Identify(IIdentities identities, RdId id) => Delegate.Identify(identities, id);

    public RdId RdId
    {
      get => Delegate.RdId;
      set => Delegate.RdId = value;
    }
    #endregion
  }
  
  
}