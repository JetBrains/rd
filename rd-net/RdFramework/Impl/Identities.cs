using System;
using System.Threading;

namespace JetBrains.Rd.Impl
{
  /// <summary>
  /// Legacy implementation of <see cref="IIdentities"/> that mixes parent ID into dynamic IDs.
  /// This approach can cause ID collisions. Use <see cref="SequentialIdentities"/> instead.
  /// </summary>
  [Obsolete("Use SequentialIdentities instead")]
  public class Identities : IIdentities
  {
    private readonly IdKind Kind;
    private int myId;
    private const int BaseClientId = RdId.MaxStaticId;
    private const int BaseServerId = RdId.MaxStaticId + 1;

    public Identities(IdKind kind)
    {
      Kind = kind;
      myId = kind == IdKind.Client ? BaseClientId : BaseServerId;
    }

    public RdId Next(RdId parent)
    {
      return RdIdUtil.Mix(parent, Interlocked.Add(ref myId, 2));
    }
    
    public RdId Mix(RdId rdId, string tail)
    {
      return RdIdUtil.Mix(rdId, tail);
    }

    public RdId Mix(RdId rdId, int tail)
    {
      return RdIdUtil.Mix(rdId, tail);
    }

    public RdId Mix(RdId rdId, long tail)
    {
      return RdIdUtil.Mix(rdId, tail);
    }
  }

  /// <summary>
  /// Recommended implementation of <see cref="IIdentities"/> that avoids ID collisions.
  ///
  /// - Dynamic IDs (<see cref="Next"/>): Sequential integers that ignore the parent ID.
  ///   Client IDs are even, server IDs are odd, ensuring no overlap.
  /// - Stable IDs (<see cref="Mix(RdId,string)"/>): Hash-based with the high bit set (0x8000000000000000)
  ///   to ensure they never collide with dynamic IDs. The number of stable entities is small,
  ///   so hash collisions are unlikely.
  /// </summary>
  public class SequentialIdentities : IIdentities
  {
    private readonly IdKind Kind;
    private long myId;
    private const int BaseClientId = RdId.MaxStaticId;
    private const int BaseServerId = RdId.MaxStaticId + 1;

    /// <summary>High bit mask to distinguish stable IDs from dynamic IDs.</summary>
    private const long StableMask = 1L << 63;

    public SequentialIdentities(IdKind kind)
    {
      Kind = kind;
      myId = kind == IdKind.Client ? BaseClientId : BaseServerId;
    }

    public RdId Next(RdId parent)
    {
      // Ignore parent to avoid collisions from different creation order on client/server
      return new RdId(Interlocked.Add(ref myId, 2));
    }
    
    public RdId Mix(RdId rdId, string tail)
    {
      return new RdId(StableMask | RdIdUtil.Mix(rdId, tail).Value);
    }

    public RdId Mix(RdId rdId, int tail)
    {
      return new RdId(StableMask | RdIdUtil.Mix(rdId, tail).Value);
    }

    public RdId Mix(RdId rdId, long tail)
    {
      return new RdId(StableMask | RdIdUtil.Mix(rdId, tail).Value);
    }
  }
}