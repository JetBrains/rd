using System;
using JetBrains.Annotations;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Text.Intrinsics
{
  public enum RdTextChangeKind
  {
    Insert,
    Remove,
    Replace,
    Reset,
    PromoteVersion,
    InsertLeftSide,
    InsertRightSide
  }

  public class RdTextChange : IPrintable
  {
    public readonly int FullTextLength;
    public readonly RdTextChangeKind Kind;

    public readonly string New;

    public readonly string Old;

    public readonly int StartOffset;

    public RdTextChange(RdTextChangeKind kind, int startOffset, string old, string @new, int fullTextLength) {
      Kind = kind;
      StartOffset = startOffset;
      Old = old;
      New = @new;
      FullTextLength = fullTextLength;
    }

    public void Print(PrettyPrinter printer) {
      printer.Println("RdTextChange (");
      using (printer.IndentCookie())
      {
        printer.Println($"Kind = {Kind}");
        printer.Println($"Start = {StartOffset}");
        printer.Println($"OldText = '{Escape(Old)}'");
        printer.Println($"NewText = '{Escape(New)}'");
        printer.Println($"FullTextLength = {FullTextLength}");
      }

      printer.Println(")");
    }

    public RdTextChange Reverse() {
      var newKind = ReverseKind(Kind);
      var fullTextLength = FullTextLength == -1 ? -1 : FullTextLength - this.GetDelta();
      return new RdTextChange(newKind, StartOffset, New, Old, fullTextLength);
    }

    private static RdTextChangeKind ReverseKind(RdTextChangeKind kind) {
      switch (kind)
      {
        case RdTextChangeKind.Insert: return RdTextChangeKind.Remove;
        case RdTextChangeKind.Remove: return RdTextChangeKind.Insert;
        case RdTextChangeKind.Replace: return RdTextChangeKind.Replace;
        case RdTextChangeKind.Reset: throw new InvalidOperationException("Reset change isn't invertible.");
        case RdTextChangeKind.PromoteVersion: throw new InvalidOperationException("PromoteVersion change isn't invertible.");
        case RdTextChangeKind.InsertLeftSide: return RdTextChangeKind.Remove;
        case RdTextChangeKind.InsertRightSide: return RdTextChangeKind.Remove;
        default: throw new ArgumentOutOfRangeException($"Unexpected Kind value: {kind}.");
      }
    }

    private static string Escape(string s) => s.Replace("\n", "\\n").Replace("\r", "\\r");

    public override string ToString() {
      var printer = new PrettyPrinter();
      Print(printer);
      return printer.ToString();
    }
  }
}