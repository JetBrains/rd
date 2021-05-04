using System;
using System.Reflection;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Util;
using JetBrains.Util.Util;

#if NET35
using TypeInfo = System.Type;
#endif

namespace JetBrains.Rd.Reflection
{
  public static class Intrinsic
  {
    [CanBeNull]
    public static SerializerPair TryGetIntrinsicSerializer(TypeInfo typeInfo, Func<Type, SerializerPair> getInstanceSerializer)
    {
      if (ReflectionSerializerVerifier.HasIntrinsicNonProtocolMethods(typeInfo))
      {
        var genericArguments = typeInfo.GetGenericArguments();
        /*
        if (genericArguments.Length == 1)
        {
          var argument = genericArguments[0];
          var staticRead = SerializerReflectionUtil.GetReadStaticSerializer(typeInfo, argument);
          var staticWrite = SerializerReflectionUtil.GetWriteStaticDeserializer(typeInfo);
          return SerializerPair.CreateFromMethods(staticRead, staticWrite, getInstanceSerializer(argument));
        }
        */
        if (genericArguments.Length == 0)
        {
          var staticRead = SerializerReflectionUtil.GetReadStaticNonProtocolSerializer(typeInfo);
          var instanceWriter = SerializerReflectionUtil.GetWriteNonProtocolDeserializer(typeInfo);
          return SerializerPair.CreateFromNonProtocolMethods(staticRead, instanceWriter);
        }

        return null;
      }

      if (ReflectionSerializerVerifier.HasIntrinsicProtocolMethods(typeInfo))
      {
        var genericArguments = typeInfo.GetGenericArguments();
        switch (genericArguments.Length)
        {
          case 2:
          {
            var key = genericArguments[0];
            var value = genericArguments[1];
            var staticRead = SerializerReflectionUtil.GetReadStaticSerializer(typeInfo, key, value);
            var staticWrite = SerializerReflectionUtil.GetWriteStaticDeserializer(typeInfo);
            return SerializerPair.CreateFromMethods(staticRead, staticWrite, getInstanceSerializer(key), getInstanceSerializer(value));
          }
          case 1:
          {
            var argument = genericArguments[0];
            var staticRead = SerializerReflectionUtil.GetReadStaticSerializer(typeInfo, argument);
            var staticWrite = SerializerReflectionUtil.GetWriteStaticDeserializer(typeInfo);
            return SerializerPair.CreateFromMethods(staticRead, staticWrite, getInstanceSerializer(argument));
          }
          case 0:
          {
            var staticRead = SerializerReflectionUtil.GetReadStaticSerializer(typeInfo);
            var staticWrite = SerializerReflectionUtil.GetWriteStaticDeserializer(typeInfo);
            return SerializerPair.CreateFromMethods(staticRead, staticWrite);
          }
          default:
            return null;
        }
      }
      else if (ReflectionSerializerVerifier.HasIntrinsicFields(typeInfo))
      {
        var readField = typeInfo.GetField("Read", BindingFlags.Public | BindingFlags.Static);
        var writeField = typeInfo.GetField("Write", BindingFlags.Public | BindingFlags.Static);
        if (readField == null)
          Assertion.Fail($"Invalid intrinsic serializer for type {typeInfo}. Static field 'Read' with type {typeof(CtxReadDelegate<>).ToString(true)} not found");
        if (writeField == null)
          Assertion.Fail($"Invalid intrinsic serializer for type {typeInfo}. Static field 'Write' with type {typeof(CtxWriteDelegate<>).ToString(true)} not found");
        var reader = readField.GetValue(null);
        var writer = writeField.GetValue(null);
        return new SerializerPair(reader, writer);
      }
      else if (ReflectionSerializerVerifier.HasIntrinsicAttribute(typeInfo))
      {
        var marshallerType = typeInfo.GetCustomAttribute<RdScalarAttribute>().NotNull().Marshaller;
        var marshaller = Activator.CreateInstance(marshallerType);
        return (SerializerPair) ReflectionUtil.InvokeStaticGeneric(typeof(SerializerPair), nameof(SerializerPair.FromMarshaller), typeInfo, marshaller);
      }

      return null;
    }
  }
}