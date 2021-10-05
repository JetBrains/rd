using JetBrains.Diagnostics;

namespace JetBrains.Rd.Text.Intrinsics
{
  public static class RdTextChangeSerializer
  {
    public static CtxReadDelegate<RdTextChange> ReadDelegate = (ctx, stream) =>
    {
      var kind = (RdTextChangeKind)stream.ReadInt();
      var startOffset = stream.ReadInt();
      var oldText = stream.ReadString().NotNull("oldText");
      var newText = stream.ReadString().NotNull("newText");
      var fullTextLength = stream.ReadInt();
      return new RdTextChange(kind, startOffset, oldText, newText, fullTextLength);
    };

    public static CtxWriteDelegate<RdTextChange> WriteDelegate = (ctx, writer, value) =>
    {
      writer.Write((int)value.Kind);
      writer.Write(value.StartOffset);
      writer.Write(value.Old);
      writer.Write(value.New);
      writer.Write(value.FullTextLength);
    };

  }
}