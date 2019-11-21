using System;
using System.Collections.Generic;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Reflection
{
  public class RdExtReflectionBindableBase : RdExtBase, IReflectionBindable
  {
    List<KeyValuePair<string, object>> IReflectionBindable.BindableChildren => BindableChildren;

    private bool bindableChildrenFilled = false;
    protected override Action<ISerializers> Register { get; } = s => { };

    protected void EnsureBindableChildren()
    {
      if (bindableChildrenFilled) return;
      bindableChildrenFilled = true;
      BindableChildrenUtil.FillBindableFields(this);
    }

    /// <summary>
    /// Override this method to set-up data flow in your RdExt
    /// </summary>
    public virtual void OnActivated()
    {
      EnsureBindableChildren();
    }

    protected override void InitBindableFields(Lifetime lifetime)
    {
      EnsureBindableChildren();
      base.InitBindableFields(lifetime);
    }

    public override void Identify(IIdentities identities, RdId id)
    {
      EnsureBindableChildren();
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