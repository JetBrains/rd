using System;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Text.Impl.Intrinsics
{
  public class RdAssertion : IPrintable, IEquatable<RdAssertion> {
    //fields
    //public fields
    public int MasterVersion {get; private set;}
    public int SlaveVersion {get; private set;}
    public string Text {get; private set;}
    
    //private fields
    //primary constructor
    public RdAssertion(
      int masterVersion,
      int slaveVersion,
      string text
    )
    {
      if (text == null) throw new ArgumentNullException("text");
      
      MasterVersion = masterVersion;
      SlaveVersion = slaveVersion;
      Text = text;
    }
    //secondary constructor
    //statics
    
    public static CtxReadDelegate<RdAssertion> Read = (ctx, reader) => 
    {
      var masterVersion = reader.ReadInt();
      var slaveVersion = reader.ReadInt();
      var text = reader.ReadString().NotNull("text");
      return new RdAssertion(masterVersion, slaveVersion, text);
    };
    
    public static CtxWriteDelegate<RdAssertion> Write = (ctx, writer, value) => 
    {
      writer.Write(value.MasterVersion);
      writer.Write(value.SlaveVersion);
      writer.Write(value.Text);
    };
    //custom body
    //equals trait
    public override bool Equals(object? obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((RdAssertion) obj);
    }
    public bool Equals(RdAssertion? other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return MasterVersion == other.MasterVersion && SlaveVersion == other.SlaveVersion && Text == other.Text;
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + MasterVersion.GetHashCode();
        hash = hash * 31 + SlaveVersion.GetHashCode();
        hash = hash * 31 + Text.GetHashCode();
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("RdAssertion (");
      using (printer.IndentCookie()) {
        printer.Print("masterVersion = "); MasterVersion.PrintEx(printer); printer.Println();
        printer.Print("slaveVersion = "); SlaveVersion.PrintEx(printer); printer.Println();
        printer.Print("text = "); Text.PrintEx(printer); printer.Println();
      }
      printer.Print(")");
    }
    //toString
    public override string ToString()
    {
      var printer = new SingleLinePrettyPrinter();
      Print(printer);
      return printer.ToString();
    }
  }
}