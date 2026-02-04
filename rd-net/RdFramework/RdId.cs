using System;
using JetBrains.Annotations;
using JetBrains.Serialization;

namespace JetBrains.Rd
{
  public struct RdId : IEquatable<RdId>
  {
    public static readonly RdId Nil = new RdId(0);
    public static readonly RdId Root = Nil;
    
    public static readonly RdId TestValue = new RdId(1);
    public const int MaxStaticId = 1000000;

    private readonly long myValue;

    public RdId(long value)
    {
      myValue = value;
    }

    public bool IsNil => myValue == Nil.myValue;

    public long Value => myValue;

    public bool Equals(RdId other)
    {
      return myValue == other.myValue;
    }

    public override bool Equals(object? obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      return obj is RdId && Equals((RdId) obj);
    }

    public override int GetHashCode()
    {
      unchecked
      {
        return (int) myValue;
      }
    }

    public override string ToString()
    {
      return $"{(ulong)myValue}";
    }

    public void Write(UnsafeWriter writer)
    {
      writer.WriteInt64(myValue);
    }

    public static RdId Read(UnsafeReader reader)
    {
      return new RdId(reader.ReadInt64());
    }

    public static void Write(UnsafeWriter writer, RdId value)
    {
      value.Write(writer);
    }

    public static bool operator ==(RdId left, RdId right)
    {
      return left.Equals(right);
    }

    public static bool operator !=(RdId left, RdId right)
    {
      return !left.Equals(right);
    }
  }

  public enum IdKind
  {
    Client,
    Server
  }

  public static class RdIdUtil
  {
    public static RdId Define<T>(long? id = null)
    {
      return new RdId(id ?? Hash(typeof(T).Name));
    }

    public static RdId Define(Type type, long? id = null)
    {
      return new RdId(id ?? Hash(type.Name));
    }

    /// <summary>
    /// Define an RdId by fully-qualified type name.
    /// You should use it only in case of C#-C# communication.
    /// </summary>
    public static RdId DefineByFqn(Type type)
    {
      return new RdId(Hash(type.FullName));
    }

    [Pure]
    public static RdId Mix(RdId rdId, string tail)
    {
      return new RdId(Hash(tail, rdId.Value));
    }

    [Pure]
    public static RdId Mix(RdId rdId, int tail)
    {
      return Mix(rdId, (long) tail);
    }

    [Pure]
    public static RdId Mix(RdId rdId, long tail)
    {
      return new RdId(rdId.Value * 31 + (tail + 1));
    }

    public static long Hash(string? s, long initValue = 19)
    {
      if (s == null) return 0;

      long hash = initValue;
// ReSharper disable LoopCanBeConvertedToQuery 
// ReSharper disable ForCanBeConvertedToForeach
      for (int i = 0; i < s.Length; i++)
      {
        hash = hash*31 + s[i];
      }
// ReSharper restore ForCanBeConvertedToForeach
// ReSharper restore LoopCanBeConvertedToQuery

      return hash;
    }
  }
}
