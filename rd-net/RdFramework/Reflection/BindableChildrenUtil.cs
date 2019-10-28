using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Util;

namespace JetBrains.Rd.Reflection
{
  internal interface IReflectionBindable
  {
    List<KeyValuePair<string, object>> BindableChildren { get; }
    void OnActivated();
  }

  internal static class BindableChildrenUtil
  {
    private static readonly Dictionary<Type, Action<IReflectionBindable, PrettyPrinter>> ourPrettyPrinters = new Dictionary<Type, Action<IReflectionBindable, PrettyPrinter>>();
    private static readonly Dictionary<Type, Action<IReflectionBindable>> ourFillBindableChildren = new Dictionary<Type, Action<IReflectionBindable>>();
    private static readonly object ourPrettyPrintersLock = new object();

    internal static void PrettyPrint(PrettyPrinter p, IReflectionBindable instance)
    {
      Action<IReflectionBindable, PrettyPrinter> prettyPrinter;
      lock (ourPrettyPrintersLock)
      {
        ourPrettyPrinters.TryGetValue(instance.GetType(), out prettyPrinter);
      }

      if (prettyPrinter == null)
      {
        var t = instance.GetType();
        var header = t.Name + " (";
        var bindableMembers = SerializerReflectionUtil.GetBindableMembers(t.GetTypeInfo());
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

      prettyPrinter(instance, p);
    }

    internal static void FillBindableFields(IReflectionBindable instance)
    {
      var type = instance.GetType();
      Action<IReflectionBindable> fillBindableFields;
      lock (ourFillBindableChildren)
      {
        ourFillBindableChildren.TryGetValue(type, out fillBindableFields);
      }

      if (fillBindableFields == null)
      {
        var t = type;
        var bindableMembers = SerializerReflectionUtil.GetBindableMembers(t.GetTypeInfo()).ToArray();
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

      fillBindableFields(instance);
    }
  }
}