using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Lifetimes;
using JetBrains.Rd.Reflection;
using JetBrains.Rd.Tasks;
using NUnit.Framework;

namespace Test.RdFramework.Reflection;

/// <summary>
/// Tests for <see cref="SyncCallMonitor"/> covering the <see cref="ProxyGeneratorUtil.SyncNested"/> code path.
/// </summary>
[TestFixture]
public class SyncCallMonitorViaProxyTest : ProxyGeneratorTestBase
{
  protected override bool IsAsync => true;
  protected override bool RespectRpcTimeouts => true;

  [RdRpc]
  public interface IMonitorCallTest
  {
    void FastCall(Lifetime cancel);

    [RpcTimeout(1)]
    void SlowCall(Lifetime cancel);
  }

  [RdExt]
  internal class MonitorCallTest : RdExtReflectionBindableBase, IMonitorCallTest
  {
    public void FastCall(Lifetime cancel) { }
    public void SlowCall(Lifetime cancel) => SpinWait.SpinUntil(() => !cancel.IsAlive, 5000);
  }

  private SyncCallInfo? myFinished;
  private SyncCallInfo? myTimedOut;

  public override void SetUp()
  {
    base.SetUp();
    myFinished = null;
    myTimedOut = null;
    SyncCallMonitor.SyncCallFinished += OnFinished;
    SyncCallMonitor.SyncCallTimedOut += OnTimedOut;
  }

  public override void TearDown()
  {
    SyncCallMonitor.SyncCallFinished -= OnFinished;
    SyncCallMonitor.SyncCallTimedOut -= OnTimedOut;
    base.TearDown();
  }

  private void OnFinished(SyncCallInfo info) => myFinished = info;
  private void OnTimedOut(SyncCallInfo info) => myTimedOut = info;

  [Test, Timeout(5000)]
  public async Task SuccessfulCall_FiresSyncCallFinished_NotTimedOut()
  {
    await TestTemplate<MonitorCallTest, IMonitorCallTest>(model =>
    {
      model.FastCall(TestLifetime);

      Assert.NotNull(myFinished);
      Assert.Null(myTimedOut);
      Assert.IsNotEmpty(myFinished!.Value.Location.ToString());
      Assert.GreaterOrEqual(myFinished.Value.ElapsedTime.TotalMilliseconds, 0);
      return Task.CompletedTask;
    });
  }

  [Test, Timeout(5000)]
  public async Task TimedOutCall_FiresSyncCallTimedOut_NotFinished()
  {
    await TestTemplate<MonitorCallTest, IMonitorCallTest>(model =>
    {
      Assert.Throws<TimeoutException>(() => model.SlowCall(TestLifetime));

      Assert.NotNull(myTimedOut);
      Assert.Null(myFinished);
      Assert.IsNotEmpty(myTimedOut!.Value.Location.ToString());
      Assert.GreaterOrEqual(myTimedOut.Value.ElapsedTime.TotalMilliseconds, 0);
      return Task.CompletedTask;
    });
  }
}

/// <summary>
/// Tests for <see cref="SyncCallMonitor"/> covering the <see cref="RdCall{TReq,TRes}.Sync"/> code path.
/// </summary>
[TestFixture]
[Apartment(ApartmentState.STA)]
public class SyncCallMonitorDirectSyncTest : RdFrameworkTestBase
{
  private const int ourKey = 1;
  private bool myPreviousRespectRpcTimeouts;

  private SyncCallInfo? myFinished;
  private SyncCallInfo? myTimedOut;

  public override void SetUp()
  {
    myPreviousRespectRpcTimeouts = RpcTimeouts.RespectRpcTimeouts;
    RpcTimeouts.RespectRpcTimeouts = true;
    base.SetUp();
    ClientWire.AutoTransmitMode = true;
    ServerWire.AutoTransmitMode = true;
    SyncCallMonitor.SyncCallFinished += OnFinished;
    SyncCallMonitor.SyncCallTimedOut += OnTimedOut;
    myFinished = null;
    myTimedOut = null;
  }

  public override void TearDown()
  {
    SyncCallMonitor.SyncCallFinished -= OnFinished;
    SyncCallMonitor.SyncCallTimedOut -= OnTimedOut;
    RpcTimeouts.RespectRpcTimeouts = myPreviousRespectRpcTimeouts;
    base.TearDown();
  }

  private void OnFinished(SyncCallInfo info) => myFinished = info;
  private void OnTimedOut(SyncCallInfo info) => myTimedOut = info;

  [Test]
  public void SuccessfulCall_FiresSyncCallFinished_NotTimedOut()
  {
    var caller = BindToServer(TestLifetime, NewRdCall<int, string>(), ourKey);
    BindToClient(TestLifetime, NewRdCall<int, string>(), ourKey)
      .SetSync((_, req) => req.ToString());

    var result = caller.Sync(42);

    Assert.AreEqual("42", result);
    Assert.NotNull(myFinished);
    Assert.Null(myTimedOut);
    Assert.IsNotEmpty(myFinished!.Value.Location.ToString());
    Assert.GreaterOrEqual(myFinished.Value.ElapsedTime.TotalMilliseconds, 0);
  }

  [Test]
  public void TimedOutCall_FiresSyncCallTimedOut_NotFinished()
  {
    // Disable auto-transmit so the request is queued but never delivered: the task never
    // completes and Sync times out waiting for it.
    ServerWire.AutoTransmitMode = false;

    var caller = BindToServer(TestLifetime, NewRdCall<int, string>(), ourKey);
    BindToClient(TestLifetime, NewRdCall<int, string>(), ourKey)
      .SetSync((_, req) => req.ToString());

    var timeouts = new RpcTimeouts(TimeSpan.FromMilliseconds(1), TimeSpan.FromMilliseconds(1));
    Assert.Throws<TimeoutException>(() => caller.Sync(42, timeouts));
    ServerWire.MissOneMessage(); // drop queued request to satisfy TearDown wire-empty assertion

    Assert.NotNull(myTimedOut);
    Assert.Null(myFinished);
    Assert.IsNotEmpty(myTimedOut!.Value.Location.ToString());
    Assert.AreEqual(timeouts.WarnAwaitTime, myTimedOut.Value.Timeouts.WarnAwaitTime);
    Assert.AreEqual(timeouts.ErrorAwaitTime, myTimedOut.Value.Timeouts.ErrorAwaitTime);
  }
}
