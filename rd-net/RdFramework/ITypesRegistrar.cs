using System;
using JetBrains.Annotations;

namespace JetBrains.Rd
{
  public interface ITypesRegistrar
  {
    /// <summary>
    /// Tries to discovery type with specific <see cref="RdId"/>.
    /// If it succeeds then should Invoke <see cref="ISerializers.Register{T}"/> method on provided serializers.
    /// </summary>
    /// <param name="id"></param>
    /// <param name="serializers"></param>
    void TryRegister(RdId id, ISerializers serializers);
    void TryRegister(Type clrType, ISerializers serializers);
  }
}