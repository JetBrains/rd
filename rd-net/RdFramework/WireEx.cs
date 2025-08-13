using System;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;

namespace JetBrains.Rd
{
  public static class WireEx
  {
    public static int GetServerPort(this IWire wire)
    {
      var serverSocketWire = wire as SocketWire.Server;
      if (serverSocketWire == null)
        throw new ArgumentException("You must use SocketWire.Server to get server port");
      var port = (serverSocketWire.ConnectionEndPoint as EndPointWrapper.IPEndpointWrapper)?.LocalPort;
      if (!port.HasValue)
        throw new ArgumentException("You must use SocketWire.Server with connection over TCP to get server port");
      return port.Value;
    }

    public static void Send(this IWire wire, RdId id, Action<UnsafeWriter> writer)
    {
      wire.Send(id, writer, static (action, w) => action(w));
    }

    public static void WriteContext(this IWire wire, UnsafeWriter writer)
    {
      var contextHandler = wire.Contexts;
      if(contextHandler == null)
        ProtocolContexts.WriteEmptyContexts(writer);
      else
        contextHandler.WriteContexts(writer);
    }
  }
}