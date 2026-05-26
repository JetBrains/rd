using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Util;
using JetBrains.Serialization;
using JetBrains.Util;

namespace JetBrains.Rd.Base
{
  public interface IRdDynamic
  {
    RName Location { get; }
    
    IProtocol? TryGetProto();
    bool TryGetSerializationContext(out SerializationCtx ctx);
  }

  public static class RdDynamicEx
  {
    public static IProtocol GetProtoOrThrow(this IRdDynamic dynamic)
    {
      return dynamic.TryGetProto() ?? throw new ProtocolNotBoundException(dynamic.ToString());
    }
  }


  public interface IPrintable
  {
    void Print(PrettyPrinter printer);
  }


  public interface IRdWireable : IRdDynamic
  {
    RdId RdId { get; }
    void OnWireReceived(UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper);    
  }

  public interface IRdWireableDispatchHelper
  {
    RdId RdId { get; }
    Lifetime Lifetime { get; }
    
    public void Dispatch(IScheduler? scheduler, Action action);
  }

  public static class RdWireableDispatchHelperEx
  {
    public static void Dispatch(this IRdWireableDispatchHelper helper, IScheduler? scheduler, Action action)
    {
      helper.Dispatch(scheduler, action);
    }
    
    public static void Dispatch(this IRdWireableDispatchHelper helper, Action action)
    {
      helper.Dispatch(null, action);
    }
  }

  public interface IRdBindable : IRdDynamic, IPrintable
  {
    RdId RdId { get; set; }
    void PreBind(Lifetime lf, IRdDynamic parent, string name);    
    void Bind();    
    void Identify(IIdentities identities, RdId id);
  }

  internal readonly ref struct AllowBindCookie
  {
    private readonly bool myCreated;

    [ThreadStatic]
    public static int IsBindAllowedCount;

    public static bool IsBindAllowed => IsBindAllowedCount > 0;
    public static bool IsBindNotAllowed => !IsBindAllowed;

    private AllowBindCookie(bool created)
    {
      myCreated = created;
    }

    public static AllowBindCookie Create()
    {
      IsBindAllowedCount++;
      return new AllowBindCookie(true);
    }

    public void Dispose()
    {
      if (myCreated)
        IsBindAllowedCount--;
    }
  }


  public static class RdBindableEx
  {
    #region Bind


    internal static void PreBindPolymorphic(this object? value, Lifetime lifetime, IRdDynamic parent, string name)
    {
      if (value is IRdBindable rdBindable) 
        rdBindable.PreBind(lifetime, parent, name);
      else
        //Don't remove 'else'. RdList is bindable and collection simultaneously.
        (value as IEnumerable)?.PreBind0(lifetime, parent, name);
      
    }

    internal static void BindPolymorphic(this object? value)
    {
      if (value is IRdBindable rdBindable) 
        rdBindable.Bind();
      else
        //Don't remove 'else'. RdList is bindable and collection simultaneously.
        (value as IEnumerable)?.Bind0();
      
    }
    
    internal static class FastIsBindable<T>
    {
      // ReSharper disable once StaticMemberInGenericType
      public static readonly bool Value = Calculate();

      private static bool Calculate()
      {
        var type = typeof(T);
        if (type.IsValueType)
          return false;

        var rdBindableType = typeof(IRdBindable);
        if (rdBindableType.IsAssignableFrom(type))
          return true;

        if (type.IsArray && type.GetElementType() is { } elementType && rdBindableType.IsAssignableFrom(elementType))
          return true;

        if (type.IsGenericType && type.GetGenericTypeDefinition() == typeof(List<>))
        {
          var argument = type.GetGenericArguments().Single();
          if (rdBindableType.IsAssignableFrom(argument))
            return true;
        }

        return false;
      }
    }

    internal static bool IsBindable<T>(this T obj)
    {
      if (obj == null)
        return false;
      
      if (FastIsBindable<T>.Value)
        return true;
      
      switch (obj)
      {
        case IRdBindable _:
          return true;
        case IEnumerable enumerable when !(typeof(T).IsValueType && obj.Equals((T)default!)):
        {
          foreach (var item in enumerable)
            return item is IRdBindable;
          break;
        }
      }

      return false;
    }


