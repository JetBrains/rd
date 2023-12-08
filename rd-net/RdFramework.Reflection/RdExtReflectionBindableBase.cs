using System;
using System.Collections.Generic;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Reflection
{
  public class RdExtReflectionBindableBase : DefaultExtBase, IReflectionBindable
  {
    List<KeyValuePair<string, object>> IReflectionBindable.BindableChildren => BindableChildren;

    private bool myBindableChildrenFilled = false;
    protected override Action<ISerializers> Register { get; } = s => { };

    void IReflectionBindable.EnsureBindableChildren()
    {
      if (myBindableChildrenFilled) return;
      myBindableChildrenFilled = true;
      BindableChildrenUtil.FillBindableFields(this);
    }

    /// <summary>
    /// Override this method to set-up data flow in your RdExt
    /// </summary>
    public virtual void OnActivated()
    {
      ((IReflectionBindable) this).EnsureBindableChildren();
    }

    protected override void PreInitBindableFields(Lifetime lifetime)
    {
      ((IReflectionBindable) this).EnsureBindableChildren();
      base.PreInitBindableFields(lifetime);
    }

    protected override void InitBindableFields(Lifetime lifetime)
    {
      ((IReflectionBindable) this).EnsureBindableChildren();
      base.InitBindableFields(lifetime);
    }

    public override void Identify(IIdentities identities, RdId id)
    {
      ((IReflectionBindable) this).EnsureBindableChildren();
      base.Identify(identities, id);
    }

    public override string ToString()
    {
      var prettyPrinter = new PrettyPrinter();
      Print(prettyPrinter);
      return prettyPrinter.ToString();
    }

    public override void Print(PrettyPrinter p)
    {
      BindableChildrenUtil.PrettyPrint(p, this);
    }
  }
}