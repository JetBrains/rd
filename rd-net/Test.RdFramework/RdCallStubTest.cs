#nullable enable
using System;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Tasks;
using JetBrains.Serialization;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.RdFramework;

[TestFixture]
public class RdCallStubTest : RdFrameworkTestBase
{
  private class StubWire : IWire
  {
    public bool IsStub => true;
    public void Send<TParam>(RdId id, TParam param, Action<TParam, UnsafeWriter> writer) {}
    public void Advise(Lifetime lifetime, IRdWireable entity) {}
    public ProtocolContexts Contexts { get; set; } = null!;
    public IRdWireable? TryGetById(RdId rdId) => null;
  }

  [Test]
  public void TestStubWireBindableResult()
  {
    var scheduler = SynchronousScheduler.Instance;
    var wire = new StubWire();
    var protocol = new Protocol("TestProtocol", new Serializers(), new SequentialIdentities(IdKind.Server), scheduler, wire, LifetimeDefinition.Lifetime);
    wire.Contexts = protocol.Contexts;
    var call = NewRdCall<int, RdProperty<string>>();
    using (AllowBindCookie.Create())
    {
      call.PreBind(LifetimeDefinition.Lifetime, protocol, "call");
      call.Bind();
    }
    call.SetSync((_, req) =>
    {
      var property = NewRdProperty<string>();
      property.Value = req.ToString();
      return property;
    });
    var ld = new LifetimeDefinition();
    var lf = ld.Lifetime;
    var prop = call.Start(lf, 123).AsTask().GetOrWait(lf);
    Assert.AreEqual(prop.Value, "123");
    Assert.True(prop.IsBound);
    Assert.False(prop.RdId.IsNil);
    ld.Terminate();
    Assert.False(prop.IsBound);
  }
}