    private static void PreBind0(this IEnumerable? items, Lifetime lifetime, IRdDynamic parent, string name)
    {
      if (items == null) return;

      var cnt = 0;
      foreach (var item in items)
      {
        if (item is not IRdBindable bindable)
          return;
        bindable.PreBindEx(lifetime, parent, name + "[" + cnt++ + "]");
      }
    }

    private static void Bind0(this IEnumerable? items)
    {
      if (items == null) return;

      foreach (var item in items)
      {
        if (item is not IRdBindable bindable)
          return;
        bindable.BindEx();
      }
    }


    public static void PreBindEx<T>(this T? value, Lifetime lifetime, IRdDynamic parent, string name) where T : IRdBindable
    {
      if (value != null) value.PreBind(lifetime, parent, name);
    }

    public static void BindEx<T>(this T? value) where T : IRdBindable
    {
      if (value != null) value.Bind();
    }

    public static void BindTopLevel<T>(this T? value, Lifetime lifetime, IProtocol parent, string name) where T : IRdBindable
    {
      if (value != null)
      {
        value.PreBind(lifetime, parent, name);
        value.Bind();
      }
    }

    #endregion



    #region Identify

    internal static void IdentifyPolymorphic(this object? value, IIdentities ids, RdId id)
    {  
      if (value is IRdBindable rdBindable)
        rdBindable.Identify(ids, id);
      else
        (value as IEnumerable).Identify0(ids, id);
    }
    


    private static void Identify0(this IEnumerable? items, IIdentities ids, RdId id)
    {
      if (items == null) return;

      var i = 0;
      foreach (var x in items)
      {
        (x as IRdBindable).IdentifyEx(ids, ids.Mix(id, i++));
      }
    }


    public static void IdentifyEx<T>(this T? value, IIdentities ids, RdId id) where T : IRdBindable
    {
      if (value != null) value.Identify(ids, id);
    }

    //PLEASE DON'T MERGE these two methods into one with IEnumerable<T>, just believe me
    public static void IdentifyEx<T>(this List<T>? items, IIdentities ids, RdId id) where T : IRdBindable
    {
      items.Identify0(ids, id);
    }

    public static void IdentifyEx<T>(this T[]? items, IIdentities ids, RdId id) where T : IRdBindable
    {
      items.Identify0(ids, id);
    }

    #endregion
  }

  public static class PrintableEx
  {
    public static void PrintEx(this object? me, PrettyPrinter printer)
    {
      var printable = me as IPrintable;
      if (printer.BufferExceeded)
        return;
      
      if (printable != null) printable.Print(printer);
      else switch (me)
      {
        case null:
          printer.Print("<null>");
          break;
        case string _:
          printer.Print("\"" + me + "\"");
          break;
        case IEnumerable enumerable:
        {

          if (!printer.PrintContent) break;
          
          printer.Print("[");        
          using (printer.IndentCookie())
          {
            var en = enumerable.GetEnumerator();
            var count = 0;
            var maxPrint = printer.CollectionMaxLength;
            while (en.MoveNext())
            {
              if (printer.BufferExceeded)
                return;
              if (count < maxPrint)
              {
                printer.Println();
                en.Current.PrintEx(printer);
              }
              count ++;
            }

            if (count > maxPrint)
            {
              printer.Println();
              printer.Print("... and " + (count - maxPrint) + " more");
            }

            if (count > 0) printer.Println();
            else printer.Print("<empty>");
          }        
          printer.Print("]");
          break;
        }
        default:
          printer.Print(me.ToString());
          break;
      }
    }

    public static string PrintToString(this object? me)
    {
      var prettyPrinter = new PrettyPrinter();
      me.PrintEx(prettyPrinter);
      return prettyPrinter.ToString();
    }

    public static string PrintToStringNoLimits(this object? me)
    {
      var prettyPrinter = new PrettyPrinter { CollectionMaxLength = Int32.MaxValue };
      me.PrintEx(prettyPrinter);
      return prettyPrinter.ToString();
    }
  }
}