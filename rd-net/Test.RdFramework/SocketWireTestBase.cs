#if !NET35

using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Threading;
using NUnit.Framework;
using Test.Lifetimes;

namespace Test.RdFramework;

public abstract class SocketWireTestBase<T> : LifetimesTestBase
{
  internal static TimeSpan DefaultTimeout = TimeSpan.FromMilliseconds(100);

  internal const string Top = "top";
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

  internal abstract (IProtocol ServerProtocol, IProtocol ClientProtocol) CreateServerClient(Lifetime lifetime);
  internal abstract T GetPortOrPath();
  internal abstract (IProtocol ServerProtocol, T portOrPath) Server(Lifetime lifetime, T portOrPath = default);
  internal abstract IProtocol Client(Lifetime lifetime, T portOrPath);
  internal abstract EndPointWrapper CreateEndpointWrapper();

  [Test]
  public void TestBasicRun()
  {
    Lifetime.Using(lifetime =>
    {
      SynchronousScheduler.Instance.SetActive(lifetime);
      var (serverProtocol, clientProtocol) = CreateServerClient(lifetime);

      var sp = NewRdProperty<int>().Static(1);
      sp.BindTopLevel(lifetime, serverProtocol, Top);
      var cp = NewRdProperty<int>().Static(1);
      cp.BindTopLevel(lifetime, clientProtocol, Top);

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
      var (serverProtocol, clientProtocol) = CreateServerClient(lifetime);

      var sp = NewRdProperty<int>().Static(1);
      sp.BindTopLevel(lifetime, serverProtocol, Top);
      var cp = NewRdProperty<int>().Static(1);
      cp.BindTopLevel(lifetime, clientProtocol, Top);

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
      var (serverProtocol, clientProtocol) = CreateServerClient(lifetime);

      var sp = NewRdProperty<string>().Static(1);
      sp.BindTopLevel(lifetime, serverProtocol, Top);
      var cp = NewRdProperty<string>().Static(1);
      cp.BindTopLevel(lifetime, clientProtocol, Top);

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

      var portOrPath = GetPortOrPath();
      var clientProtocol = Client(lifetime, portOrPath);

      var cp = NewRdProperty<int>().Static(1);
      cp.BindTopLevel(lifetime, clientProtocol, Top);
      cp.SetValue(1);

      Thread.Sleep(2000);
      var (serverProtocol, _) = Server(lifetime, portOrPath);
      var sp = NewRdProperty<int>().Static(1);
      sp.BindTopLevel(lifetime, serverProtocol, Top);

      var prev = sp.Maybe;


      cp.SetValue(4);
      Thread.Sleep(200);
      WaitAndAssert(sp, 4, prev);
    });
  }


  [Test]
  [Timeout(5000)]
  public void TestServerWithoutClient()
  {
    Lifetime.Using(lifetime =>
    {
      WithLongTimeout(lifetime);
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
      var (protocol, _) = Server(lifetime);
      Thread.Sleep(100);
      var p = NewRdProperty<int>().Static(1);
      p.BindTopLevel(lifetime, protocol, Top);
      p.SetValue(1);
      p.SetValue(2);
      Thread.Sleep(50);
    });
  }


  [Test]
  [Timeout(5000)]
  public void TestClientWithoutServer()
  {
    Lifetime.Using(lifetime =>
    {
      WithLongTimeout(lifetime);
      SynchronousScheduler.Instance.SetActive(lifetime);
      Client(lifetime, GetPortOrPath());
    });
  }

  [Test]
  public void TestClientWithoutServerWithDelay()
  {
    Lifetime.Using(lifetime =>
    {
      SynchronousScheduler.Instance.SetActive(lifetime);
      Client(lifetime, GetPortOrPath());
      Thread.Sleep(100);
    });
  }

  [Test]
  public void TestClientWithoutServerWithDelayAndMessages()
  {
    Lifetime.Using(lifetime =>
    {
      SynchronousScheduler.Instance.SetActive(lifetime);
      var protocol = Client(lifetime, GetPortOrPath());
      Thread.Sleep(100);
      var p = NewRdProperty<int>().Static(1);
      p.BindTopLevel(lifetime, protocol, Top);
      p.SetValue(1);
      p.SetValue(2);
      Thread.Sleep(50);
    });
  }


  [Test, Ignore("https://github.com/JetBrains/rd/issues/69")]
  public void TestDisconnect() => TestDisconnectBase((list, i) => list.Add(i));

