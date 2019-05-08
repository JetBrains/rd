using System;
using JetBrains.Rd.Base;
using JetBrains.Rd.Text.Impl.Intrinsics;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Text.Impl.Ot.Intrinsics
{
  public class RdAck : IPrintable, IEquatable<RdAck> {
    //fields
    //public fields
    public int Timestamp {get; private set;}
    public RdChangeOrigin Origin {get; private set;}

    //private fields
    //primary constructor
    public RdAck(
      int timestamp,
      RdChangeOrigin origin
    )
    {
      Timestamp = timestamp;
      Origin = origin;
    }
    //secondary constructor
    //statics

    public static CtxReadDelegate<RdAck> Read = (ctx, reader) =>
    {
      var timestamp = reader.ReadInt();
      var origin = (RdChangeOrigin)reader.ReadInt();
      return new RdAck(timestamp, origin);
    };

    public static CtxWriteDelegate<RdAck> Write = (ctx, writer, value) =>
    {
      writer.Write(value.Timestamp);
      writer.Write((int)value.Origin);
    };
    //custom body
    //equals trait
    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != GetType()) return false;
      return Equals((RdAck) obj);
    }
    public bool Equals(RdAck other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return Timestamp == other.Timestamp && Origin == other.Origin;
    }
    //hash code trait
    public override int GetHashCode()
    {
      unchecked {
        var hash = 0;
        hash = hash * 31 + Timestamp.GetHashCode();
        hash = hash * 31 + (int) Origin;
        return hash;
      }
    }
    //pretty print
    public void Print(PrettyPrinter printer)
    {
      printer.Println("RdAck (");
      using (printer.IndentCookie()) {
        printer.Print("timestamp = "); Timestamp.PrintEx(printer); printer.Println();
        printer.Print("origin = "); Origin.PrintEx(printer); printer.Println();
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