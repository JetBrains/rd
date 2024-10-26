using System.IO;
using System.Net;
using System.Net.Sockets;

namespace JetBrains.Rd.Impl;

public class EndPointWrapper
{
  public EndPoint EndPointImpl { get; }
  public IPAddress? LocalAddress { get; private set; }
  public int? LocalPort { get; private set; }
  public string? LocalPath { get; private set; }
  public AddressFamily AddressFamily { get; private set; }
  public SocketType SocketType { get; private set; }
  public ProtocolType ProtocolType { get; private set; }

  private EndPointWrapper(EndPoint endPoint)
  {
    EndPointImpl = endPoint;
  }

  public static EndPointWrapper CreateIpEndPoint(IPAddress? address = null, int? port = null)
  {
    var address1 = address ?? IPAddress.Loopback;
    var port1 = port ?? 0;
    return new EndPointWrapper(new IPEndPoint(address1, port1))
    {
      AddressFamily = AddressFamily.InterNetwork,
      SocketType = SocketType.Stream,
      ProtocolType = ProtocolType.Tcp,
      LocalAddress = address1,
      LocalPort = port1,
      LocalPath = null,
    };
  }

  public static EndPointWrapper CreateUnixEndPoint(string? path = null)
  {
    var path1 = path ?? Path.GetTempFileName();
    return new EndPointWrapper(new UnixDomainSocketEndPoint(path1)) {
      AddressFamily = AddressFamily.Unix,
      SocketType = SocketType.Stream,
      ProtocolType = ProtocolType.Unspecified,
      LocalAddress = null,
      LocalPort = null,
      LocalPath = path1,
    };
  }
}