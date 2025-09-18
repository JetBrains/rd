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
      CtxReadDelegate<List<T>?> readListSerializer =
        (ctx, reader) => reader.ReadList(itemSerializer.GetReader<T>(), ctx);

      CtxWriteDelegate<IEnumerable<T>> writeListSerializer =
        (ctx, writer, value) => writer.WriteEnumerable(itemSerializer.GetWriter<T>(), ctx, value);

      return new SerializerPair(readListSerializer, writeListSerializer);
    }

    public static SerializerPair CreateDictionarySerializerPair<TKey, TValue>(
      SerializerPair keySerializer, SerializerPair valueSerializer)
    {
      var read = CreateReadDictionary<TKey, TValue>(keySerializer, valueSerializer);

      CtxWriteDelegate<IDictionary<TKey, TValue>?> write = (ctx, writer, value) =>
      {
        if (value is Dictionary<TKey, TValue> val && !Equals(val.Comparer, EqualityComparer<TKey>.Default))
          throw new Exception($"Unable to serialize {value.GetType().ToString(true)}. Custom equality comparers are not supported");

        if (value == null)
        {
          writer.WriteInt32(-1);
          return;
        }

        writer.WriteInt32(value.Count);

        var keyWriter = keySerializer.GetWriter<TKey>();
        var valueWriter = valueSerializer.GetWriter<TValue>();

        foreach (var kvp in value)
        {
          keyWriter(ctx, writer, kvp.Key);
          valueWriter(ctx, writer, kvp.Value);
        }
      };

      return new SerializerPair(read, write);
    }

    public static SerializerPair CreateReadOnlyDictionarySerializerPair<TKey, TValue>(
      SerializerPair keySerializer, SerializerPair valueSerializer)
    {
      var read = CreateReadDictionary<TKey, TValue>(keySerializer, valueSerializer);

      CtxWriteDelegate<IReadOnlyDictionary<TKey, TValue>?> write = (context, writer, value) =>
      {
        if (value is Dictionary<TKey, TValue> val && !Equals(val.Comparer, EqualityComparer<TKey>.Default))
          throw new Exception($"Unable to serialize {value.GetType().ToString(true)}. Custom equality comparers are not supported");

        if (value == null)
        {
          writer.WriteInt32(-1);
          return;
        }

        writer.WriteInt32(value.Count);

        var keyWriter = keySerializer.GetWriter<TKey>();
        var valueWriter = valueSerializer.GetWriter<TValue>();

        foreach (var kvp in value)
        {
          keyWriter(context, writer, kvp.Key);
          valueWriter(context, writer, kvp.Value);
        }
      };

      return new SerializerPair(read, write);
    }

    private static CtxReadDelegate<Dictionary<TKey, TValue>?> CreateReadDictionary<TKey, TValue>(
      SerializerPair keySerializer, SerializerPair valueSerializer)
    {
      CtxReadDelegate<Dictionary<TKey, TValue>?> read = (context, reader) =>
      {
        int count = reader.ReadInt();
        if (count == -1)
          return null;

        var result = new Dictionary<TKey, TValue>(count);
        var keyReader = keySerializer.GetReader<TKey>();
        var valueReader = valueSerializer.GetReader<TValue>();

        for (var index = 0; index < count; index++)
        {
          var key = keyReader(context, reader);
          var value = valueReader(context, reader);
          result.Add(key, value);
        }

        return result;
      };

      return read;
    }
  }
}