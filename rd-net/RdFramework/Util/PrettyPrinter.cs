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
        myPrinter.myIndent += myPrinter.Step; 
      }

      public void Dispose()
      {
        myPrinter.myIndent -= myPrinter.Step;
      }
    }
    
    
    #region Inner state
    
    private bool myNeedIndent = true;
    private int myIndent;
    private readonly StringBuilder myBuilder = new StringBuilder();
    #endregion
    
    
    #region Settings used by PrettyPrinter itself
    [PublicAPI] public const int InfiniteCapacity = -1;
    [PublicAPI] public int BufferCapacity { get; set; } = InfiniteCapacity;
    
    [PublicAPI] public int Step { get; set; } = 2;

    [PublicAPI]
    public int CollectionMaxLength { get; set; } = 3;
    #endregion
    
    #region Settings for PrettyPriter clients
    [PublicAPI] public bool PrintContent { get; set; } = true;
    #endregion
    
    

    public bool BufferExceeded
    {
      get
      {
        if (BufferCapacity == InfiniteCapacity)
          return false;

        return myBuilder.Length > BufferCapacity;
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