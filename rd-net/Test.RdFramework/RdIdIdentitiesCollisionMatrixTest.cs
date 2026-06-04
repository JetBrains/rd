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
  public class RdIdIdentitiesCollisionMatrixTest
  {
    public enum Derivation { Dynamic, Stable }

    private static IIdentities Legacy()
    {
#pragma warning disable CS0618 // Type or member is obsolete
      return new Identities(IdKind.Server);
#pragma warning restore CS0618
    }

    private static IIdentities Sequential() => new SequentialIdentities(IdKind.Server);

    private static IEnumerable<TestCaseData> Cases()
    {
      yield return new TestCaseData((Func<IIdentities>)Legacy, Derivation.Dynamic, true)
        .SetName("legacy + dynamic Next COLLIDES even with the fix");
      yield return new TestCaseData((Func<IIdentities>)Legacy, Derivation.Stable, false)
        .SetName("legacy + stable Mix does not collide");
      yield return new TestCaseData((Func<IIdentities>)Sequential, Derivation.Dynamic, false)
        .SetName("SequentialIdentities + dynamic Next does not collide");
      yield return new TestCaseData((Func<IIdentities>)Sequential, Derivation.Stable, false)
        .SetName("SequentialIdentities + stable Mix does not collide");
    }

    [TestCaseSource(nameof(Cases))]
    public void Collision(Func<IIdentities> identitiesFactory, Derivation mode, bool expectedToCollide)
    {
      var ld = new LifetimeDefinition();
      try
      {
        var lifetime = ld.Lifetime;
        var protocol = new Protocol("MatrixCol", new Serializers(), identitiesFactory(),
          SynchronousScheduler.Instance, new StubWire(), lifetime);
        var identities = protocol.Identities;

        // p1 (id 3) is identified/filled before p2 (id 1) — reverse id order is what lets legacy collide.
        var p1 = new Node();
        var p2 = new Node();
        p1.Identify(identities, new RdId(3), stable: false);
        p2.Identify(identities, new RdId(1), stable: false);

        using (AllowBindCookie.Create())
        {
          p1.PreBind(lifetime, protocol, "p1");
          p1.Bind();
          p2.PreBind(lifetime, protocol, "p2");
          p2.Bind();

          TestDelegate fillTree = () =>
          {
            AddChildren(identities, p1, mode, 31, lifetime);
            AddChildren(identities, p2, mode, 1, lifetime);
          };

          if (expectedToCollide)
            Assert.Throws<ArgumentException>(fillTree, "expected a duplicate-RdId registration to throw");
          else
            Assert.DoesNotThrow(fillTree, "ids must stay unique");
        }
      }
      finally
      {
        ld.Terminate();
      }
    }

    private static void AddChildren(IIdentities identities, Node parent, Derivation mode, int count, Lifetime lifetime)
    {
      for (var i = 0; i < count; i++)
      {
        var name = "x" + i;
        var childId = mode == Derivation.Dynamic
          ? identities.Next(parent.RdId)             // dynamic (RdMap/RdList element) id
          : identities.Mix(parent.RdId, "." + name); // stable (structural child) id
        var child = new Node();
        child.Identify(identities, childId, stable: false);
        child.PreBind(lifetime, parent, name);
        child.Bind();
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
