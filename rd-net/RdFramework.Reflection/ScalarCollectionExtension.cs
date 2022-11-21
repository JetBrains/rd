using System;
using System.Collections.Generic;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;
using JetBrains.Util;

namespace JetBrains.Rd.Reflection;

/// <summary>
/// An extension for <see cref="ReflectionSerializers"/> for basic collections and dictionaries
/// </summary>
public static class ScalarCollectionExtension
{
  public static ReflectionSerializers WithBasicCollectionSerializers(this ReflectionSerializers self)
  {
    self.BeforeCreation.Advise(Lifetime.Eternal, type =>
    {
      if (type.IsGenericType)
      {
        if (IsList(type))
        {
          var genericTypeArgument = type.GetGenericArguments()[0];
          var argumentTypeSerializerPair = self.GetOrRegisterSerializerPair(genericTypeArgument, true);
          var result = (SerializerPair)ReflectionUtil.InvokeStaticGeneric(typeof(CollectionSerializers), nameof(CollectionSerializers.CreateListSerializerPair), genericTypeArgument, argumentTypeSerializerPair)!;
          self.Register(type, result);
        }
        else if (IsDictionary(type) || IsReadOnlyDictionary(type))
        {
          var typeArguments = type.GetGenericArguments();
          var tkey = typeArguments[0];
          var tvalue = typeArguments[1];
          var keySerializer = self.GetOrRegisterSerializerPair(tkey, true);
          var valueSerializer = self.GetOrRegisterSerializerPair(tvalue, true);
          var serializersFactoryName = IsReadOnlyDictionary(type) ? nameof(CollectionSerializers.CreateReadOnlyDictionarySerializerPair) : nameof(CollectionSerializers.CreateDictionarySerializerPair);
          var result = (SerializerPair)ReflectionUtil.InvokeStaticGeneric2(typeof(CollectionSerializers), serializersFactoryName, tkey, tvalue, keySerializer, valueSerializer)!;
          self.Register(type, result);
        }
      }
      else if (type.IsArray)
      {
        var result = (SerializerPair)ReflectionUtil.InvokeStaticGeneric(typeof(ScalarCollectionExtension), nameof(CreateArraySerializer), type.GetElementType(), new object[] { self })!;
        self.Register(type, result);
      }
    });
    return self;
  }

  private static SerializerPair CreateArraySerializer<T>(ISerializersSource serializersSource)
  {
    var serializers = serializersSource.GetOrRegisterSerializerPair(typeof(T), true);
    var itemReader = serializers.GetReader<T>();
    var itemWriter = serializers.GetWriter<T>();

    CtxReadDelegate<T[]?> reader = (ctx, unsafeReader) => unsafeReader.ReadArray(itemReader, ctx);
    CtxWriteDelegate<T[]> writer = (ctx, unsafeWriter, value) => unsafeWriter.WriteArray(itemWriter, ctx, value);
    return new SerializerPair(reader, writer);
  }


  public static bool IsList(Type t)
  {
    return t.IsGenericType && t.GetGenericTypeDefinition() is var generic && (
      generic == typeof(List<>)
      || generic == typeof(IList<>)
      || generic == typeof(ICollection<>)
      || generic == typeof(IEnumerable<>)
#if !NET35
      || generic == typeof(IReadOnlyList<>)
#endif
    );
  }

  public static bool IsDictionary(Type t)
  {
    return t.IsGenericType && t.GetGenericTypeDefinition() is var generic &&
           (generic == typeof(Dictionary<,>) ||
            generic == typeof(IDictionary<,>)
           );
  }

  public static bool IsReadOnlyDictionary(Type t)
  {
#if !NET35

    return t.IsGenericType && t.GetGenericTypeDefinition() is var generic &&
           generic == typeof(IReadOnlyDictionary<,>);
#else
      return false;
#endif
  }

}