using JetBrains.Rd.Base;

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
    bool TryGetInterned(object value, out int result);
    
    /// <summary>
    /// Interns a value and returns an ID for it
    /// </summary>
    int Intern(object value);
    
    /// <summary>
    /// Gets a value from an interned ID. Throws an exception if the ID doesn't correspond to a value
    /// </summary>
    T UnIntern<T>(int id);
    
    /// <summary>
    /// Removes an interned value. Any future attempts to un-intern IDs previously associated with this value will fail.
    /// Not thread-safe. It's up to user to ensure that the value being removed is not being used in messages written on background threads.
    /// </summary>
    void Remove(object value);
  }
  
}