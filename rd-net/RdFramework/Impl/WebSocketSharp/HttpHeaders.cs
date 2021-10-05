/*
 * HttpHeaderInfo.cs
 *
 * The MIT License
 *
 * Copyright (c) 2013-2014 sta.blockhead
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


using System;
using System.Collections.Generic;
using System.IO;
using System.Text;
using System.Threading;
using JetBrains.Diagnostics;

#nullable disable

namespace JetBrains.Rd.Impl.WebSocketSharp
{
  internal static class HttpHeaders
  {
    private const int HeadersMaxLength = 8192;
  
    private static string[] ReadUntilCrLfCrLf(Stream stream, int maxLength)
    {
      var buff = new List<byte>();
      var cnt = 0;

      void Add(int i)
      {
        if (i == -1) 
          throw new EndOfStreamException("The header cannot be read from the data source.");

        buff.Add((byte)i);
        cnt++;
      }

      var read = false;
      while (cnt < maxLength)
      {
        if (stream.ReadByte().EqualsWith('\r', Add) &&
            stream.ReadByte().EqualsWith('\n', Add) &&
            stream.ReadByte().EqualsWith('\r', Add) &&
            stream.ReadByte().EqualsWith('\n', Add))
        {
          read = true;
          break;
        }
      }

      if (!read)
        throw new WebSocketException("The length of header part is greater than the max length.");

      return Encoding.UTF8.GetString(buff.ToArray())
        .Replace(CrLf + " ", " ")
        .Replace(CrLf + "\t", " ").Split(new[] {CrLf}, StringSplitOptions.RemoveEmptyEntries);
    }

    private static T ReadWithTimeout<T>(Stream stream, Func<string[], T> parser, int millisecondsTimeout, ILog log)
    {
      var timeout = false;
      var timer = new Timer(
        state =>
        {
          timeout = true;
          stream.Dispose();
        },
        null,
        millisecondsTimeout,
        -1);

      T result = default(T);
      Exception exception = null;
      try
      {
        var data = ReadUntilCrLfCrLf(stream, HeadersMaxLength);
        LogRequest(log, data);
        result = parser(data);
      }
      catch (Exception ex)
      {
        exception = ex;
      }
      finally
      {
        timer.Change(-1, -1);
        timer.Dispose();
      }

      var msg = timeout
        ? "A timeout has occurred while reading an HTTP request/response."
        : exception != null
          ? "An exception has occurred while reading an HTTP request/response."
          : null;

      if (msg != null)
        throw new WebSocketException(msg, exception);

      return result;
    }

    private static void LogRequest(ILog log, string[] data)
    {
      if (log.IsTraceEnabled())
      {
        log.Trace("ReadWithTimeout got: ");
        foreach (var requestLine in data)
          log.Trace(requestLine + CrLf);
      }
    }

    public static Header ReadHeader(Stream stream, int millisecondsTimeout, ILog log)
    {
      return ReadWithTimeout(stream, Parse, millisecondsTimeout, log);
    }

    public struct Header
    {
      public string Method { get; }
      public Version Version { get; }
      public string Uri { get; }
      private readonly IDictionary<string, string> myHeaders;

      public Header(string method, string uri, Version version, IDictionary<string, string> headers)
      {
        Method = method;
        Version = version;
        Uri = uri;
        myHeaders = headers;
      }

      public string this[string key] => myHeaders.TryGetValue(key, out var value) ? value : null;
    }
  
    public static string SuccessHandshakeResponse (string webSocketAcceptCode)
    {
      return $@"HTTP/1.1 101 Switching Protocols" + CrLf +
             "Upgrade: websocket" + CrLf +
             "Connection: Upgrade" + CrLf +
             $"Sec-WebSocket-Accept: {webSocketAcceptCode}" + CrLf + CrLf;
    }
  
    public static string FailedHandshakeResponse ()
    {
      return $@"HTTP/1.1 400 Bad Request" + CrLf +
             "Connection: close" + CrLf + CrLf;
    }

    public static bool IsWebSocketRequest(this Header header)
    {
      return header.Method == "GET"
             && header.Version > HttpVersion.Version10
             && header["Upgrade"].Equals("websocket", StringComparison.OrdinalIgnoreCase)
             && header["Connection"].Contains("Upgrade");
    }

  
    private static Header Parse (string[] headerParts)
    {
      var requestLine = headerParts[0].Split (new[] { ' ' }, 3);
      if (requestLine.Length != 3)
        throw new ArgumentException ("Invalid request line: " + headerParts[0]);

      var headers = new Dictionary<string, string>();

      for (int i = 1; i < headerParts.Length; i++)
      {
        var headerKeyAndValue = headerParts[i].Split(':');
        headers.Add (headerKeyAndValue[0].Trim(), headerKeyAndValue[1].Trim());
      }

      return new Header (
        requestLine[0], requestLine[1], new Version (requestLine[2].Substring (5)), headers);
    }

    private const string CrLf = "\r\n";
  }
}