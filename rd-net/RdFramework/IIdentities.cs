using System;

namespace JetBrains.Rd
{
  /// <summary>
  /// Generates unique identifiers for objects in an object graph.
  ///
  /// Two types of IDs are supported:
  /// - Dynamic IDs: Generated for entities created at runtime (via <see cref="Next"/>)
  /// - Stable IDs: Hash-based IDs for statically known entities like extensions (via <see cref="Mix(RdId,string)"/>)
  /// </summary>
  public interface IIdentities
  {
    /// <summary>
    /// Generates the next unique dynamic identifier for a runtime-created entity.
    /// </summary>
    RdId Next(RdId parent);

    /// <summary>
    /// Creates a stable identifier by mixing the parent ID with a string key.
    /// </summary>
    RdId Mix(RdId rdId, string tail);

    /// <summary>
    /// Creates a stable identifier by mixing the parent ID with an integer key.
    /// </summary>
    RdId Mix(RdId rdId, int tail);

    /// <summary>
    /// Creates a stable identifier by mixing the parent ID with a long key.
    /// </summary>
    RdId Mix(RdId rdId, long tail);
  }
}