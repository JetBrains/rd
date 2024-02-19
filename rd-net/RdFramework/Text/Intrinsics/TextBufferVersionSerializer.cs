namespace JetBrains.Rd.Text.Intrinsics
{
  public static class TextBufferVersionSerializer
  {
    public static CtxReadDelegate<TextBufferVersion> ReadDelegate = (ctx, stream) =>
    {
      var masterVersion = stream.ReadInt();
      var slaveVersion = stream.ReadInt();
      return new TextBufferVersion(masterVersion, slaveVersion);
    };

    public static CtxWriteDelegate<TextBufferVersion> WriteDelegate = (ctx, writer, value) =>
    {
      writer.WriteInt32(value.Master);
      writer.WriteInt32(value.Slave);
    };
  }
}