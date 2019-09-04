using System;
using System.Collections.Generic;
using System.Reflection;
using JetBrains.Rd;
using JetBrains.Rd.Reflection;
using JetBrains.Util;

namespace Test.RdFramework.Reflection
{
  public class TestRdTypesCatalog : IPolymorphicTypesCatalog
  {
    private readonly ReflectionSerializers myReflectionSerializers;
    private readonly Dictionary<RdId, (Type type, Action<ISerializers> action)> myRegisterActions = new Dictionary<RdId, (Type, Action<ISerializers>)>();
    private readonly Dictionary<Type,  Action<ISerializers>> myRegisterActionsByType = new Dictionary<Type, Action<ISerializers>>();
    private readonly Dictionary<RdId, Type> myRdIdToTypeMapping = new Dictionary<RdId, Type>();

    public TestRdTypesCatalog(ReflectionSerializers reflectionSerializers)
    {
      myReflectionSerializers = reflectionSerializers;
    }

    public void TryDiscoverRegister(RdId id, ISerializers serializers)
    {
      if (myRegisterActions.TryGetValue(id, out var pair))
      {
        pair.action(serializers);
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
      var typeInfo = type.GetTypeInfo();
      if (ReflectionSerializerVerifier.HasRdModelAttribute(typeInfo) ||
        ReflectionSerializerVerifier.HasRdExtAttribute(typeInfo))
      {
        myRdIdToTypeMapping[RdId.Define(type)] = type;
      }
    }

    public Type TryDiscover(RdId id)
    {
      if (myRegisterActions.TryGetValue(id, out var pair))
      {
        return pair.type;
      }

      return null;
    }

    public void TryRegister(Type realType, ISerializers serializers)
    {
      var serializerPair = myReflectionSerializers.GetOrRegisterSerializerPair(realType);
      ReflectionUtil.InvokeGenericThis(serializers, nameof(serializers.Register), realType,
        new[] {serializerPair.Reader, serializerPair.Writer, null});
    }

    public void Register<T>()
    {
      var pair = myReflectionSerializers.GetOrRegisterSerializerPair(typeof(T));
      // var rdId = RdId.Root.Mix(typeof(T).Name);
      var rdId = RdId.Define<T>();
      myRegisterActions.Add(rdId, (typeof(T), t => t.Register(pair.GetReader<T>(), pair.GetWriter<T>())));
      myRegisterActionsByType.Add(typeof(T), t => t.Register(pair.GetReader<T>(), pair.GetWriter<T>()));
    }
  }
}