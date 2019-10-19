using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class ProxyGeneratorAsyncCallsTest : ProxyGeneratorTestBase
  {
    public class AsyncTestFixture<T>
    {
      public virtual int Rounds => 1;

      public virtual void Client(int round) { }
      public virtual void Server(int round) { }
    }

    [RdRpc]
    public interface IAsyncCallsTest
    {
      Task<string> GetStringAsync();
      Task RunSomething();

      Task<int> iSum(int a, int b);
      Task<ulong> ulSum(ulong a, ulong b);
      Task<short> sSum(short a, short b);
      Task<ushort> usSum(ushort a, ushort b);
      Task<byte> bSum(byte a, byte b);
      Task<uint> uiSum(uint a, uint b);
      Task<long> lSum(long a, long b);
    }

    [RdExt]
    public class AsyncCallsTest : RdReflectionBindableBase, IAsyncCallsTest
    {
      public Task<string> GetStringAsync()
      {
        return Task.FromResult("result");
      }

      public Task RunSomething()
      {
        return Task.CompletedTask;
      }

      public Task<int> iSum(int a, int b) => Task.FromResult(a + b);
      public Task<uint> uiSum(uint a, uint b) => Task.FromResult(a + b);
      public Task<long> lSum(long a, long b) => Task.FromResult(a + b);
      public Task<ulong> ulSum(ulong a, ulong b) => Task.FromResult(a + b);
      public Task<short> sSum(short a, short b) => Task.FromResult(unchecked((short) (a + b)));
      public Task<ushort> usSum(ushort a, ushort b) => Task.FromResult(unchecked((ushort) (a + b)));
      public Task<byte> bSum(byte a, byte b) => Task.FromResult(unchecked((byte) (a + b)));
    }

    [Test]
    public void TestAsync()
    {
      string result = null;
      TestAsyncCalls(model =>
      {
        model.GetStringAsync().ContinueWith(t => result = t.Result, TaskContinuationOptions.ExecuteSynchronously);
      });
      Assert.AreEqual(result, "result");
    }

    [Test]
    public void TestAsyncVoid()
    {
      // todo: really check long running task result
      TestAsyncCalls(model => { model.RunSomething(); });
    }

    [Test] public void TestAsyncSum1() => TestAsyncCalls(model => Assert.AreEqual(model.iSum(100, -150).Result, -50));
    [Test] public void TestAsyncSum2() => TestAsyncCalls(model => Assert.AreEqual(model.uiSum(uint.MaxValue, 0).Result, uint.MaxValue));
    [Test] public void TestAsyncSum3() => TestAsyncCalls(model => Assert.AreEqual(model.sSum(100, -150).Result, -50));
    [Test] public void TestAsyncSum4() => TestAsyncCalls(model => Assert.AreEqual(model.usSum(ushort.MaxValue, 1).Result, 0));
    [Test] public void TestAsyncSum5() => TestAsyncCalls(model => Assert.AreEqual(model.lSum(long.MaxValue, 0).Result, long.MaxValue));
    [Test] public void TestAsyncSum6() => TestAsyncCalls(model => Assert.AreEqual(model.ulSum(ulong.MaxValue, 0).Result, ulong.MaxValue));
    [Test] public void TestAsyncSum7() => TestAsyncCalls(model => Assert.AreEqual(model.bSum(byte.MaxValue, 1).Result, 0));

    private void TestAsyncCalls(Action<IAsyncCallsTest> run) => TestTemplate<AsyncCallsTest, IAsyncCallsTest>(run);

    private void TestTemplate<TImpl, TInterface>(Action<TInterface> runTest) where TImpl : RdBindableBase where TInterface : class
    {
      ClientProtocol.Scheduler.Queue(() =>
      {
        var client = ReflectionRdActivator.ActivateBind<TImpl>(TestLifetime, ClientProtocol);
      });

      TInterface proxy = null;
      ServerProtocol.Scheduler.Queue(() => { proxy = CreateServerProxy<TInterface>(); });

      WaitMessages();

      using (var barrier = new ManualResetEvent(false))
      {
        ServerProtocol.Scheduler.Queue(() =>
          Assertion.Assert((proxy as RdReflectionBindableBase).NotNull().Connected.Value, "((RdReflectionBindableBase)proxy).Connected.Value"));
        runTest(proxy);
        ServerProtocol.Scheduler.Queue(() => barrier.Set());

        WaitMessages();
        barrier.WaitOne();
      }
    }

    private void WaitMessages()
    {
      bool IsIdle(IRdDynamic p) => ((SingleThreadScheduler) p.Proto.Scheduler).IsIdle;
      SpinWait.SpinUntil(() => IsIdle(ServerProtocol) && IsIdle(ClientProtocol));
    }

    protected override IScheduler CreateScheduler(bool isServer)
    {
      string name = (isServer ? "server" : "client") + " scheduler";
      IScheduler result = null;

      var thread = new Thread(() => SingleThreadScheduler.RunInCurrentStackframe(TestLifetime, name, s => result = s)) { Name = name };
      thread.SetApartmentState(ApartmentState.STA);
      thread.Start();
      SpinWait.SpinUntil(() => result != null);
      return result;
    }
  }
}