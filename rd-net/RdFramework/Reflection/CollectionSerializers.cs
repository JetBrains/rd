using System;
using System.Collections.Generic;
using JetBrains.Rd.Impl;
using JetBrains.Util.Util;

namespace JetBrains.Rd.Reflection
{
  /// <summary>
  /// Special asymmetric serializers for collections. Used in reflection serializers to use covariant conversion instead of runtime casting.
  /// </summary>
  internal class CollectionSerializers
  {
    public static SerializerPair CreateListSerializerPair<T>(SerializerPair itemSerializer)
    {
      CtxReadDelegate<List<T>> readListSerializer = (ctx, reader) => reader.ReadList(itemSerializer.GetReader<T>(), ctx);
      CtxWriteDelegate<ICollection<T>> writeListSerializer =(ctx, writer, value) => writer.WriteCollection(itemSerializer.GetWriter<T>(), ctx, value);
      return new SerializerPair(readListSerializer, writeListSerializer);
    }

    public static SerializerPair CreateDictionarySerializerPair<TKey, TValue>(SerializerPair keySerializer, SerializerPair valueSerializer)
    {
      var read = CreateReadDictionary<TKey, TValue>(keySerializer, valueSerializer);
      CtxWriteDelegate<IDictionary<TKey, TValue>> write = (ctx, writer, value) =>
      {
        if (value is Dictionary<TKey, TValue> val && !Equals(val.Comparer, EqualityComparer<TKey>.Default))
          throw new Exception($"Unable to serialize {value.GetType().ToString(true)}. Custom equality comparers are not supported");
        if (value == null)
        {
          writer.Write(-1);
          return;
        }
        writer.Write(value.Count);
        var keyw = keySerializer.GetWriter<TKey>();
        var valuew = valueSerializer.GetWriter<TValue>();
        foreach (var kvp in value)
        {
          keyw(ctx, writer, kvp.Key);
          valuew(ctx, writer, kvp.Value);
        }
      };
      return new SerializerPair(read, write);
    }

    public static SerializerPair CreateReadOnlyDictionarySerializerPair<TKey, TValue>(SerializerPair keySerializer, SerializerPair valueSerializer)
    {
#if NET35
      throw new NotSupportedException();
#else
      var read = CreateReadDictionary<TKey, TValue>(keySerializer, valueSerializer);
      CtxWriteDelegate<IReadOnlyDictionary<TKey, TValue>> write = (ctx, writer, value) =>
      {
        if (value is Dictionary<TKey, TValue> val && !Equals(val.Comparer, EqualityComparer<TKey>.Default))
          throw new Exception($"Unable to serialize {value.GetType().ToString(true)}. Custom equality comparers are not supported");
        if (value == null)
        {
          writer.Write(-1);
          return;
        }
        writer.Write(value.Count);
        var keyw = keySerializer.GetWriter<TKey>();
        var valuew = valueSerializer.GetWriter<TValue>();
        foreach (var kvp in value)
        {
          keyw(ctx, writer, kvp.Key);
          valuew(ctx, writer, kvp.Value);
        }
      };
      return new SerializerPair(read, write);
#endif
    }


    private static CtxReadDelegate<Dictionary<TKey, TValue>> CreateReadDictionary<TKey, TValue>(SerializerPair keySerializer, SerializerPair valueSerializer)
    {
      CtxReadDelegate<Dictionary<TKey, TValue>> read = (ctx, reader) =>
      {
        int count = reader.ReadInt();
        if (count == -1)
          return null;
        var result = new Dictionary<TKey, TValue>(count);
        var keyr = keySerializer.GetReader<TKey>();
        var valuer = valueSerializer.GetReader<TValue>();
        for (int i = 0; i < count; i++)
        {
          var key = keyr(ctx, reader);
          var value = valuer(ctx, reader);
          result.Add(key, value);
        }

        return result;
      };
      return read;
    }
  }
}