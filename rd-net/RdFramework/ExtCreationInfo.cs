using JetBrains.Diagnostics;
using JetBrains.Rd.Base;

namespace JetBrains.Rd
{
  public readonly struct ExtCreationInfo
  {
    public readonly RName Name;
    public readonly RdId? Id;
    public readonly long Hash;
    public readonly RdExtBase? Ext;

    public ExtCreationInfo(RName name, RdId? id, long hash, RdExtBase? ext)
    {
      Name = name;
      Id = id;
      Hash = hash;
      Ext = ext;
    }
  }

  public readonly struct ExtCreationInfoEx
  {
    public readonly ExtCreationInfo Info;
    public readonly bool IsLocal;

    public ExtCreationInfoEx(ExtCreationInfo info, bool isLocal)
    {
      Info = info;
      IsLocal = isLocal;
    }
  }
}