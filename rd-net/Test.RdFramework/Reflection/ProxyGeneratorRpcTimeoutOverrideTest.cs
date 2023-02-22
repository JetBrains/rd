using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

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
    void MTimeout();

    [RpcTimeout]
    void MQuick();
  }

  [RdExt]
  internal class CallTest : RdExtReflectionBindableBase, ICallTest
  {
    public void MTimeout() => Thread.Sleep(50);
    public void MQuick() { }
  }

  [Test]
  public async Task TestRpcTimeouts()
  {
    ThrowLoggedExceptions();

    await TestTemplate<CallTest, ICallTest>(model =>
    {
      model.MQuick(); // should not throw, timeouts are satisfied
      ThrowLoggedExceptions(); 

      // should produce log message with level=Error, timeout 1ms is violated
      model.MTimeout();
      Assert.Throws<Exception>(ThrowLoggedExceptions);

      return Task.CompletedTask;
    });
  }

  ////////////////////////

  [RdRpc, RpcTimeout(1)]
  public interface ICall2Test
  {
    void MTimeout();
  }
  [RdExt]
  internal class Call2Test : RdExtReflectionBindableBase, ICall2Test
  {
    public void MTimeout() => Thread.Sleep(50);
  }

  [Test]
  public async Task TestRpcTimeouts2()
  {
    await TestTemplate<Call2Test, ICall2Test>(model =>
    {
      // should produce log message with level=Error, timeout 1ms is violated
      model.MTimeout();
      Assert.Throws<Exception>(ThrowLoggedExceptions);
      return Task.CompletedTask;
    });
  }
}