using System;
using JetBrains.Rd.Impl;

namespace JetBrains.Rd.Reflection;

public interface IScalarSerializers
{
  /// <summary>
  /// Return static serializers for type Static serializer is the serializer which always return provided type and
  /// never any of the inheritors. It makes sense to ask these serializers for struct, enums and sealed classes
  /// </summary>
  /// <param name="type"></param>
  /// <returns></returns>
  SerializerPair GetOrCreate(Type type);

  /// <summary>
  /// Return instance serializers for the type.
  /// Instance means that Polymorphic types is possible, you can ask here for serializer for interface, for example
  /// </summary>
  /// <param name="t"></param>
  /// <returns></returns>
  SerializerPair GetInstanceSerializer(Type t);

  /// <summary>
  /// Register custom serializer for provided polymorphic type. It will be used instead of default <see
  /// cref="Polymorphic{T}"/>. Be aware, that you can register your custom serializer only before any serializer was
  /// asked via <see cref="GetInstanceSerializer"/>.
  /// </summary>
  /// <param name="type"></param>
  /// <param name="serializers"></param>
  void RegisterPolymorphicSerializer(Type type, SerializerPair serializers);

  void GetOrCreate<T>(out CtxReadDelegate<T> reader, out CtxWriteDelegate<T> writer);

  bool CanBePolymorphic(Type type);
}