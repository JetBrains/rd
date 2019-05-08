using System.Threading;

namespace JetBrains.Rd.Impl
{
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
      return parent.Mix(Interlocked.Add(ref myId, 2));
    }
  }
}