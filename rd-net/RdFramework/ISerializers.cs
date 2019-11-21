using System;
using JetBrains.Annotations;
using JetBrains.Serialization;

namespace JetBrains.Rd
{
  public interface ISerializersContainer
  {
    void Register<T>([NotNull] CtxReadDelegate<T> reader, [NotNull] CtxWriteDelegate<T> writer, long? predefinedType = null);

    void RegisterEnum<T>() where T :
#if !NET35
    unmanaged, 
#endif
     Enum;

    void RegisterToplevelOnce(Type toplevelType, Action<ISerializers> registerDeclaredTypesSerializers);
  }

  public interface ISerializers : ISerializersContainer
  {
    T Read<T>(SerializationCtx ctx, [NotNull] UnsafeReader reader, [CanBeNull] CtxReadDelegate<T> unknownInstanceReader = null);

    void Write<T>(SerializationCtx ctx, [NotNull] UnsafeWriter writer, [CanBeNull] T value);
  }
}