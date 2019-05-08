//using System;
//using System.IO;
//using System.Net;
//using System.Net.Sockets;
//using System.Security.Cryptography;
//using System.Text;
//using JetBrains.Annotations;
//using JetBrains.Collections.Viewable;
//using JetBrains.Diagnostics;
//using JetBrains.Lifetimes;
//using JetBrains.Rd.Impl.WebSocketSharp;
//using JetBrains.Serialization;
//
//// ReSharper disable SuggestBaseTypeForParameter Performance
//
//namespace JetBrains.Rd.Impl
//{
//  /// <summary>
//  /// TODO: Support closing handshake 
//  /// </summary>
//  public static class WebSocketWire
//  {    
//    public abstract class Base : SocketWire.Base
//    {
//      protected Base(string id, Lifetime lifetime, [NotNull] IScheduler scheduler) 
//        : base(id, lifetime, scheduler)
//      {
//      }
//
//      private class ProtocolMessage
//      {
//        public int ReadBytes;
//        public int Length;
//        public byte[] Data;
//      }
//      
//      protected override bool ReadPkg(byte[] buffer2, byte[] buffer4, byte[] buffer8, byte[] buffer, ref int lo, ref int hi)
//      {
//        var message = new ProtocolMessage();
//        do
//        {
//          if (!ReadHeader(buffer2, buffer, ref lo, ref hi, out var frame)) 
//            return false;
//
//          if (!ReadExtendedPayloadLength(buffer2, buffer8, buffer, ref lo, ref hi, ref frame, out var payloadLengthCount))
//            return false;
//
//          if (!ReadMaskingKey(buffer, ref lo, ref hi, ref frame))
//            return false;
//
//          if (!ReadPayload(buffer4, buffer, ref lo, ref hi, frame, out var payloadLength, ref message))
//            return false;
//        
//          ReadBytesCount += 2/* flags */ + payloadLengthCount + 4/* mask */ + (int)payloadLength;
//          
//        } while (message.ReadBytes < message.Length);
//
//        Receive(message.Data);
//
//        return true;
//      }
//
//      private bool ReadHeader(byte[] buffer2, byte[] buffer, ref int lo, ref int hi, out WebSocketFrameHeader frameHeader)
//      {
//        frameHeader = null;
//        
//        if (!ReadArrayFromSocket(buffer2, buffer, ref lo, ref hi))
//          return false;
//
//        frameHeader = WebSocketFrameHeader.ParseHeader(buffer2);
//        return true;
//      }
//
//      private bool ReadPayload(byte[] buffer4,
//        byte[] buffer,
//        ref int lo,
//        ref int hi,
//        WebSocketFrameHeader frameHeader,
//        out ulong payloadLength,
//        ref ProtocolMessage protocolMessage)
//      {
//        payloadLength = frameHeader.FullPayloadLength;
//
//        if (payloadLength == 0)
//          return true;
//
//        if (payloadLength > WebSocketFrameHeader.PayloadMaxLength)
//          throw new WebSocketException(CloseStatusCode.TooBig, "A frame has a long payload length.");
//
//        int resLo;
//        int resHi;
//        int framePayloadLength;
//        switch (frameHeader.Opcode)
//        {
//          // On close we should send close message, but just drop connection for now
//          case Opcode.Close:
//            return false;
//          
//          // First frame of the message
//          case Opcode.Binary:
//            // Read protocol length field
//            if (!ReadArrayFromSocket(buffer4, buffer, ref lo, ref hi))
//              return false;
//            Mask(buffer4, frameHeader.MaskingKey);
//            protocolMessage.Length = UnsafeReader.ReadInt32FromBytes(buffer4);
//            protocolMessage.Data = new byte[protocolMessage.Length];
//            framePayloadLength = (int)payloadLength - 4;
//            resLo = 0;
//            resHi = framePayloadLength;
//            break;
//          
//          // Continuation of the message
//          case Opcode.Cont:
//            framePayloadLength = (int)payloadLength;
//            resLo = protocolMessage.ReadBytes;
//            resHi = resLo + framePayloadLength;
//            break;
//          default:
//            throw new WebSocketException("Unsupported opcode");
//        }
//         
//        if (!ReadArrayFromSocket(protocolMessage.Data, resLo, resHi, buffer, ref lo, ref hi))
//          return false;
//        
//        protocolMessage.ReadBytes += framePayloadLength;
//          
//        Mask(protocolMessage.Data, resLo, resHi, frameHeader.MaskingKey);
//        
//        return true;
//      }
//
//      private bool ReadMaskingKey(byte[] buffer, ref int lo, ref int hi, ref WebSocketFrameHeader frameHeader)
//      {
//        if (!frameHeader.IsMasked)
//        {
//          frameHeader.MaskingKey = WebSocketFrameHeader.EmptyBytes;
//          return true;
//        }
//
//        var maskingKey = new byte[4];
//        if (!ReadArrayFromSocket(maskingKey, buffer, ref lo, ref hi))
//          return false;
//        
//        frameHeader.MaskingKey = maskingKey;
//        return true;
//      }
//
//      private bool ReadExtendedPayloadLength(byte[] buffer2,
//        byte[] buffer8,
//        byte[] buffer,
//        ref int lo,
//        ref int hi,
//        ref WebSocketFrameHeader frameHeader,
//        out int payloadLengthCount)
//      {
//        payloadLengthCount = frameHeader.ExtendedPayloadLengthCount;
//        if (payloadLengthCount == 0)
//          return true;
//
//        switch (payloadLengthCount)
//        {
//          case 2:
//            if (!ReadArrayFromSocket(buffer2, buffer, ref lo, ref hi))
//              return false;
//            frameHeader.ExtPayloadLength = ReverseBytes.Of(UnsafeReader.ReadUInt16FromBytes(buffer2));
//            break;
//          case 8:
//            if (!ReadArrayFromSocket(buffer8, buffer, ref lo, ref hi))
//              return false;
//            frameHeader.ExtPayloadLength = ReverseBytes.Of(UnsafeReader.ReadUInt64FromBytes(buffer8));
//            break;
//          default:
//            throw new WebSocketException(CloseStatusCode.InvalidData, "ExtendedPayloadLengthCount is not 2 or 8");
//        }
//        return true;
//      }
//
//      private static void Mask(byte[] data, byte[] key)
//      {
//        Mask(data, 0, data.Length, key);
//      }
//
//      private static void Mask(byte[] data, int lo, int hi, byte[] key)
//      {
//        for (long i = lo; i < hi; i++)
//          data[i] = (byte)(data[i] ^ key[i % 4]);
//      }
//
//      protected override void SendPkg(UnsafeWriter.Cookie cookie)
//      {
//        using (var headerCookie = UnsafeWriter.NewThreadLocalWriter())
//        {
//          new WebSocketFrameHeader(Fin.Final, WebSocketSharp.Mask.Off, Opcode.Binary, (ulong)cookie.Count)
//            .CreateAndWrite(headerCookie.Writer);
//          SendBuffer.Put(headerCookie);
//        }
//        SendBuffer.Put(cookie);
//      }
//    }
//
//    public class Server : Base
//    {
//      public Server(Lifetime lifetime, [NotNull] IScheduler scheduler, IPEndPoint endPoint, string optId = null, bool supportsReconnect = false) 
//        : base("ServerWebSocket-"+(optId ?? "<noname>"), lifetime, scheduler)
//      {
//        StartServerSocket(lifetime, endPoint, supportsReconnect);
//      }
//
//      protected override bool AcceptHandshake(Socket socket)
//      {
//        Log.Verbose("{0} : handshaking", Id);
//        using (var stream = new NetworkStream(socket))
//        {
//          var requestHeaders = HttpHeaders.ReadHeader(stream, 90000, Log);
//          
//          Log.Verbose("{0} : handshake request", Id);
//          if (!CheckHandshakeRequest(requestHeaders, out var message))
//          {
//            Log.Error("{0}: improper handshake request. Reason: {1}", Id, message);
//            RefuseHandshake(stream);
//            return false;
//          }
//
//          var base64Key = CreateResponseKey(requestHeaders["Sec-WebSocket-Key"]);
//          var handshakeResponse = HttpHeaders.SuccessHandshakeResponse(base64Key);
//          Log.Verbose("{0} : handshake response\r\n{1}", Id, handshakeResponse);
//          return SendString(stream, handshakeResponse);
//        }
//      }
//
//      private bool SendString(Stream stream, string data)
//      {
//        var bytes = Encoding.UTF8.GetBytes(data);
//        try
//        {
//          stream.Write(bytes, 0, bytes.Length);
//        }
//        catch (Exception ex)
//        {
//          Log.Error($"Can't send string `{data}`", ex);
//          return false;
//        }
//
//        return true;
//      }
//
//      private static bool CheckHandshakeRequest (
//        HttpHeaders.Header headers, out string message
//      )
//      {
//        message = null;
//
//        if (!headers.IsWebSocketRequest()) {
//          message = "Not a handshake request.";
//          return false;
//        }
//
//        var key = headers["Sec-WebSocket-Key"];
//        if (key == null) {
//          message = "It includes no Sec-WebSocket-Key header.";
//          return false;
//        }
//
//        if (key.Length == 0) {
//          message = "It includes an invalid Sec-WebSocket-Key header.";
//          return false;
//        }
//
//        var version = headers["Sec-WebSocket-Version"];
//        if (version == null) {
//          message = "It includes no Sec-WebSocket-Version header.";
//          return false;
//        }
//
//        if (version != Version) {
//          message = "It includes an invalid Sec-WebSocket-Version header.";
//          return false;
//        }
//
//        var protocol = headers["Sec-WebSocket-Protocol"];
//        if (protocol != null && protocol.Length == 0) {
//          message = "It includes an invalid Sec-WebSocket-Protocol header.";
//          return false;
//        }
//
//        return true;
//      }
//      
//      private void RefuseHandshake (Stream stream)
//      {
//        SendString(stream, HttpHeaders.FailedHandshakeResponse());
//      }
//      
//      private const string Version = "13";
//      private const string Guid = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
//
//      private static string CreateResponseKey(string base64Key)
//      {
//        var buff = new StringBuilder(base64Key, 64);
//        buff.Append(Guid);
//        var sha1 = SHA1.Create();
//        var bytes = Encoding.UTF8.GetBytes(buff.ToString());
//        var src = sha1.ComputeHash(bytes);
//
//        return Convert.ToBase64String(src);
//      }
//    }
//  }
//}