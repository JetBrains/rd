using System;
using JetBrains.Rd.Base;
using JetBrains.Serialization;
#nullable disable

namespace JetBrains.Rd
{
  /// <summary>
  /// Interns values sent over protocol
  /// </summary>
  public interface IInternRoot<TBase> : IRdReactive
  {
    /// <summary>
    /// Tries to get an ID for a value. Doesn't intern it if it's not interned.
    /// </summary>
    InternId TryGetInterned(TBase value);
    
    /// <summary>
    /// Interns a value and returns an ID for it. May return invalid ID in case of multithreaded contention.
    /// </summary>
    InternId Intern(TBase value);
    
    /// <summary>
    /// Gets a value from an interned ID. Throws an exception if the ID doesn't correspond to a value
    /// </summary>
    T UnIntern<T>(InternId id) where T : TBase;
    
    /// <summary>
    /// Gets a value from an interned ID. Returns true if successful, false otherwise
    /// </summary>
    bool TryUnIntern<T>(InternId id, out T result) where T : TBase;
    
    /// <summary>
    /// Removes an interned value. Any future attempts to un-intern IDs previously associated with this value will fail.
    /// Not thread-safe. It's up to user to ensure that the value being removed is not being used in messages written on background threads.
    /// </summary>
    void Remove(TBase value);
  }
  
  /// <summary>
  /// An ID representing an interned value
  /// </summary>
  public readonly struct InternId : IEquatable<InternId>
  {
    private const int InvalidId = -1;
    private readonly int myValue;

    internal InternId(int value)
    {
      myValue = value;
    }

    /// <summary>
    /// True if this ID represents an actual interned value. False indicates a failed interning operation or unset value
    /// </summary>
    public bool IsValid => myValue != InvalidId;
    
    /// <summary>
    /// True if this ID represents a value interned by local InternRoot 
    /// </summary>
    public bool IsLocal => (myValue & 1) == 0;
    
    public static InternId Invalid = new InternId(InvalidId);

    public static InternId Read(UnsafeReader reader) => new InternId(reader.ReadInt());

    public static void Write(UnsafeWriter writer, InternId value)
    {
      writer.WriteInt32(value.myValue == InvalidId ? value.myValue : value.myValue ^ 1);
    }

    public bool Equals(InternId other) => myValue == other.myValue;

    public override bool Equals(object obj) => obj is InternId other && Equals(other);

    public override int GetHashCode() => myValue;

    public static bool operator ==(InternId left, InternId right) => left.Equals(right);

    public static bool operator !=(InternId left, InternId right) => !left.Equals(right);

    public override string ToString() => $"InternId({myValue})";
  }
}