using System;

namespace JetBrains.Rd.Impl.WebSocketSharp
{
  public static class Ext
  {
    internal static string GetMessage(this CloseStatusCode code)
    {
      switch (code)
      {
        case CloseStatusCode.ProtocolError:
          return "A WebSocket protocol error has occurred.";
        case CloseStatusCode.UnsupportedData:
          return "Unsupported data has been received.";
        case CloseStatusCode.Abnormal:
          return "An exception has occurred.";
        case CloseStatusCode.InvalidData:
          return "Invalid data has been received.";
        case CloseStatusCode.PolicyViolation:
          return "A policy violation has occurred.";
        case CloseStatusCode.TooBig:
          return "A too big message has been received.";
        case CloseStatusCode.MandatoryExtension:
          return "WebSocket client didn't receive expected extension(s).";
        case CloseStatusCode.ServerError:
          return "WebSocket server got an internal error.";
        case CloseStatusCode.TlsHandshakeFailure:
          return "An error has occurred during a TLS handshake.";
        default:
          return String.Empty;
      }
    }
    
    internal static bool IsControl(this byte opcode)
    {
      return opcode > 0x7 && opcode < 0x10;
    }

    internal static bool IsControl(this Opcode opcode)
    {
      return opcode >= Opcode.Close;
    }

    internal static bool IsData(this byte opcode)
    {
      return opcode == 0x1 || opcode == 0x2;
    }

    internal static bool IsData(this Opcode opcode)
    {
      return opcode == Opcode.Text || opcode == Opcode.Binary;
    }
    
    internal static bool IsSupported(this byte opcode)
    {
      return Enum.IsDefined(typeof(Opcode), opcode);
    }
    
    /// <summary>
    /// Determines whether the specified <see cref="int"/> equals the specified <see cref="char"/>,
    /// and invokes the specified <c>Action&lt;int&gt;</c> delegate at the same time.
    /// </summary>
    /// <returns>
    /// <c>true</c> if <paramref name="value"/> equals <paramref name="c"/>;
    /// otherwise, <c>false</c>.
    /// </returns>
    /// <param name="value">
    /// An <see cref="int"/> to compare.
    /// </param>
    /// <param name="c">
    /// A <see cref="char"/> to compare.
    /// </param>
    /// <param name="action">
    /// An <c>Action&lt;int&gt;</c> delegate that references the method(s) called
    /// at the same time as comparing. An <see cref="int"/> parameter to pass to
    /// the method(s) is <paramref name="value"/>.
    /// </param>
    internal static bool EqualsWith(this int value, char c, Action<int> action)
    {
      action(value);
      return value == c - 0;
    }
  }
}