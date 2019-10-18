using System;
using System.Collections.Generic;
using System.Reflection;
using JetBrains.Util;

namespace JetBrains.Rd.Reflection
{
  public class SimpleTypesCatalog : IPolymorphicTypesCatalog
  {
    private readonly ReflectionSerializersFactory myReflectionSerializersFactory;
    private readonly Dictionary<RdId, Action<ISerializers>> myRegisterActions = new Dictionary<RdId, Action<ISerializers>>();
    private readonly Dictionary<Type, Action<ISerializers>> myRegisterActionsByType = new Dictionary<Type, Action<ISerializers>>();
    private readonly Dictionary<RdId, Type> myRdIdToTypeMapping = new Dictionary<RdId, Type>();

    public SimpleTypesCatalog(ReflectionSerializersFactory reflectionSerializersFactory)
    {
      myReflectionSerializersFactory = reflectionSerializersFactory;
    }

    public void TryDiscoverRegister(RdId id, ISerializers serializers)
    {
      if (myRegisterActions.TryGetValue(id, out var pair))
      {
        pair(serializers);
      }
      else if (myRdIdToTypeMapping.TryGetValue(id, out var type))
      {
        TryRegister(type, serializers);
      }
    }

    public void TryDiscoverRegister(Type clrType, ISerializers serializers)
    {
      if (myRegisterActionsByType.TryGetValue(clrType, out var action))
      {
        action(serializers);
      }
      else
      {
        TryRegister(clrType, serializers);
      }
    }

    public void AddType(Type type)
    {
      myRdIdToTypeMapping[RdId.Define(type)] = type;
    }

    public void TryRegister(Type realType, ISerializers serializers)
    {
      var serializerPair = myReflectionSerializersFactory.GetOrRegisterSerializerPair(realType);
      ReflectionUtil.InvokeGenericThis(serializers, nameof(serializers.Register), realType,
        new[] {serializerPair.Reader, serializerPair.Writer, null});
    }

    public void Register<T>()
    {
      var pair = myReflectionSerializersFactory.GetOrRegisterSerializerPair(typeof(T));
      // var rdId = RdId.Root.Mix(typeof(T).Name);
      var rdId = RdId.Define<T>();
      myRegisterActions.Add(rdId, t => t.Register(pair.GetReader<T>(), pair.GetWriter<T>()));
      myRegisterActionsByType.Add(typeof(T), t => t.Register(pair.GetReader<T>(), pair.GetWriter<T>()));
    }
  }
}