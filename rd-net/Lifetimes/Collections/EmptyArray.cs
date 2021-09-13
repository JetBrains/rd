using System.Diagnostics;
using JetBrains.Annotations;

namespace JetBrains.Util
{
  /// <summary>
  /// Reuses the single instance of an empty array (one per type). If possible, prefer <code>EmptyList{T}.InstanceList</code>
  /// because each time you enumerate empty array, new <code>Array.SZArrayEnumerator</code> class instance is being created.
  /// </summary>
  [DebuggerDisplay("Length = 0")]
  public static class EmptyArray<T>
  {
    public static readonly T[] Instance = new T[0];
  }

  /// <summary>
  /// Reuses the single instance of an empty array (one per type).
  /// </summary>
  public static class EmptyArray
  {
    /// <summary>Synonym for <see cref="EmptyArray{T}.Instance"/></summary>
    [Pure]
    public static T[] GetInstance<T>()
    {
      return EmptyArray<T>.Instance;
    }
  }
}