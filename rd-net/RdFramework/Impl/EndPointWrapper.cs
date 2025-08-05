using System;
using System.IO;
using System.Net;
using System.Net.Sockets;

namespace JetBrains.Rd.Impl;

public abstract class EndPointWrapper
{
  public abstract EndPoint EndPointImpl { get; }
  public AddressFamily AddressFamily { get; private set; }
  public SocketType SocketType { get; private set; }
  public ProtocolType ProtocolType { get; private set; }

  public class IPEndpointWrapper : EndPointWrapper
  {
    public override EndPoint EndPointImpl => IPEndPointImpl;
    public IPEndPoint IPEndPointImpl { get; }
    public IPAddress LocalAddress { get; }
    public int LocalPort { get; }
    
    public IPEndpointWrapper(IPEndPoint endPoint)
    {
      IPEndPointImpl = endPoint;
      AddressFamily = AddressFamily.InterNetwork;
      SocketType = SocketType.Stream;
      ProtocolType = ProtocolType.Tcp;
      LocalAddress = endPoint.Address;
      LocalPort = endPoint.Port;
    }
  }
  
  public class UnixEndpointWrapper : EndPointWrapper
  {
    public string LocalPath { get; private set; }
    
#if NET8_0_OR_GREATER
    public override EndPoint EndPointImpl => UnixEndPoint;
    public UnixDomainSocketEndPoint UnixEndPoint { get; }
#else
    public override EndPoint EndPointImpl =>
      throw new NotSupportedException("Unix Sockets are supported on NET8.0 and greater");
#endif
    
    public UnixEndpointWrapper(UnixSocketConnectionParams connectionParams)
    {
      LocalPath = connectionParams.Path;
#if NET8_0_OR_GREATER
      UnixEndPoint = new UnixDomainSocketEndPoint(connectionParams.Path);
      AddressFamily = AddressFamily.Unix;
      SocketType = SocketType.Stream;
      ProtocolType = ProtocolType.Unspecified;
#else
      throw new NotSupportedException("Unix Sockets are supported on NET8.0 and greater");
#endif
    }
  }


  public static IPEndpointWrapper CreateIpEndPoint(IPAddress? address = null, int? port = null)
  {
    var address1 = address ?? IPAddress.Loopback;
    var port1 = port ?? 0;
    return new IPEndpointWrapper(new IPEndPoint(address1, port1));
  }

  public static UnixEndpointWrapper CreateUnixEndPoint(UnixSocketConnectionParams? connectionParams)
  { 
    return new UnixEndpointWrapper(connectionParams ?? new UnixSocketConnectionParams(Path.GetTempFileName()));
  }
  
  public record struct UnixSocketConnectionParams(string Path);
}