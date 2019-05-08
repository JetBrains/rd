using System;
using JetBrains.Annotations;

namespace JetBrains.Rd.Impl
{
  /// <summary>
  /// This is a temporary stub for proper secure strings in protocol
  /// Unlike a normal string, this one won't be stored in logs or any other string representations of protocol entities
  /// </summary>
  public struct RdSecureString : IEquatable<RdSecureString>
  {
    [NotNull]
    public readonly string Contents;

    public RdSecureString([NotNull] string contents)
    {
      Contents = contents;
    }

    public override string ToString()
    {
      return "RdSecureString";
    }

    public bool Equals(RdSecureString other)
    {
      return string.Equals(Contents, other.Contents);
    }

    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      return obj is RdSecureString && Equals((RdSecureString) obj);
    }

    public override int GetHashCode()
    {
      return Contents.GetHashCode();
    }

    public static bool operator ==(RdSecureString left, RdSecureString right)
    {
      return left.Equals(right);
    }

    public static bool operator !=(RdSecureString left, RdSecureString right)
    {
      return !left.Equals(right);
    }
  }
}