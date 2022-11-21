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
  /// <param name="serializers"></param>
  /// <returns></returns>
  SerializerPair CreateSerializer(Type type, ISerializersSource serializers);
}