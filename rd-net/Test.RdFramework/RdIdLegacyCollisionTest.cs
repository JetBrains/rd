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
  public class RdIdLegacyCollisionTest
  {
    private sealed class Leaf : RdBindableBase
    {
    }

    private sealed class CollisionModel : RdBindableBase
    {
      public readonly List<Leaf> Kids = new List<Leaf>();

      public CollisionModel(int childCount)
      {
        for (var k = 0; k < childCount; k++)
        {
          var leaf = new Leaf();
          Kids.Add(leaf);
          BindableChildren.Add(new KeyValuePair<string, object>("x" + k, leaf));
        }
      }
    }

    private sealed class StubWire : IWire
    {
      public bool IsStub => true;
      public void Send<TParam>(RdId id, TParam param, Action<TParam, UnsafeWriter> writer) { }
      public void Advise(Lifetime lifetime, IRdWireable entity) { }
      public ProtocolContexts Contexts { get; set; } = null!;
      public IRdWireable? TryGetById(RdId rdId) => null;
    }

    private static IIdentities Legacy()
    {
#pragma warning disable CS0618 // Type or member is obsolete
      return new Identities(IdKind.Server);
#pragma warning restore CS0618
    }

    private static IIdentities Sequential() => new SequentialIdentities(IdKind.Server);

    private static IEnumerable<TestCaseData> Cases()
    {
      yield return new TestCaseData((Func<IIdentities>)Legacy).SetName("legacy Identities (with the fix)");
      yield return new TestCaseData((Func<IIdentities>)Sequential).SetName("SequentialIdentities");
    }

    [TestCaseSource(nameof(Cases))]
    public void ChildRdIdsDoNotCollideAcrossParents(Func<IIdentities> identitiesFactory)
    {
      var ld = new LifetimeDefinition();
      var lifetime = ld.Lifetime;
      var protocol = new Protocol("Collision", new Serializers(), identitiesFactory(),
        SynchronousScheduler.Instance, new StubWire(), lifetime);

      var model1 = new CollisionModel(31);
      var model2 = new CollisionModel(1);

      using (AllowBindCookie.Create())
      {
        model1.Identify(protocol.Identities, new RdId(3), stable: false);
        model2.Identify(protocol.Identities, new RdId(1), stable: false);

        model1.PreBind(lifetime, protocol, "m1");
        model1.Bind();
        // Without the fix this is where model2.x0 duplicates model1.x0 and Register throws.
        model2.PreBind(lifetime, protocol, "m2");
        model2.Bind();
      }

      Assert.AreNotEqual(model1.Kids[0].RdId.Value, model2.Kids[0].RdId.Value,
        "named children of different parents must not collide");

      var all = new HashSet<long>();
      foreach (var leaf in model1.Kids) Assert.True(all.Add(leaf.RdId.Value), "duplicate id in model1");
      foreach (var leaf in model2.Kids) Assert.True(all.Add(leaf.RdId.Value), "duplicate id in model2");

      ld.Terminate();
    }
  }
}
