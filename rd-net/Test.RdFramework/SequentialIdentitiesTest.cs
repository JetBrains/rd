using JetBrains.Rd;
using JetBrains.Rd.Impl;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  public class SequentialIdentitiesTest
  {
    private const long HighBit = 1L << 63;

    [Test]
    public void TestNextHasNoHighBit()
    {
      var clientIdentities = new SequentialIdentities(IdKind.Client);
      var serverIdentities = new SequentialIdentities(IdKind.Server);

      for (int i = 0; i < 1000; i++)
      {
        var clientId = clientIdentities.Next(RdId.Nil);
        var serverId = serverIdentities.Next(RdId.Nil);

        Assert.That(clientId.Value & HighBit, Is.EqualTo(0L), $"Client dynamic ID should not have high bit set: {clientId.Value}");
        Assert.That(serverId.Value & HighBit, Is.EqualTo(0L), $"Server dynamic ID should not have high bit set: {serverId.Value}");
      }
    }

    [Test]
    public void TestMixAlwaysHasHighBit()
    {
      var clientIdentities = new SequentialIdentities(IdKind.Client);
      var serverIdentities = new SequentialIdentities(IdKind.Server);

      var testStrings = new[] { "", "a", "test", "Protocol", "Extension", "InternRoot" };
      
      foreach (var s in testStrings)
      {
        var clientId = clientIdentities.Mix(RdId.Nil, s);
        var serverId = serverIdentities.Mix(RdId.Nil, s);
        Assert.That(clientId.Value & HighBit, Is.Not.EqualTo(0L), $"Client stable ID from string '{s}' should have high bit set");
        Assert.That(serverId.Value & HighBit, Is.Not.EqualTo(0L), $"Server stable ID from string '{s}' should have high bit set");
      }
    }

    [Test]
    public void TestNoOverlapBetweenNextAndMix()
    {
      var identities = new SequentialIdentities(IdKind.Client);

      var dynamicIds = new System.Collections.Generic.HashSet<long>();
      var stableIds = new System.Collections.Generic.HashSet<long>();

      for (int i = 0; i < 1000; i++)
      {
        dynamicIds.Add(identities.Next(RdId.Nil).Value);
      }

      for (int i = 0; i < 1000; i++)
      {
        stableIds.Add(identities.Mix(RdId.Nil, $"key{i}").Value);
      }

      foreach (var id in dynamicIds)
      {
        Assert.That(stableIds.Contains(id), Is.False, $"Dynamic and stable IDs should never overlap, but found: {id}");
      }
    }

    [Test]
    public void TestNextIgnoresParent()
    {
      var identities1 = new SequentialIdentities(IdKind.Client);
      var identities2 = new SequentialIdentities(IdKind.Client);

      var differentParents = new[] { RdId.Nil, new RdId(1), new RdId(12345), new RdId(long.MaxValue), new RdId(-1) };

      // Get IDs using different parents from identities1
      var idsWithDifferentParents = new System.Collections.Generic.List<long>();
      foreach (var parent in differentParents)
      {
        idsWithDifferentParents.Add(identities1.Next(parent).Value);
      }

      // Get IDs using same parent from identities2
      var idsWithSameParent = new System.Collections.Generic.List<long>();
      foreach (var _ in differentParents)
      {
        idsWithSameParent.Add(identities2.Next(RdId.Nil).Value);
      }

      // Both should produce the same sequence since parent is ignored
      Assert.That(idsWithDifferentParents, Is.EqualTo(idsWithSameParent),
        "Next() should return sequential IDs regardless of parent");

      // Verify they are sequential (incrementing by 2)
      for (int i = 1; i < idsWithDifferentParents.Count; i++)
      {
        Assert.That(idsWithDifferentParents[i] - idsWithDifferentParents[i - 1], Is.EqualTo(2L),
          "Next() should increment by 2");
      }
    }
  }
}
