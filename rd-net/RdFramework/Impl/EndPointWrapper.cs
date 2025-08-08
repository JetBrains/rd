using System;
using System.IO;
using System.Net;
using System.Net.Sockets;

namespace JetBrains.Rd.Impl;

public abstract class EndPointWrapper
{
  public AddressFamily AddressFamily { get; private set; }
  public SocketType SocketType { get; private set; }
  public ProtocolType ProtocolType { get; private set; }

  public abstract EndPoint ToEndPoint();

  private EndPointWrapper() {}
  
  public class IPEndpointWrapper : EndPointWrapper
  {
    public IPEndPoint IpEndPoint { get; }
    public IPAddress LocalAddress { get; }
    public int LocalPort { get; }
    
    public IPEndpointWrapper(IPEndPoint endPoint)
    {
      IpEndPoint = endPoint;
      AddressFamily = AddressFamily.InterNetwork;
      SocketType = SocketType.Stream;
      ProtocolType = ProtocolType.Tcp;
      LocalAddress = endPoint.Address;
      LocalPort = endPoint.Port;
    }

    public override EndPoint ToEndPoint()
    {
      return IpEndPoint;
    }
  }
  
  public class UnixEndpointWrapper : EndPointWrapper
  {
    public string LocalPath { get; private set; }

#if NET8_0_OR_GREATER
    public UnixDomainSocketEndPoint UnixEndPoint { get; }
#endif

    public UnixEndpointWrapper(UnixSocketConnectionParams connectionParams) : this(connectionParams.Path) {}
    
    public UnixEndpointWrapper(string path)
    {
#if NET8_0_OR_GREATER
      UnixEndPoint = new UnixDomainSocketEndPoint(path);
#endif
      AddressFamily = AddressFamily.Unix;
      SocketType = SocketType.Stream;
      ProtocolType = ProtocolType.Unspecified;
      LocalPath = path;
    }

    public override EndPoint ToEndPoint()
    {
#if NET8_0_OR_GREATER
      return UnixEndPoint;
#else
      throw new NotSupportedException("Unix domain sockets are not supported on this platform");
#endif
    }
  }


  public static IPEndpointWrapper CreateIpEndPoint(IPAddress? address = null, int? port = null)
  {
    var address1 = address ?? IPAddress.Loopback;
    var port1 = port ?? 0;
    return new IPEndpointWrapper(new IPEndPoint(address1, port1));
  }

  public static UnixEndpointWrapper CreateUnixEndPoint(UnixSocketConnectionParams connectionParams)
  { 
    return new UnixEndpointWrapper(connectionParams);
  }
  
  public static UnixEndpointWrapper CreateUnixEndPoint(string? path)
  { 
    return new UnixEndpointWrapper(path ?? Path.GetTempFileName());
  }

  public static EndPointWrapper FromEndPoint(EndPoint endPoint)
  {
    if (endPoint is IPEndPoint ipEndPoint)
      return new IPEndpointWrapper(ipEndPoint);
#if NET8_0_OR_GREATER
    if (endPoint is UnixDomainSocketEndPoint unixEndPoint)
      return new UnixEndpointWrapper(unixEndPoint.ToString());
#endif
    throw new NotSupportedException($"Unknown endpoint type: {endPoint.GetType()}");
  }
  
#if NET8_0_OR_GREATER
  public static bool AreUnixSocketsSupported => true;
#else
  public static bool AreUnixSocketsSupported => false;
#endif
  
  public record struct UnixSocketConnectionParams(string Path);
}