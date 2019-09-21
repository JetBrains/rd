using System;
using System.Collections;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Util;

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
        (value as IEnumerable).Bind0(lifetime, parent, name);
      
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
    public static void PrintEx(this object thIs, PrettyPrinter printer)
    {
      var printable = thIs as IPrintable;
      if (printer.BufferExceeded)
        return;
      
      if (printable != null) printable.Print(printer);
      else if (thIs == null) printer.Print("<null>");
      else if (thIs is string) printer.Print("\"" + thIs + "\"");
      else if (thIs is IEnumerable enumerable)
      {
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
      }
      else printer.Print(thIs.ToString());
    }

    public static string PrintToString(this Object thIs)
    {
      var prettyPrinter = new PrettyPrinter();
      thIs.PrintEx(prettyPrinter);
      return prettyPrinter.ToString();
    }

    public static string PrintToStringNoLimits(this Object thIs)
    {
      var prettyPrinter = new PrettyPrinter {CollectionMaxLength = Int32.MaxValue};
      thIs.PrintEx(prettyPrinter);
      return prettyPrinter.ToString();
    }
  }
}