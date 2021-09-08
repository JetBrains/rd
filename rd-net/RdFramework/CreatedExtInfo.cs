using JetBrains.Diagnostics;

namespace JetBrains.Rd
{
  public struct CreatedExtInfo
  {
    public RName Name;
    public RdId? Id;
    public long Hash;

    public CreatedExtInfo(RName name, RdId? id, long hash)
    {
      Name = name;
      Id = id;
      Hash = hash;
    }
  }
}