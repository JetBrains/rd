using System;
using System.Collections;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Util;
using JetBrains.Serialization;
using JetBrains.Util.Util;

namespace JetBrains.Rd.Base
{
  public interface IRdDynamic
  {
    [NotNull] IProtocol Proto { get; }
    SerializationCtx SerializationContext { get; }
    [NotNull] RName Location { get; }
  }


  public interface IPrintable
  {
    void Print(PrettyPrinter printer);
  }


  public interface IRdWireable
  {
    RdId RdId { get; }

    IScheduler WireScheduler { get; }

    void OnWireReceived(UnsafeReader reader);    
  }

  public interface IRdBindable : IRdDynamic, IPrintable
  {
    RdId RdId { get; set; }
    void Bind(Lifetime lf, IRdDynamic parent, string name);    
    void Identify(IIdentities identities, RdId id);
  }


  public static class RdBindableEx
  {


    #region Bind


    internal static void BindPolymorphic(this object value, Lifetime lifetime, IRdDynamic parent, string name)
    {
      if (value is IRdBindable rdBindable) 
        rdBindable.Bind(lifetime, parent, name);
      else
        //Don't remove 'else'. RdList is bindable and collection simultaneously.
        (value as IEnumerable)?.Bind0(lifetime, parent, name);
      
    }

    internal static bool IsBindable<T>(this T obj)
    {
      switch (obj)
      {
        case IRdBindable _:
          return true;
        case IEnumerable enumerable:
        {
          foreach (var item in enumerable)
            return item is IRdBindable;
          break;
        }
      }

      return false;
    }


    private static void Bind0([CanBeNull] this IEnumerable items, Lifetime lifetime, IRdDynamic parent, string name)
    {
      if (items == null) return;

      var cnt = 0;
      foreach (var item in items)
        (item as IRdBindable).BindEx(lifetime, parent, name + "[" + cnt++ + "]");
    }


    public static void BindEx<T>([CanBeNull] this T value, Lifetime lifetime, IRdDynamic parent, string name) where T : IRdBindable
    {
      if (value != null) value.Bind(lifetime, parent, name);
    }

    // ASSHEATING C# OVERLOAD RESOLUTION
    public static void BindEx<T>([CanBeNull] this List<T> items, Lifetime lifetime, IRdDynamic parent, string name) where T : IRdBindable
    {
      Bind0(items, lifetime, parent, name);
    }

    public static void BindEx<T>([CanBeNull] this T[] items, Lifetime lifetime, IRdDynamic parent, string name) where T : IRdBindable
    {
      Bind0(items, lifetime, parent, name);
    }

    #endregion



    #region Identify

    internal static void IdentifyPolymorphic(this object value, IIdentities ids, RdId id)
    {  
      if (value is IRdBindable rdBindable)
        rdBindable.Identify(ids, id);
      else
        (value as IEnumerable).Identify0(ids, id);
    }
    


    private static void Identify0([CanBeNull] this IEnumerable items, IIdentities ids, RdId id)
    {
      if (items == null) return;

      var i = 0;
      foreach (var x in items)
      {
        (x as IRdBindable).IdentifyEx(ids, id.Mix(i++));
      }
    }


    public static void IdentifyEx<T>([CanBeNull] this T value, IIdentities ids, RdId id) where T : IRdBindable
    {
      if (value != null) value.Identify(ids, id);
    }

    //PLEASE DON'T MERGE these two methods into one with IEnumerable<T>, just believe me
    public static void IdentifyEx<T>([CanBeNull] this List<T> items, IIdentities ids, RdId id) where T : IRdBindable
    {
      items.Identify0(ids, id);
    }

    public static void IdentifyEx<T>([CanBeNull] this T[] items, IIdentities ids, RdId id) where T : IRdBindable
    {
      items.Identify0(ids, id);
    }

    #endregion
  }



  public static class PrintableEx
  {
    public static void PrintEx(this object me, PrettyPrinter printer)
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
          printer.Print(enumerable.GetType().ToString(false, true));
          if (me is ICollection collection)
          {
            printer.Print($"(count={collection.Count})");  
          }

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

    public static string PrintToString(this object me)
    {
      var prettyPrinter = new PrettyPrinter();
      me.PrintEx(prettyPrinter);
      return prettyPrinter.ToString();
    }

    public static string PrintToStringNoLimits(this object me)
    {
      var prettyPrinter = new PrettyPrinter { CollectionMaxLength = Int32.MaxValue };
      me.PrintEx(prettyPrinter);
      return prettyPrinter.ToString();
    }
  }
}