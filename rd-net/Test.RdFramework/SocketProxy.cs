using System;
using System.IO;
using System.Net;
using System.Net.Sockets;
using System.Threading.Tasks;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using Test.Lifetimes;

namespace Test.RdFramework
{
  public class SocketProxy
  {
    public readonly string Id;
    private readonly Lifetime myLifetime;
    private readonly int myServerPort;
    private readonly ILog myLogger;

    private int? myPort;

    public int Port
    {
      get
      {
        if (myPort == null)
        {
          throw new InvalidOperationException("SocketProxy was not started");
        }

        return myPort.Value;
      }
    }

    private const int DefaultBufferSize = 16370;
    private readonly byte[] myServerToClientBuffer = new byte[DefaultBufferSize];
    private readonly byte[] myClientToServerBuffer = new byte[DefaultBufferSize];

    private readonly SequentialLifetimes myServerToClientLifetime;
    private readonly SequentialLifetimes myClientToServerLifetime;
    private TcpClient myProxyServer;
    private TcpListener myProxyClient;

    internal SocketProxy(string id, Lifetime lifetime, int serverPort)
    {
      Id = id;
      myLifetime = lifetime;
      myServerPort = serverPort;
      myLogger = Log.GetLog<SocketProxy>().GetSublogger(id);

      myServerToClientLifetime = new SequentialLifetimes(myLifetime).With(lifetimes => lifetimes.Next());
      myClientToServerLifetime = new SequentialLifetimes(myLifetime).With(lifetimes => lifetimes.Next());

      myLifetime.OnTermination(() =>
      {
        myPort = null;
        
        StopServerToClientMessaging();
        StopClientToServerMessaging();

        myProxyServer?.Close();
      });
    }

    internal SocketProxy(string id, Lifetime lifetime, IProtocol protocol) :
      this(id, lifetime, protocol.Wire.GetServerPort())
    {
    }

    public async void Start()
    {
      static void SetSocketOptions(TcpClient acceptedClient) => acceptedClient.NoDelay = true;

      while (myLifetime.IsAlive)
      {
        try
        {
          myLogger.Verbose("Creating proxies for server and client...");
          myProxyServer = new TcpClient(IPAddress.Loopback.ToString(), myServerPort);
          myProxyClient = new TcpListener(new IPEndPoint(IPAddress.Loopback, 0));

          SetSocketOptions(myProxyServer);

          myProxyClient.Start();

          myPort = ((IPEndPoint) myProxyClient.LocalEndpoint).Port;
          myLogger.Verbose($"Proxies for server on port {myServerPort} and client on port {Port} created successfully");

          var acceptedClient = await myProxyClient.AcceptTcpClientAsync();

          SetSocketOptions(acceptedClient);

          myLogger.Verbose($"New client connected on port {Port}");

          Connect(myProxyServer, acceptedClient);
        }
        catch (Exception e)
        {
          myLogger.Error(e, "Failed to create proxies");
        }
      }
    }

    private void Connect(TcpClient proxyServer, TcpClient proxyClient)
    {
      try
      {
        myLogger.Verbose("Connecting proxies between themselves...");

        Stream proxyServerStream = proxyServer.GetStream();
        Stream proxyClientStream = proxyClient.GetStream();

        Task.Run(
          () =>
          {
            Messaging("Server to client",
              proxyServerStream, proxyClientStream, myServerToClientBuffer, myServerToClientLifetime);
          },
          myLifetime);

        Task.Run(
          () =>
          {
            Messaging("Client to server",
              proxyClientStream, proxyServerStream, myClientToServerBuffer, myClientToServerLifetime);
          },
          myLifetime);

        myLogger.Verbose("Async transferring messages started");
      }
      catch (Exception e)
      {
        myLogger.Error(e, "Connecting proxies failed");
      }
    }

    private async void Messaging(string id, Stream source, Stream destination, byte[] buffer,
      SequentialLifetimes lifetimes)
    {
      while (myLifetime.IsAlive)
      {
        try
        {
          var length = await source.ReadAsync(buffer, 0, buffer.Length, myLifetime);
          if (length == 0)
          {
            myLogger.Verbose($"{id}: Connection lost");
            break;
          }

          myLogger.Verbose($"{id}: Message of length: {length} was read");
          if (!lifetimes.IsCurrentTerminated)
          {
            await destination.WriteAsync(buffer, 0, length, myLifetime);
            myLogger.Verbose($"{id}: Message of length: {length} was written");
          }
          else
          {
            myLogger.Verbose($"{id}: Message of length {length} was not transferred, because lifetime was terminated");
          }
        }
        catch (OperationCanceledException)
        {
          myLogger.Verbose($"{id}: Messaging cancelled");
        }
        catch (Exception e)
        {
          myLogger.Error(e, $"{id}: Messaging failed");
        }
      }
    }


    public void StopClientToServerMessaging()
    {
      myClientToServerLifetime.TerminateCurrent();
    }

    public void StartClientToServerMessaging()
    {
      myClientToServerLifetime.Next();
    }

    public void StopServerToClientMessaging()
    {
      myServerToClientLifetime.TerminateCurrent();
    }

    public void StartServerToClientMessaging()
    {
      myServerToClientLifetime.Next();
    }
  }
}