using System;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework;

[TestFixture]
public class SocketWireIpEndpointTest : SocketWireTestBase<int>
{
  internal override int GetPortOrPath()
  {
    var l = new TcpListener(IPAddress.Loopback, 0);
    l.Start();
    int port = ((IPEndPoint) l.LocalEndpoint).Port;
    l.Stop();
    return port;
  }
  
  internal override (IProtocol ServerProtocol, int portOrPath) Server(Lifetime lifetime, int port = 0) => CreateServer(lifetime, port);

  internal static (IProtocol ServerProtocol, int portOrPath) CreateServer(Lifetime lifetime, int port = 0)
  {
    var id = "TestServer";
    var endPointWrapper = EndPointWrapper.CreateIpEndPoint(IPAddress.Loopback, port);
    var server = new SocketWire.Server(lifetime, SynchronousScheduler.Instance, endPointWrapper, id);
    var protocol = new Protocol(id, new Serializers(), new Identities(IdKind.Server), SynchronousScheduler.Instance, server, lifetime);
    return (protocol, server.Port!.Value);
  }
  
  internal override IProtocol Client(Lifetime lifetime, int port) => CreateClient(lifetime, port);

  internal static IProtocol CreateClient(Lifetime lifetime, int port)
  {
    var id = "TestClient";
    var client = new SocketWire.Client(lifetime, SynchronousScheduler.Instance, port, id);
    return new Protocol(id, new Serializers(), new Identities(IdKind.Server), SynchronousScheduler.Instance, client, lifetime);
  }

  internal override EndPointWrapper CreateEndpointWrapper() => EndPointWrapper.CreateIpEndPoint();

  internal IProtocol Client(Lifetime lifetime, IProtocol serverProtocol)
  {
    // ReSharper disable once PossibleNullReferenceException
    // ReSharper disable once PossibleInvalidOperationException
    return Client(lifetime, (serverProtocol.Wire as SocketWire.Server).Port.Value);
  }

  internal override (IProtocol ServerProtocol, IProtocol ClientProtocol) CreateServerClient(Lifetime lifetime)
  {
    var (serverProtocol, _) = Server(lifetime);
    var clientProtocol = Client(lifetime, serverProtocol);
    return (serverProtocol, clientProtocol);
  }
  
  [TestCase(true)]
  [TestCase(false)]
  public void TestPacketLoss(bool isClientToServer)
  {
    using (Log.UsingLogFactory(new TextWriterLogFactory(Console.Out, LoggingLevel.TRACE)))
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);

        var (serverProtocol, _) = Server(lifetime);
        var serverWire = (SocketWire.Base) serverProtocol.Wire;

        var proxy = new SocketProxy("TestProxy", lifetime, serverProtocol);
        proxy.Start();

        var clientProtocol = Client(lifetime, proxy.Port);
        var clientWire = (SocketWire.Base) clientProtocol.Wire;

        Thread.Sleep(DefaultTimeout);

        if (isClientToServer)
          proxy.StopClientToServerMessaging();
        else
          proxy.StopServerToClientMessaging();

        var detectionTimeoutTicks = ((SocketWire.Base) clientProtocol.Wire).HeartBeatInterval.Ticks *
                                    (SocketWire.Base.MaximumHeartbeatDelay + 3);
        var detectionTimeout = TimeSpan.FromTicks(detectionTimeoutTicks);
          
        Thread.Sleep(detectionTimeout);

        Assert.IsTrue(serverWire.Connected.Value);
        Assert.IsTrue(clientWire.Connected.Value);
          
        Assert.IsFalse(serverWire.HeartbeatAlive.Value);
        Assert.IsFalse(clientWire.HeartbeatAlive.Value);

        if (isClientToServer)
          proxy.StartClientToServerMessaging();
        else
          proxy.StartServerToClientMessaging();

        Thread.Sleep(detectionTimeout);

        Assert.IsTrue(serverWire.Connected.Value);
        Assert.IsTrue(clientWire.Connected.Value);
          
        Assert.IsTrue(serverWire.HeartbeatAlive.Value);
        Assert.IsTrue(clientWire.HeartbeatAlive.Value);

      });
  }
  
  // [Test]
  // [Ignore("Not enough timeout to get the correct test")]
  // public void TestStressHeartbeat()
  // {
  //   // using (Log.UsingLogFactory(new TextWriterLogFactory(Console.Out, LoggingLevel.TRACE)))
  //   Lifetime.Using(lifetime =>
  //   {
  //     SynchronousScheduler.Instance.SetActive(lifetime);
  //
  //     var interval = TimeSpan.FromMilliseconds(50);
  //
  //     var serverProtocol = Server(lifetime);
  //     var serverWire = ((SocketWire.Base) serverProtocol.Wire).With(wire => wire.HeartBeatInterval = interval);
  //
  //     var latency = TimeSpan.FromMilliseconds(40);
  //     var proxy = new SocketProxy("TestProxy", lifetime, serverProtocol) {Latency = latency};
  //     proxy.Start();
  //
  //     var clientProtocol = Client(lifetime, proxy.Port);
  //     var clientWire = ((SocketWire.Base) clientProtocol.Wire).With(wire => wire.HeartBeatInterval = interval);
  //
  //     Thread.Sleep(DefaultTimeout);
  //
  //     serverWire.HeartbeatAlive.WhenFalse(lifetime, _ => Assert.Fail("Detected false disconnect on server side"));
  //     clientWire.HeartbeatAlive.WhenFalse(lifetime, _ => Assert.Fail("Detected false disconnect on client side"));
  //
  //     Thread.Sleep(TimeSpan.FromSeconds(50));
  //   });
  // }
}