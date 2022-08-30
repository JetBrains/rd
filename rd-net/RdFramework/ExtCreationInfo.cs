using JetBrains.Diagnostics;
using JetBrains.Rd.Base;

namespace JetBrains.Rd
{
  public struct ExtCreationInfo
  {
    public RName Name;
    public RdId? Id;
    public long Hash;
    public RdExtBase? Ext;

    public ExtCreationInfo(RName name, RdId? id, long hash, RdExtBase? ext)
    {
      Name = name;
      Id = id;
      Hash = hash;
      Ext = ext;
    }
  }
}