using System;
using System.Collections.Generic;
using System.Net;
using System.Net.Sockets;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Threading;
using JetBrains.Util;
using NUnit.Framework;
using Test.Lifetimes;

namespace Test.RdFramework
{
  [TestFixture]
  public class SocketWireTest : LifetimesTestBase
  {

    public const string Top = "top";
    private void WaitAndAssert<T>(RdProperty<T> property, T expected, T prev)
    {
      WaitAndAssert(property, expected, new Maybe<T>(prev));
    }


    private void WaitAndAssert<T>(RdProperty<T> property, T expected, Maybe<T> prev = default(Maybe<T>))
    {
      var start = Environment.TickCount;
      const int timeout = 5000;
      while (Environment.TickCount - start < timeout && property.Maybe == prev) Thread.Sleep(10);
      if (property.Maybe == prev)
        throw new TimeoutException($"Timeout {timeout} ms while waiting for value '{expected}'");
      Assert.AreEqual(expected, property.Value);
    }


    static int FindFreePort()
    {
      TcpListener l = new TcpListener(IPAddress.Loopback, 0);
      l.Start();
      int port = ((IPEndPoint) l.LocalEndpoint).Port;
      l.Stop();
      return port;
    }


    private IProtocol Server(Lifetime lifetime, int? port = null)
    {
      var id = "TestServer";
      return new Protocol(id, new Serializers(), new Identities(IdKind.Server), SynchronousScheduler.Instance,
          new SocketWire.Server(lifetime, SynchronousScheduler.Instance, new IPEndPoint(IPAddress.Loopback, port ?? 0), id), lifetime
      );
    }

    private IProtocol Client(Lifetime lifetime, int port)
    {
      var id = "TestClient";
      return new Protocol(id, new Serializers(), new Identities(IdKind.Server), SynchronousScheduler.Instance,        
          new SocketWire.Client(lifetime, SynchronousScheduler.Instance, port, id), lifetime
      );
    }

    private IProtocol Client(Lifetime lifetime, IProtocol serverProtocol)
    {
      // ReSharper disable once PossibleNullReferenceException
      return Client(lifetime, (serverProtocol.Wire as SocketWire.Server).Port);
    }

    [Test]
    public void TestBasicRun()
    {
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);
        var serverProtocol = Server(lifetime);
        var clientProtocol = Client(lifetime, serverProtocol);

        var sp = new RdProperty<int>().Static(1);
        sp.Bind(lifetime, serverProtocol, Top);
        var cp = new RdProperty<int>().Static(1);
        cp.Bind(lifetime, clientProtocol, Top);

