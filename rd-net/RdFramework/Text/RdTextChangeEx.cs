using System;
using JetBrains.Rd.Text.Intrinsics;

namespace JetBrains.Rd.Text
{
  public static class RdTextChangeEx
  {
    public static int GetDelta(this RdTextChange that) => that.New.Length - that.Old.Length;

    public static void AssertDocumentLength(this RdTextChange that, int currentLen)
    {
      if (that.Kind != RdTextChangeKind.Reset && that.FullTextLength != -1)
      {
        var actual = currentLen + that.GetDelta();
        var expected = that.FullTextLength;
        if (actual != expected)
          throw new InvalidOperationException($"Expected the document size: {expected}, but actual: {actual}.");
      }
    }
  }
}