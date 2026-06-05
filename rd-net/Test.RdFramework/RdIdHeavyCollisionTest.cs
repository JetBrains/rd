using System;
using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  public class RdIdHeavyCollisionTest
  {
    private const int NodeCount = 20000;
    private const int Seed = 1;

    private static IIdentities Legacy()
    {
#pragma warning disable CS0618 // Type or member is obsolete
      return new Identities(IdKind.Server);
#pragma warning restore CS0618
    }

    private static IIdentities Sequential() => new SequentialIdentities(IdKind.Server);

    private static IEnumerable<TestCaseData> Cases()
    {
      yield return new TestCaseData((Func<IIdentities>)Legacy, true)
        .SetName("legacy Next COLLIDES in a large decoupled graph");
      yield return new TestCaseData((Func<IIdentities>)Sequential, false)
        .SetName("SequentialIdentities stays unique across a large decoupled graph");
    }

    [TestCaseSource(nameof(Cases))]
    public void HeavyDecoupledGraph(Func<IIdentities> identitiesFactory, bool expectCollision)
    {
      var ld = new LifetimeDefinition();
      try
      {
        var lifetime = ld.Lifetime;
        var protocol = new Protocol("Heavy", new Serializers(), identitiesFactory(),
          SynchronousScheduler.Instance, new StubWire(), lifetime);
        var identities = protocol.Identities;

        var root = new Node();
        root.Identify(identities, new RdId(1), stable: false);
        var nodes = new List<Node> { root };

        using (AllowBindCookie.Create())
        {
          root.PreBind(lifetime, protocol, "root");

          var random = new Random(Seed);
          TestDelegate build = () =>
          {
            for (var k = 0; k < NodeCount; k++)
            {
              // Attach to an arbitrary earlier node: decoupling parent id from allocation order is what
              // exposes legacy Next's poor distribution (a balanced tree would not collide).
              var parent = nodes[random.Next(nodes.Count)];
              var child = new Node();
              child.Identify(identities, identities.Next(parent.RdId), stable: false);
              child.PreBind(lifetime, parent, "n" + k); // registers; throws on a duplicate RdId
              nodes.Add(child);
            }
          };

          if (expectCollision)
            Assert.Throws<ArgumentException>(build, "legacy dynamic ids must collide in a large decoupled graph");
          else
            Assert.DoesNotThrow(build, "SequentialIdentities must stay unique across the whole graph");
        }
      }
      finally
      {
        ld.Terminate();
      }
    }

    private sealed class Node : RdBindableBase
    {
    }

    private sealed class StubWire : IWire
    {
      public bool IsStub => true;
      public void Send<TParam>(RdId id, TParam param, Action<TParam, UnsafeWriter> writer) { }
      public void Advise(Lifetime lifetime, IRdWireable entity) { }
      public ProtocolContexts Contexts { get; set; } = null!;
      public IRdWireable? TryGetById(RdId rdId) => null;
    }
  }
}
