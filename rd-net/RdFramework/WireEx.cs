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
      return serverSocketWire.Port;
    }

    public static void Send(this IWire wire, RdId id, Action<UnsafeWriter> writer)
    {
      wire.Send(id, writer, (action, w) => action(w));
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