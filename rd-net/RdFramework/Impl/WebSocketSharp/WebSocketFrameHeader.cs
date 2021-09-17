/*
 * WebSocketFrame.cs
 *
 * The MIT License
 *
 * Copyright (c) 2012-2015 sta.blockhead
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/*
 * Contributors:
 * - Chris Swiedler
 */
#nullable disable
using System.Security.Cryptography;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl.WebSocketSharp
{
  internal class WebSocketFrameHeader
  {
    public ulong? ExtPayloadLength { get; set; }
    private Fin myFin;
    private Mask myMask;
    public Opcode Opcode { get; private set; }
    private byte myPayloadLength;
    private Rsv myRsv1;
    private Rsv myRsv2;
    private Rsv myRsv3;

    internal static readonly byte[] EmptyBytes;

    static WebSocketFrameHeader()
    {
      EmptyBytes = new byte[0];
      PayloadMaxLength = int.MaxValue;
    }

    public WebSocketFrameHeader(Fin fin, Mask mask, Opcode opcode, ulong payloadLength)
    {
      myFin = fin;
      myMask = mask;
      Opcode = opcode;
      myPayloadLength = PayloadLengthByte(payloadLength);
      ExtPayloadLength = payloadLength;
      myRsv1 = Rsv.Off;
      myRsv2 = Rsv.Off;
      myRsv3 = Rsv.Off;
    }

    internal int ExtendedPayloadLengthCount => myPayloadLength < 126 ? 0 : (myPayloadLength == 126 ? 2 : 8);

    internal ulong FullPayloadLength => myPayloadLength < 126
      ? myPayloadLength
      : ExtPayloadLength.Value;

    public bool IsMasked => myMask == Mask.On;

    public byte[] MaskingKey { get; set; }

    private static byte[] CreateMaskingKey()
    {
      var key = new byte[4];
      ourRandomNumberGenerator.GetBytes(key);
      return key;
    }
    
    public static WebSocketFrameHeader ParseHeader(byte[] header)
    {
      if (header.Length != 2)
        throw new WebSocketException("The header of a frame cannot be read from the stream.");

      // FIN
      var fin = (header[0] & 0x80) == 0x80 ? Fin.Final : Fin.More;

      // RSV1
      var rsv1 = (header[0] & 0x40) == 0x40 ? Rsv.On : Rsv.Off;

      // RSV2
      var rsv2 = (header[0] & 0x20) == 0x20 ? Rsv.On : Rsv.Off;

      // RSV3
      var rsv3 = (header[0] & 0x10) == 0x10 ? Rsv.On : Rsv.Off;

      // Opcode
      var opcode = (byte)(header[0] & 0x0f);

      // MASK
      var mask = (header[1] & 0x80) == 0x80 ? Mask.On : Mask.Off;

      // Payload Length
      var payloadLen = (byte)(header[1] & 0x7f);

      var err = !opcode.IsSupported()
        ? "An unsupported opcode."
        : !opcode.IsData() && rsv1 == Rsv.On
          ? "A non data frame is compressed."
          : opcode.IsControl() && fin == Fin.More
            ? "A control frame is fragmented."
            : opcode.IsControl() && payloadLen > 125
              ? "A control frame has a long payload length."
              : null;

      if (err != null)
        throw new WebSocketException(CloseStatusCode.ProtocolError, err);

      return new WebSocketFrameHeader
      {
        myFin = fin,
        myRsv1 = rsv1,
        myRsv2 = rsv2,
        myRsv3 = rsv3,
        Opcode = (Opcode)opcode,
        myMask = mask,
        myPayloadLength = payloadLen
      };
    }

    internal void CreateAndWrite(UnsafeWriter writer)
    {
      var header = (int)myFin;
      header = (header << 1) + (int)myRsv1;
      header = (header << 1) + (int)myRsv2;
      header = (header << 1) + (int)myRsv3;
      header = (header << 4) + (int)Opcode;
      header = (header << 1) + (int)myMask;
      header = (header << 7) + (int)myPayloadLength;
      writer.Write(ReverseBytes.Of((ushort)header));

      if (myPayloadLength > 125)
      {
        if (myPayloadLength == 126)
          writer.Write(ReverseBytes.Of((ushort)ExtPayloadLength.Value));
        else
          writer.Write(ReverseBytes.Of(ExtPayloadLength.Value));
      }
    }
    
    private static byte PayloadLengthByte(ulong payloadDataLength)
    {
      byte payloadLengthByte;
      if (payloadDataLength < 126)
      {
        payloadLengthByte = (byte)payloadDataLength;
      }
      else if (payloadDataLength < 0x010000)
      {
        payloadLengthByte = 126;
      }
      else
      {
        payloadLengthByte = 127;
      }

      return payloadLengthByte;
    }

    /// <summary>
    /// Represents the allowable max length.
    /// </summary>
    /// <remarks>
    ///   <para>
    ///   A <see cref="WebSocketException"/> will occur if the payload data length is
    ///   greater than the value of this field.
    ///   </para>
    ///   <para>
    ///   If you would like to change the value, you must set it to a value between
    ///   <c>WebSocket.FragmentLength</c> and <c>Int64.MaxValue</c> inclusive.
    ///   </para>
    /// </remarks>
    public static readonly ulong PayloadMaxLength;

    private static readonly RandomNumberGenerator ourRandomNumberGenerator = RandomNumberGenerator.Create();

    private WebSocketFrameHeader()
    {
    }
  }
}