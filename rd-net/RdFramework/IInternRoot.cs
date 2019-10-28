using System;
using JetBrains.Rd.Base;
using JetBrains.Serialization;

namespace JetBrains.Rd
{
  /// <summary>
  /// Interns values sent over protocol
  /// </summary>
  public interface IInternRoot : IRdReactive
  {
    /// <summary>
    /// Tries to get an ID for a value. Doesn't intern it if it's not interned.
    /// </summary>
    bool TryGetInterned(object value, out InterningId result);
    
    /// <summary>
    /// Interns a value and returns an ID for it. May return invalid ID in case of multithreaded contention.
    /// </summary>
    InterningId Intern(object value);
    
    /// <summary>
    /// Gets a value from an interned ID. Throws an exception if the ID doesn't correspond to a value
    /// </summary>
    T UnIntern<T>(InterningId id);
    
    /// <summary>
    /// Gets a value from an interned ID. Returns true if successful, false otherwise
    /// </summary>
    bool TryUnIntern<T>(InterningId id, out T result);
    
    /// <summary>
    /// Removes an interned value. Any future attempts to un-intern IDs previously associated with this value will fail.
    /// Not thread-safe. It's up to user to ensure that the value being removed is not being used in messages written on background threads.
    /// </summary>
    void Remove(object value);
  }
  
  public readonly struct InterningId : IEquatable<InterningId>
  {
    public readonly int Value;

    public InterningId(int value)
    {
      Value = value;
    }

    public bool IsValid => Value != -1;
    public bool IsLocal => (Value & 1) == 0;
    
    public static InterningId Invalid = new InterningId(-1);

    public static InterningId Read(UnsafeReader reader)
    {
      return new InterningId(reader.ReadInt());
    }

    public static void Write(UnsafeWriter writer, InterningId value)
    {
      writer.Write(value.Value == -1 ? value.Value : value.Value ^ 1);
    }

    public bool Equals(InterningId other)
    {
      return Value == other.Value;
    }

    public override bool Equals(object obj)
    {
      return obj is InterningId other && Equals(other);
    }

    public override int GetHashCode()
    {
      return Value;
    }

    public static bool operator ==(InterningId left, InterningId right)
    {
      return left.Equals(right);
    }

    public static bool operator !=(InterningId left, InterningId right)
    {
      return !left.Equals(right);
    }
  }
}