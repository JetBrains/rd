using System;

namespace JetBrains.Core
{
  /// <summary>
  /// Type that has the single instance accessible by <see cref="Instance"/>. Adornment to <see cref="System.Void"/>.
  /// </summary>
  public class Unit : IEquatable<Unit>
  {
    /// <summary>
    /// The only way to get instance of type <see cref="Unit"/>
    /// </summary>
    public static readonly Unit Instance = new Unit();
    
    private Unit() {}

    public override string ToString() => "<unit>";

    public bool Equals(Unit other) => true;

    public override bool Equals(object? obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      return obj.GetType() == GetType() && Equals((Unit) obj);
    }

    public override int GetHashCode() => 0; 
    

    public static bool operator ==(Unit? left, Unit? right)
    {
      return Equals(left, right);
    }

    public static bool operator !=(Unit? left, Unit? right)
    {
      return !Equals(left, right);
    }
  }
}