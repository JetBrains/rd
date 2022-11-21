using System;

namespace JetBrains.Rd.Reflection;

/// <summary>
/// The source of "real" serializers serializers. Polyporhic serializers always write <see cref="RdId"/> to the stream and only after it serializes the type itself.
/// </summary>
public interface ISerializersSource
{
  SerializerPair GetOrRegisterSerializerPair(Type type, bool instance = false);

  SerializerPair GetPolymorphic<T>();
  SerializerPair GetPolymorphic(Type type);
}