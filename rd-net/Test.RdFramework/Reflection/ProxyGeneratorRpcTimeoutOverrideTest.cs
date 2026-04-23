using JetBrains.Lifetimes;
using JetBrains.Rd.Reflection;
using NUnit.Framework;
using System;
using System.Threading;
using System.Threading.Tasks;

namespace Test.RdFramework.Reflection;

[TestFixture]
public class ProxyGeneratorRpcTimeoutOverrideTest : ProxyGeneratorTestBase
{
  protected override bool IsAsync => true;
  protected override bool RespectRpcTimeouts => true;

  ////////////////////////

  [RdRpc]
  public interface ICallTest
  {
    [RpcTimeout(1)]
    void MTimeout(Lifetime cancel);

    [RpcTimeout]
    void MQuick(Lifetime cancel);
  }

  [RdExt]
  internal class CallTest : RdExtReflectionBindableBase, ICallTest
  {
    public void MTimeout(Lifetime cancel) => SpinWait.SpinUntil(() => !cancel.IsAlive, 5000);
    public void MQuick(Lifetime cancel) { }
  }

  [Test, Timeout(1000)]
  public async Task TestRpcTimeouts()
  {
    ThrowLoggedExceptions();

    await TestTemplate<CallTest, ICallTest>(model =>
    {
      model.MQuick(TestLifetime); // should not throw, timeouts are satisfied
      ThrowLoggedExceptions();

      // should exit fast, expected timeout 1ms is violated, returning TimeoutException
      Assert.Throws<TimeoutException>(() => model.MTimeout(TestLifetime));

      return Task.CompletedTask;
    });
  }

  ////////////////////////

  [RdRpc, RpcTimeout(1)]
  public interface ICall2Test
  {
    void MTimeout(Lifetime cancel);
  }
  [RdExt]
  internal class Call2Test : RdExtReflectionBindableBase, ICall2Test
  {
    public void MTimeout(Lifetime cancel) => SpinWait.SpinUntil(() => !cancel.IsAlive, 5000);
  }

  [Test, Timeout(1000)]
  public async Task TestRpcTimeouts2()
  {
    await TestTemplate<Call2Test, ICall2Test>(model =>
    {
      // should exit fast, expected timeout 1ms is violated, returning TimeoutException
      Assert.Throws<TimeoutException>(() => model.MTimeout(TestLifetime));
      return Task.CompletedTask;
    });
  }
}