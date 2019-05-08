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

    public static RdId Define<T>(int? id = null)
    {
      return new RdId(id ?? Hash(typeof(T).Name));
    }

    public static RdId Define(Type type, int? id = null)
    {
      return new RdId(id ?? Hash(type.Name));
    }

    public RdId(long value)
    {
      myValue = value;
    }

    [Pure]
    public RdId Mix(string tail)
    {
      return new RdId(Hash(tail, myValue));
    }

    [Pure]
    public RdId Mix(int tail)
    {
      return Mix((long) tail);
    }

    [Pure]
    public RdId Mix(long tail)
    {
      return new RdId(myValue * 31 + (tail + 1));
    }

    private static long Hash(string s, long initValue = 19)
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

    public bool IsNil => myValue == Nil.myValue;

    public long Value => myValue;

    public bool Equals(RdId other)
    {
      return myValue == other.myValue;
    }

    public override bool Equals(object obj)
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
      writer.Write(myValue);
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
}
