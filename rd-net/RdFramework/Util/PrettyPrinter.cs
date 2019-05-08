using System;
using System.Text;
using JetBrains.Annotations;
using JetBrains.Rd.Base;

namespace JetBrains.Rd.Util
{
  public class PrettyPrinter
  {
    public struct PrettyPrinterIndentCookie : IDisposable
    {
      private readonly PrettyPrinter myPrinter;

      internal PrettyPrinterIndentCookie(PrettyPrinter printer) : this()
      {
        myPrinter = printer;
        myPrinter.myIndent += myPrinter.myStep; 
      }

      public void Dispose()
      {
        myPrinter.myIndent -= myPrinter.myStep;
      }
    }
    
    private readonly int myLimit;
    private int myStep = 2;
    private readonly StringBuilder myBuilder = new StringBuilder();
    private bool myNeedIndent = true;
    private int myIndent = 0;

    public PrettyPrinter(int limit = -1)
    {
      myLimit = limit;
      CollectionMaxLength = 3;
    }

    public int CollectionMaxLength { get; set; }

    public bool BufferExceeded
    {
      get
      {
        if (myLimit < 0)
          return false;

        return myBuilder.Length > myLimit;
      }
    }

    public PrettyPrinterIndentCookie IndentCookie()
    {      
      return new PrettyPrinterIndentCookie(this);
    }

    public void Print(string str)
    {
      foreach (var c in str)
      {
        if (BufferExceeded)
          return;

        if (myNeedIndent)
        {
          myBuilder.Append(' ', myIndent);
          myNeedIndent = false;
        }

        myBuilder.Append(c);
        if (c == '\n') myNeedIndent = true;
      }
    }

    public void Println()
    {
      Print(Environment.NewLine);
    }

    public void Println(string str)
    {
      Print(str);
      Println();
    }

    public override string ToString()
    {
      var res = myBuilder.ToString();
      return BufferExceeded ? res + "..." : res;
    }
  }

  public static class PrintableEx
  {
    [NotNull]
    public static string PrintToString([CanBeNull] this IPrintable printable, int? collectionMaxLength = null)
    {
      if (printable == null)
      {
        return "NULL";
      }
      var prettyPrinter = new PrettyPrinter();
      if (collectionMaxLength.HasValue)
        prettyPrinter.CollectionMaxLength = collectionMaxLength.Value;
      printable.Print(prettyPrinter);
      return prettyPrinter.ToString();
    }
  }
}