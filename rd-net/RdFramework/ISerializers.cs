using System;
using JetBrains.Annotations;
using JetBrains.Serialization;

namespace JetBrains.Rd
{
  public interface ISerializersContainer
  {
    void Register<T>(CtxReadDelegate<T> reader, CtxWriteDelegate<T> writer, long? predefinedType = null);

    void RegisterEnum<T>() where T :
    unmanaged, 
     Enum;

    void RegisterToplevelOnce(Type toplevelType, Action<ISerializers> registerDeclaredTypesSerializers);
  }

  public interface ISerializers : ISerializersContainer
  {
    T? Read<T>(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<T>? unknownInstanceReader = null);

    void Write<T>(SerializationCtx ctx, UnsafeWriter writer, T value);
  }
}