using System.Collections.Generic;

namespace JetBrains.Rd.Text.Intrinsics
{
  public struct TextBufferVersion
  {
    public static TextBufferVersion InitVersion = new TextBufferVersion(-1, -1);

    public TextBufferVersion(int master, int slave) : this()
    {
      Master = master;
      Slave = slave;
    }

    public int Master { get; }

    public int Slave { get; }

    #region Overrides

    public bool Equals(TextBufferVersion other)
    {
      return Master == other.Master && Slave == other.Slave;
    }

    public static bool operator ==(TextBufferVersion left, TextBufferVersion right)
    {
      return left.Equals(right);
    }

    public static bool operator !=(TextBufferVersion left, TextBufferVersion right)
    {
      return !left.Equals(right);
    }

    public override bool Equals(object? obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      return obj is TextBufferVersion && Equals((TextBufferVersion) obj);
    }

    public override int GetHashCode()
    {
      unchecked
      {
        return (Master*397) ^ Slave;
      }
    }

    #endregion

    #region Comparer

    private static readonly IEqualityComparer<TextBufferVersion> ourMasterVersionSlaveVersionComparerInstance =
      new MasterVersionSlaveVersionEqualityComparer();

    public static IEqualityComparer<TextBufferVersion> MasterVersionSlaveVersionComparer
    {
      get { return ourMasterVersionSlaveVersionComparerInstance; }
    }

    private sealed class MasterVersionSlaveVersionEqualityComparer : IEqualityComparer<TextBufferVersion>
    {
      public bool Equals(TextBufferVersion x, TextBufferVersion y)
      {
        return x.Master == y.Master && x.Slave == y.Slave;
      }

      public int GetHashCode(TextBufferVersion obj)
      {
        unchecked
        {
          return (obj.Master*397) ^ obj.Slave;
        }
      }
    }

    #endregion

    #region Utils

    public TextBufferVersion IncrementMaster()
    {
      return new TextBufferVersion(Master + 1, Slave);
    }

    public TextBufferVersion IncrementSlave()
    {
      return new TextBufferVersion(Master, Slave + 1);
    }

    #endregion

    public override string ToString()
    {
      return $"TextBufferVersion(master={Master}, slave={Slave})";
    }
  }
}