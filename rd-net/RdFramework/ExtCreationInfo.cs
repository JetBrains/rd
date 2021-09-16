using JetBrains.Diagnostics;

namespace JetBrains.Rd
{
  public struct ExtCreationInfo
  {
    public RName Name;
    public RdId? Id;
    public long Hash;

    public ExtCreationInfo(RName name, RdId? id, long hash)
    {
      Name = name;
      Id = id;
      Hash = hash;
    }
  }
}