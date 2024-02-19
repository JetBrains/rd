using JetBrains.Rd.Base;
using JetBrains.Rd.Text.Intrinsics;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Text.Impl.Intrinsics
{
  public class RdTextBufferChange : IPrintable
  {
    public readonly RdTextChange Change;
    public readonly TextBufferVersion Version;
    public readonly RdChangeOrigin Origin;

    public RdTextBufferChange(TextBufferVersion version, RdChangeOrigin origin, RdTextChange change)
    {
      Change = change;
      Version = version;
      Origin = origin;
    }

    public static CtxReadDelegate<RdTextBufferChange> ReadDelegate = (ctx, stream) =>
    {
      var masterVersionRemote = stream.ReadInt();
      var slaveVersionRemote = stream.ReadInt();
      var textBufferVersion = new TextBufferVersion(masterVersionRemote, slaveVersionRemote);
      var side = (RdChangeOrigin)stream.ReadInt();
      var textChange = RdTextChangeSerializer.ReadDelegate(ctx, stream);
      return new RdTextBufferChange(textBufferVersion, side, textChange);
    };

    public static CtxWriteDelegate<RdTextBufferChange> WriteDelegate = (ctx, writer, value) =>
    {
      var version = value.Version;
      writer.WriteInt32(version.Master);
      writer.WriteInt32(version.Slave);
      var side = value.Origin;
      writer.WriteInt32((int)side);
      var change = value.Change;
      RdTextChangeSerializer.WriteDelegate(ctx, writer, change);
    };

    public void Print(PrettyPrinter printer)
    {
      printer.Println("RdTextBufferChange (");
      using (printer.IndentCookie())
      {
        printer.Println($"vesion = (master={Version.Master}, slave={Version.Slave})");
        printer.Print("change = ");
        Change.Print(printer);
      }
      printer.Print(")");
    }
  }
}