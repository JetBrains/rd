using System.Collections.Generic;
using JetBrains.Rd.Impl;

namespace JetBrains.Rd.Reflection
{
  /// <summary>
  /// Special asymmetric serializers for collections. Used in reflection serializers to use covariant conversion instead of runtime casting.
  /// </summary>
  internal class CollectionSerializers
  {
    public static SerializerPair CreateListSerializerPair<T>(SerializerPair itemSerializer)
    {
      CtxReadDelegate<IList<T>> readListSerializer = (ctx, reader) => reader.ReadList(itemSerializer.GetReader<T>(), ctx);
      CtxWriteDelegate<ICollection<T>> writeListSerializer =(ctx, writer, value) => writer.WriteCollection(itemSerializer.GetWriter<T>(), ctx, value);
      return new SerializerPair(readListSerializer, writeListSerializer);
    }
  }
}