  [Test]
  public void TestDisconnect_AllowDuplicates() => TestDisconnectBase((list, i) =>
  {
    // values may be duplicated due to asynchronous acknowledgement
    if (list.LastOrDefault() < i)
      list.Add(i);
  });

  private void TestDisconnectBase(Action<List<int>, int> advise)
  {
    var timeout = TimeSpan.FromSeconds(1);
      
    Lifetime.Using(lifetime =>
    {
      SynchronousScheduler.Instance.SetActive(lifetime);
      var (serverProtocol, clientProtocol) = CreateServerClient(lifetime);

      var sp = NewRdSignal<int>().Static(1);
      sp.BindTopLevel(lifetime, serverProtocol, Top);

      var cp = NewRdSignal<int>().Static(1);
      cp.BindTopLevel(lifetime, clientProtocol, Top);

      var log = new List<int>();
      sp.Advise(lifetime, i => advise(log, i));

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
      var (serverProtocol, portOrPath) = Server(lifetime, default);
          
      var sp = NewRdProperty<int>().Static(1);
      sp.BindTopLevel(lifetime, serverProtocol, Top);
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
        var clientProtocol = Client(lf, portOrPath);
        var cp = NewRdProperty<int>().Static(1);
        cp.IsMaster = true;
        cp.BindTopLevel(lf, clientProtocol, Top);
        cp.SetValue(1);            
        WaitAndAssert(sp, 1);            
        Assert.AreEqual(1, clientCount);
      });

          
      Lifetime.Using(lf =>
      {
        sp = NewRdProperty<int>().Static(2);
        sp.BindTopLevel(lifetime, serverProtocol, Top);
            
        var clientProtocol = Client(lf, portOrPath);
        var cp = NewRdProperty<int>().Static(2);
        cp.BindTopLevel(lf, clientProtocol, Top);
        cp.SetValue(2);
        WaitAndAssert(sp, 2);
        Assert.AreEqual(2, clientCount);
      });                  
          
          
      Lifetime.Using(lf =>
      {                        
        var clientProtocol = Client(lf, portOrPath);
        var cp = NewRdProperty<int>().Static(2);
        cp.BindTopLevel(lf, clientProtocol, Top);
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
    var endPointWrapper = CreateEndpointWrapper();
    var factory = new SocketWire.ServerFactory(sLifetime.Lifetime, SynchronousScheduler.Instance, endPointWrapper);
      
    var lf1 = new LifetimeDefinition();
    // ReSharper disable once PossibleInvalidOperationException
    if (endPointWrapper.EndPointImpl is IPEndPoint)
    {
      new SocketWire.Client(lf1.Lifetime, SynchronousScheduler.Instance, factory.LocalPort.Value);
      SpinWaitEx.SpinUntil(() => factory.Connected.Count == 1);
        
      var lf2 = new LifetimeDefinition();
      new SocketWire.Client(lf2.Lifetime, SynchronousScheduler.Instance, factory.LocalPort.Value);
      SpinWaitEx.SpinUntil(() => factory.Connected.Count == 2);      
    }
    else
    {
#if NET8_0_OR_GREATER
      var connectionParams = new EndPointWrapper.UnixSocketConnectionParams { Path = factory.LocalPath };
      new SocketWire.Client(lf1.Lifetime, SynchronousScheduler.Instance, connectionParams);
      SpinWaitEx.SpinUntil(() => factory.Connected.Count == 1);
        
      var lf2 = new LifetimeDefinition();
      new SocketWire.Client(lf2.Lifetime, SynchronousScheduler.Instance, connectionParams);
      SpinWaitEx.SpinUntil(() => factory.Connected.Count == 2);
#endif
    }
      
    lf1.Terminate();
    SpinWaitEx.SpinUntil(() => factory.Connected.Count == 1);
      
    sLifetime.Terminate();
    SpinWaitEx.SpinUntil(() => factory.Connected.Count == 0);
  }


  private static void CloseSocket(IProtocol protocol)
  {
    if (!(protocol.Wire is SocketWire.Base socketWire))
    {
      Assert.Fail();
      return;
    }
      
    SocketWire.Base.CloseSocket(socketWire.Socket.NotNull());
  }

  private static void WithLongTimeout(Lifetime lifetime)
  {
    var oldValue = SocketWire.Base.TimeoutMs;
    lifetime.Bracket(() => SocketWire.Base.TimeoutMs = 100_000, () => SocketWire.Base.TimeoutMs = oldValue);
  }
}
#endif