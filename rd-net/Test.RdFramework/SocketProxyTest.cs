using System.Collections.Generic;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Threading;
using NUnit.Framework;
using Test.Lifetimes;

namespace Test.RdFramework
{
  [TestFixture]
  [Ignore("TODO: this test tends to hang")]
  class SocketProxyTest : LifetimesTestBase
  {
    [Test]
    public void TestSimple()
    {
      // using var factory = Log.UsingLogFactory(new TextWriterLogFactory(Console.Out, LoggingLevel.TRACE));
      Lifetime.Using(lifetime =>
      {
        var proxyLifetimeDefinition = lifetime.CreateNested();
        var proxyLifetime = proxyLifetimeDefinition.Lifetime;
        {
          SynchronousScheduler.Instance.SetActive(lifetime);

          var serverProtocol = SocketWireTest.Server(lifetime);

          var proxy = new SocketProxy("TestProxy", proxyLifetime, serverProtocol).With(socketProxy =>
            socketProxy.Start());
          Thread.Sleep(SocketWireTest.DefaultTimeout);

          var clientProtocol = SocketWireTest.Client(lifetime, proxy.Port);

          var sp = NewRdSignal<int>().Static(1);
          sp.BindTopLevel(lifetime, serverProtocol, SocketWireTest.Top);

          var cp = NewRdSignal<int>().Static(1);
          cp.BindTopLevel(lifetime, clientProtocol, SocketWireTest.Top);

          var serverLog = new List<int>();
          var clientLog = new List<int>();

          sp.Advise(lifetime, i => serverLog.Add(i));
          cp.Advise(lifetime, i => clientLog.Add(i));

          //Connection is established for now

          sp.Fire(1);

          SpinWaitEx.SpinUntil(() => serverLog.Count == 1);
          SpinWaitEx.SpinUntil(() => clientLog.Count == 1);
          Assert.AreEqual(new List<int> {1}, serverLog);
          Assert.AreEqual(new List<int> {1}, clientLog);

          cp.Fire(2);

          SpinWaitEx.SpinUntil(() => serverLog.Count == 2);
          SpinWaitEx.SpinUntil(() => clientLog.Count == 2);
          Assert.AreEqual(new List<int> {1, 2}, serverLog);
          Assert.AreEqual(new List<int> {1, 2}, clientLog);

          proxy.StopServerToClientMessaging();

          cp.Advise(lifetime, i => Assert.AreNotSame(3, i, "Value {0} mustn't be received", 3));

          sp.Fire(3);

          SpinWaitEx.SpinUntil(() => serverLog.Count == 3);
          Assert.AreEqual(new List<int> {1, 2, 3}, serverLog);


          proxy.StopClientToServerMessaging();

          sp.Advise(lifetime, i => Assert.AreNotSame(4, i, "Value {0} mustn't be received", 4));

          cp.Fire(4);

          SpinWaitEx.SpinUntil(() => clientLog.Count == 3);
          Assert.AreEqual(new List<int> {1, 2, 4}, clientLog);

          //Connection is broken for now

          proxy.StartServerToClientMessaging();

          sp.Fire(5);
          SpinWaitEx.SpinUntil(() => serverLog.Count == 4);
          SpinWaitEx.SpinUntil(() => clientLog.Count == 4);
          Assert.AreEqual(new List<int> {1, 2, 3, 5}, serverLog);
          Assert.AreEqual(new List<int> {1, 2, 4, 5}, clientLog);


          proxy.StartClientToServerMessaging();

          cp.Fire(6);
          SpinWaitEx.SpinUntil(() => serverLog.Count == 5);
          SpinWaitEx.SpinUntil(() => clientLog.Count == 5);
          Assert.AreEqual(new List<int> {1, 2, 3, 5, 6}, serverLog);
          Assert.AreEqual(new List<int> {1, 2, 4, 5, 6}, clientLog);

          //Connection is established for now

          proxyLifetimeDefinition.Terminate();

          
          cp.Advise(lifetime, i => Assert.AreNotSame(7, i, "Value {0} mustn't be received", 7));
          sp.Fire(7);
          
          SpinWaitEx.SpinUntil(() => serverLog.Count == 6);
          Assert.AreEqual(new List<int> {1, 2, 3, 5, 6, 7}, serverLog);


          sp.Advise(lifetime, i => Assert.AreNotSame(8, i, "Value {0} mustn't be received", 8));
          cp.Fire(8);

          SpinWaitEx.SpinUntil(() => clientLog.Count == 6);
          Assert.AreEqual(new List<int> {1, 2, 4, 5, 6, 8}, clientLog);

          //Connection is broken for now, proxy is not alive
        }
      });
    }
  }
}