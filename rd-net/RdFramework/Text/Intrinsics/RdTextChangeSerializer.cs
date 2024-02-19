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
      writer.WriteInt32((int)value.Kind);
      writer.WriteInt32(value.StartOffset);
      writer.WriteString(value.Old);
      writer.WriteString(value.New);
      writer.WriteInt32(value.FullTextLength);
    };

  }
}