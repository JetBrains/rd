#if NET8_0_OR_GREATER
using System.IO;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework;

[TestFixture]
public class SocketWireUnixEndpointTest : SocketWireTestBase<string>
{
  internal override string GetPortOrPath() => Path.GetTempFileName();

  internal override (IProtocol ServerProtocol, string portOrPath) Server(Lifetime lifetime, string path = null)
  {
    var id = "TestServer";
    var connectionsParams = new EndPointWrapper.UnixSocketConnectionParams { Path = path };
    var endPointWrapper = EndPointWrapper.CreateUnixEndPoint(connectionsParams);
    var server = new SocketWire.Server(lifetime, SynchronousScheduler.Instance, endPointWrapper, id);
    var protocol = new Protocol(id, new Serializers(), new Identities(IdKind.Server), SynchronousScheduler.Instance, server, lifetime);
    return (protocol, endPointWrapper.LocalPath);
  }
  
  internal override IProtocol Client(Lifetime lifetime, string path)
  {
    var id = "TestClient";
    var connectionsParams = new EndPointWrapper.UnixSocketConnectionParams { Path = path };
    var client = new SocketWire.Client(lifetime, SynchronousScheduler.Instance, connectionsParams, id);
    return new Protocol(id, new Serializers(), new Identities(IdKind.Server), SynchronousScheduler.Instance, client, lifetime);
  }

  internal override EndPointWrapper CreateEndpointWrapper()
  {
    return EndPointWrapper.CreateUnixEndPoint(null);
  }

  // internal IProtocol Client(Lifetime lifetime, IProtocol serverProtocol)
  // {
  //   // ReSharper disable once PossibleNullReferenceException
  //   // ReSharper disable once PossibleInvalidOperationException
  //   return Client(lifetime, (serverProtocol.Wire as SocketWire.Server).Port.Value);
  // }
  
  internal override (IProtocol ServerProtocol, IProtocol ClientProtocol) CreateServerClient(Lifetime lifetime)
  {
    var path = GetPortOrPath();
    var (serverProtocol, _) = Server(lifetime, path);
    var clientProtocol = Client(lifetime, path);
    return (serverProtocol, clientProtocol);
  }
}
#endif