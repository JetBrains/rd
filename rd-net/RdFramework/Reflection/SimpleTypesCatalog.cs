using System;
using System.Collections.Generic;
using JetBrains.Util;

namespace JetBrains.Rd.Reflection
{
  public class SimpleTypesCatalog : IPolymorphicTypesCatalog
  {
    private readonly ReflectionSerializersFactory myReflectionSerializersFactory;
    private readonly Dictionary<RdId, Type> myRdIdToTypeMapping = new Dictionary<RdId, Type>();

    public SimpleTypesCatalog(ReflectionSerializersFactory reflectionSerializersFactory)
    {
      myReflectionSerializersFactory = reflectionSerializersFactory;
    }

    public void TryDiscoverRegister(RdId id, ISerializers serializers)
    {
      if (myRdIdToTypeMapping.TryGetValue(id, out var type))
      {
        TryRegister(type, serializers);
      }
    }

    public void TryDiscoverRegister(Type clrType, ISerializers serializers)
    {
      TryRegister(clrType, serializers);
    }

    public void AddType(Type type)
    {
      myRdIdToTypeMapping[RdId.Define(type)] = type;
    }

    public void Register<T>() => AddType(typeof(T));

    public void TryRegister(Type type, ISerializers serializers)
    {
      var serializerPair = myReflectionSerializersFactory.GetOrRegisterSerializerPair(type);
      ReflectionUtil.InvokeGenericThis(serializers, nameof(serializers.Register), type,
        new[] {serializerPair.Reader, serializerPair.Writer, null});
    }
/*

    public void Register<T>()
    {
      var pair = myReflectionSerializersFactory.GetOrRegisterSerializerPair(typeof(T));
      var rdId = RdId.Define<T>();
      myRegisterActions.Add(rdId, t => t.Register(pair.GetReader<T>(), pair.GetWriter<T>()));
      myRegisterActionsByType.Add(typeof(T), t => t.Register(pair.GetReader<T>(), pair.GetWriter<T>()));
    }*/
  }
}