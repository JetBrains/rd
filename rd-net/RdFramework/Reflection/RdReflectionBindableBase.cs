using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Tasks;
using JetBrains.Rd.Util;
using JetBrains.Util;

namespace JetBrains.Rd.Reflection
{
  public class RdReflectionBindableBase : RdExtBase
  {
    private static readonly Dictionary<Type, Action<RdReflectionBindableBase, PrettyPrinter>> ourPrettyPrinters = new Dictionary<Type, Action<RdReflectionBindableBase, PrettyPrinter>>();
    private static readonly Dictionary<Type, Action<RdReflectionBindableBase>> ourFillBindableChildren = new Dictionary<Type, Action<RdReflectionBindableBase>>();
    private static readonly object ourPrettyPrintersLock = new object();

    private bool bindableChildrenFilled = false;

    protected override Action<ISerializers> Register { get; } = s => { };

    protected void EnsureBindableChildren()
    {
      if (bindableChildrenFilled)
        return;

      bindableChildrenFilled = true;

      Action<RdReflectionBindableBase> fillBindableFields;
      lock (ourFillBindableChildren)
      {
        ourFillBindableChildren.TryGetValue(GetType(), out fillBindableFields);
      }

      if (fillBindableFields == null)
      {
        var t = GetType();
        var bindableMembers = ReflectionSerializers.GetBindableMembers(t.GetTypeInfo()).ToArray();
        var getters = bindableMembers.Select(ReflectionUtil.GetGetter).ToArray();

        fillBindableFields = (obj) =>
        {
          for (int i = 0; i < bindableMembers.Length; i++)
          {
            var value = getters[i](obj);
            obj.BindableChildren.Add(new KeyValuePair<string, object>(bindableMembers[i].Name, value));
          }
        };
        lock (ourFillBindableChildren)
        {
          ourFillBindableChildren[t] = fillBindableFields;
        }
      }

      fillBindableFields(this);
    }

    /// <summary>
    /// Override this method to set-up data flow in your RdExt
    /// </summary>
    public virtual void OnActivated()
    {
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

    public void BindCall<TReq, TRes>(IRdCall<TReq, TRes> call, Func<TReq, TRes> handler)
    {
      EnsureBindableChildren();
      // todo: support for real protocol
      var inProcRpc = (call as InprocRpc<TReq, TRes>).NotNull(nameof(call) + " != null");
      inProcRpc.SetHandler(handler);
    }

    public void BindCall<TReq, TRes>(IRdCall<TReq, TRes> call, Func<Lifetime, TReq, RdTask<TRes>> handler)
    {
      EnsureBindableChildren();
      // todo: support for real protocol
      var inProcRpc = (call as InprocRpc<TReq, TRes>).NotNull(nameof(call) + " != null");
      inProcRpc.SetHandler(handler);
    }

    public override string ToString()
    {
      var prettyPrinter = new PrettyPrinter();
      Print(prettyPrinter);
      return prettyPrinter.ToString();
    }

    public override void Print(PrettyPrinter p)
    {
      Action<RdReflectionBindableBase, PrettyPrinter> prettyPrinter;

      lock (ourPrettyPrintersLock)
      {
        ourPrettyPrinters.TryGetValue(GetType(), out prettyPrinter);
      }

      if (prettyPrinter == null)
      {
        var t = GetType();
        var header = t.Name + " (";
        var bindableMembers = ReflectionSerializers.GetBindableMembers(t.GetTypeInfo());
        var getters = bindableMembers.Select(ReflectionUtil.GetGetter).ToArray();
        var intros = bindableMembers.Select(mi => $"{mi.Name} = ").ToArray();

        prettyPrinter = (o, printer) =>
        {
          printer.Print(header);
          using (printer.IndentCookie())
          {
            for (int i = 0; i < getters.Length; i++)
            {
              printer.Print(intros[i]);
              getters[i](o).PrintEx(printer);
              printer.Println();
            }
          }
          printer.Print(")");
        };
        lock (ourPrettyPrintersLock)
        {
          ourPrettyPrinters[t] = prettyPrinter;
        }
      }

      prettyPrinter(this, p);
    }
  }
}