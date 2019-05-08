using System;
using JetBrains.Annotations;

namespace JetBrains.Rd.Reflection
{
  public interface IPolymorphicTypesCatalog
  {
    /// <summary>
    /// Tries to discovery type with specific <see cref="RdId"/>.
    /// If it succeeds then should Invoke <see cref="ISerializers.Register{T}"/> method on provided serializers.
    /// </summary>
    /// <param name="id"></param>
    /// <param name="serializers"></param>
    void TryDiscoverRegister(RdId id, [NotNull] ISerializers serializers);
    void TryDiscoverRegister([NotNull] Type clrType, [NotNull] ISerializers serializers);

    void AddType([NotNull] Type type);
  }
}