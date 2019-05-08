namespace JetBrains.Core
{
  /// <summary>
  /// Type that has the single instance. Adornment to <see cref="System.Void"/>.
  /// </summary>
  public class Unit
  {
    /// <summary>
    /// The only way to get instance of type <see cref="Unit"/>
    /// </summary>
    public static readonly Unit Instance = new Unit();
    
    private Unit() {}
  }
}