        cp.SetValue(1);
        WaitAndAssert(sp, 1);
      });
    }


    [Test]
    public void TestOrdering()
    {
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);
        var serverProtocol = Server(lifetime);
        var clientProtocol = Client(lifetime, serverProtocol);

        var sp = new RdProperty<int>().Static(1);
        sp.Bind(lifetime, serverProtocol, Top);
        var cp = new RdProperty<int>().Static(1);
        cp.Bind(lifetime, clientProtocol, Top);

        var log = new List<int>();
        sp.Advise(lifetime, it => log.Add(it));
        sp.SetValue(1);
        sp.SetValue(2);
        sp.SetValue(3);
        sp.SetValue(4);
        sp.SetValue(5);

        while (log.Count < 5) Thread.Sleep(10);
        CollectionAssert.AreEqual(new[] {1, 2, 3, 4, 5}, log);
      });
    }


    [Test]
    public void TestBigBuffer()
    {
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);
        var serverProtocol = Server(lifetime);
        var clientProtocol = Client(lifetime, serverProtocol);

        var sp = new RdProperty<string>().Static(1);
        sp.Bind(lifetime, serverProtocol, Top);
        var cp = new RdProperty<string>().Static(1);
        cp.Bind(lifetime, clientProtocol, Top);

        cp.SetValue("1");
        WaitAndAssert(sp, "1");

        sp.SetValue(new string('a', 100000));
        WaitAndAssert(cp, new string('a', 100000), "1");

        cp.SetValue("a");
        WaitAndAssert(sp, "a", new string('a', 100000));

        cp.SetValue("ab");
        WaitAndAssert(sp, "ab", "a");

        cp.SetValue("abc");
        WaitAndAssert(sp, "abc", "ab");
      });
    }


    [Test]
    public void TestRunWithSlowpokeServer()
    {
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);

        var port = FindFreePort();
        var clientProtocol = Client(lifetime, port);

        var cp = new RdProperty<int>().Static(1);
        cp.Bind(lifetime, clientProtocol, Top);
        cp.SetValue(1);

        Thread.Sleep(2000);
        var serverProtocol = Server(lifetime, port);
        var sp = new RdProperty<int>().Static(1);
        sp.Bind(lifetime, serverProtocol, Top);

        var prev = sp.Maybe;


        cp.SetValue(4);
        Thread.Sleep(200);
        WaitAndAssert(sp, 4, prev);
      });
    }


    [Test]
    public void TestServerWithoutClient()
    {
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);
        Server(lifetime);
      });
    }

    [Test]
    public void TestServerWithoutClientWithDelay()
    {
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);
        Server(lifetime);
        Thread.Sleep(100);
      });
    }

    [Test]
    public void TestServerWithoutClientWithDelayAndMessages()
    {
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);
        var protocol = Server(lifetime);
        Thread.Sleep(100);
        var p = new RdProperty<int>().Static(1);
        p.Bind(lifetime, protocol, Top);
        p.SetValue(1);
        p.SetValue(2);
        Thread.Sleep(50);
      });
    }


    [Test]
    public void TestClientWithoutServer()
    {
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);
        Client(lifetime, FindFreePort());
      });
    }

    [Test]
    public void TestClientWithoutServerWithDelay()
    {
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);
        Client(lifetime, FindFreePort());
        Thread.Sleep(100);
      });
    }

    [Test]
    public void TestClientWithoutServerWithDelayAndMessages()
    {
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);
        var protocol = Client(lifetime, FindFreePort());
        Thread.Sleep(100);
        var p = new RdProperty<int>().Static(1);
        p.Bind(lifetime, protocol, Top);
        p.SetValue(1);
        p.SetValue(2);
        Thread.Sleep(50);
      });
    }


    [Test]
    public void TestDisconnect()
    {
      var timeout = TimeSpan.FromSeconds(5);
      
      Lifetime.Using(lifetime =>
      {
        SynchronousScheduler.Instance.SetActive(lifetime);
        var serverProtocol = Server(lifetime, null);
        var clientProtocol = Client(lifetime, serverProtocol);

        var sp = new RdSignal<int>().Static(1);
        sp.Bind(lifetime, serverProtocol, Top);

        var cp = new RdSignal<int>().Static(1);
        cp.Bind(lifetime, clientProtocol, Top);

        var log = new List<int>();
        sp.Advise(lifetime, i => log.Add(i));

        cp.Fire(1);
        cp.Fire(2);
        Assert.True(SpinWaitEx.SpinUntil(timeout, () => log.Count == 2));
        Assert.AreEqual(new List<int> {1, 2}, log);

        CloseSocket(clientProtocol);
        cp.Fire(3);
        cp.Fire(4);

        Assert.True(SpinWaitEx.SpinUntil(timeout, () => log.Count == 4));
        Assert.AreEqual(new List<int> {1, 2, 3, 4}, log);

        CloseSocket(serverProtocol);
        cp.Fire(5);
        cp.Fire(6);

        Assert.True(SpinWaitEx.SpinUntil(timeout, () => log.Count == 6));
        Assert.AreEqual(new List<int> {1, 2, 3, 4, 5, 6}, log);
      });
    }

    [Test]
    public void TestReconnect()
    {
      Lifetime.Using(lifetime => 
        {
          SynchronousScheduler.Instance.SetActive(lifetime);
          var serverProtocol = Server(lifetime, null);
          
          var sp = new RdProperty<int>().Static(1);
          sp.Bind(lifetime, serverProtocol, Top);
          sp.IsMaster = false;

          var wire = serverProtocol.Wire as SocketWire.Base;
          int clientCount = 0;
          wire.NotNull().Connected.WhenTrue(lifetime, _ =>
          {
            clientCount++;
          });
          
          Assert.AreEqual(0, clientCount);
          
          Lifetime.Using(lf =>
          {
            var clientProtocol = Client(lf, serverProtocol);
            var cp = new RdProperty<int>().Static(1);
            cp.IsMaster = true;
            cp.Bind(lf, clientProtocol, Top);
            cp.SetValue(1);            
            WaitAndAssert(sp, 1);            
            Assert.AreEqual(1, clientCount);
          });

          
          Lifetime.Using(lf =>
          {
            sp = new RdProperty<int>().Static(2);
            sp.Bind(lifetime, serverProtocol, Top);
            
            var clientProtocol = Client(lf, serverProtocol);
            var cp = new RdProperty<int>().Static(2);
            cp.Bind(lf, clientProtocol, Top);
            cp.SetValue(2);
            WaitAndAssert(sp, 2);
            Assert.AreEqual(2, clientCount);
          });                  
          
          
          Lifetime.Using(lf =>
          {                        
            var clientProtocol = Client(lf, serverProtocol);
            var cp = new RdProperty<int>().Static(2);
            cp.Bind(lf, clientProtocol, Top);
            cp.SetValue(3);      
            WaitAndAssert(sp, 3, 2);
            Assert.AreEqual(3, clientCount);
          });      

        });
      
    }



    [Test]
    public void TestSocketFactory()
    {
      var sLifetime = new LifetimeDefinition();
      var factory = new SocketWire.ServerFactory(sLifetime.Lifetime, SynchronousScheduler.Instance);
      
      var lf1 = new LifetimeDefinition();
      new SocketWire.Client(lf1.Lifetime, SynchronousScheduler.Instance, factory.LocalPort);
      SpinWaitEx.SpinUntil(() => factory.Connected.Count == 1);
      
      var lf2 = new LifetimeDefinition();
      new SocketWire.Client(lf2.Lifetime, SynchronousScheduler.Instance, factory.LocalPort);
      SpinWaitEx.SpinUntil(() => factory.Connected.Count == 2);
      
      
      lf1.Terminate();
      SpinWaitEx.SpinUntil(() => factory.Connected.Count == 1);
      
      sLifetime.Terminate();
      SpinWaitEx.SpinUntil(() => factory.Connected.Count == 0);
    }
      
      

    private static void CloseSocket(IProtocol protocol)
    {
      if (!(protocol.Wire is SocketWire.Base socketWire))
        return;
      
      SocketWire.Base.CloseSocket(socketWire.Socket);
    }
  }
}