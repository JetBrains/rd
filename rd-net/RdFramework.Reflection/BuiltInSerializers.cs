using System;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using JetBrains.Diagnostics;
using JetBrains.Serialization;
using JetBrains.Util;
using JetBrains.Util.Util;

#if NET35
using TypeInfo = System.Type;
#endif

namespace JetBrains.Rd.Reflection
{
  public static class BuiltInSerializers
  {
    public enum BuiltInType
    {
      /// <summary>
      /// Built-in serializer isn't defined
      /// </summary>
      None,

      /// <summary>
      /// static CtxReadDelegate{T1} Read
      /// static CtxWriteDelegate{T2} Write
      /// </summary>
      StaticFields,

      /// <summary>
      /// static Val Read(ctx, reader)
      /// static void Write(ctx, reader, val),
      /// </summary>
      StaticProtocolMethods,

      /// <summary>
      /// static Val Read(ctx, reader)
      /// void Write(ctx, writer)
      /// </summary>
      ProtocolMethods,

      /// <summary>
      /// static Val Read(reader)
      /// static void Write(writer, ctx)
      /// </summary>
      StaticMethods,

      /// <summary>
      /// static Val Read(reader)
      /// void Write(writer)
      /// </summary>
      Methods,

      /// <summary>
      /// static Val{T} Read(ctx, reader, CtxReadDelegate{T}, CtxWriteDelegate{T})
      /// static void Write(ctx, writer, Val{T})
      /// </summary>
      ProtocolCollectionLike1,

      /// <summary>
      /// static Val{T1, T2} Read(ctx, reader, CtxReadDelegate{T1}, CtxWriteDelegate{T1}, CtxReadDelegate{T2}, CtxWriteDelegate{T2})
      /// static void Write(ctx, writer, Val{T1, T2})
      /// </summary>
      ProtocolCollectionLike2,

      /// <summary>
      /// Specified using RdScalar(typeof(Marshaller)) attribute
      /// </summary>
      MarshallerAttribute,
    }

    public static bool Has(TypeInfo t)
    {
      return GetBuiltInType(t) != BuiltInType.None;
    }

    public static BuiltInType GetBuiltInType(TypeInfo t)
    {
      if (HasBuiltInAttribute(t))
        return BuiltInType.MarshallerAttribute;

      var writeMethod = t.GetMethod("Write", BindingFlags.Public | BindingFlags.Static | BindingFlags.Instance | BindingFlags.DeclaredOnly);
      if (writeMethod != null)
      {
        var genericLength = t.GetGenericArguments().Length;
        var parameters = writeMethod.GetParameters();
        if (genericLength == 1 || genericLength == 2)
        {
          // more than one Read is defined for RdMap. We can't query method by name directly and have to allocate array.
          var likeReadMethods = t.GetMethods(BindingFlags.Static | BindingFlags.Public | BindingFlags.DeclaredOnly);
          foreach (var likeReadMethod in likeReadMethods)
          {
            if (!StringComparer.Ordinal.Equals("Read", likeReadMethod.Name))
              continue;

            var readParameters = likeReadMethod.GetParameters();
            if (readParameters.Length == 2 + genericLength * 2 && readParameters[0].ParameterType == typeof(SerializationCtx))
              return genericLength == 1 ? BuiltInType.ProtocolCollectionLike1 : BuiltInType.ProtocolCollectionLike2;
          }
        }

        if (parameters.Length == 0)
          return BuiltInType.None;
        var hasSerializationCtx = parameters[0].ParameterType == typeof(SerializationCtx);
        if (writeMethod.IsStatic)
          return hasSerializationCtx ? BuiltInType.StaticProtocolMethods : BuiltInType.StaticMethods;
        else
          return hasSerializationCtx ? BuiltInType.ProtocolMethods : BuiltInType.Methods;
      }

      var fieldInfo = t.GetField("Write", BindingFlags.Public | BindingFlags.Static | BindingFlags.DeclaredOnly);
      if (fieldInfo != null)
      {
        return BuiltInType.StaticFields;
      }

      return BuiltInType.None;
    }


    public static SerializerPair? TryGet(TypeInfo typeInfo, Func<Type, SerializerPair> getInstanceSerializer, BuiltInType? type = null)
    {
      var serType = type ?? GetBuiltInType(typeInfo);

      switch (serType)
      {
        case BuiltInType.None:
        {
          return null;
        }
        case BuiltInType.StaticFields:
        {
          return GetPairFromFields(typeInfo);
        }
        case BuiltInType.StaticProtocolMethods:
        {
          var staticRead = GetReadStaticProtocolMethod(typeInfo);
          var staticWrite = GetWriteProtocolStaticMethod(typeInfo);
          return SerializerPair.CreateFromMethods(staticRead, staticWrite);
        }
        case BuiltInType.ProtocolMethods:
        {
          var staticRead = GetReadStaticProtocolMethod(typeInfo);
          var staticWrite = GetWriteProtocolMethod(typeInfo);
          return SerializerPair.CreateFromMethods(staticRead, staticWrite);
          }
        case BuiltInType.StaticMethods:
        {
          var staticRead = GetReadStaticMethod(typeInfo);
          var staticWrite = GetWriteStaticMethod(typeInfo);
          return SerializerPair.CreateFromNonProtocolMethods(staticRead, staticWrite);
        }
        case BuiltInType.Methods:
        {
          var staticRead = GetReadStaticMethod(typeInfo);
          var write = GetWriteMethod(typeInfo);
          return SerializerPair.CreateFromNonProtocolMethods(staticRead, write);
        }
        case BuiltInType.MarshallerAttribute:
        {
          var marshallerType = typeInfo.GetCustomAttribute<RdScalarAttribute>().NotNull().Marshaller.NotNull("Marshaller cannot be null");
          var marshaller = Activator.CreateInstance(marshallerType);
          return (SerializerPair?)ReflectionUtil.InvokeStaticGeneric(typeof(SerializerPair), nameof(SerializerPair.FromMarshaller), typeInfo, marshaller);
        }
        case BuiltInType.ProtocolCollectionLike1:
        {
          var genericArguments = typeInfo.GetGenericArguments();
          var argument = genericArguments[0];
          var read = GetReadStaticSerializer(typeInfo, argument);
          var write = GetWriteProtocolStaticMethod(typeInfo);
          return SerializerPair.CreateFromMethods(read, write, getInstanceSerializer(argument));
        }
        case BuiltInType.ProtocolCollectionLike2:
        {
          var genericArguments = typeInfo.GetGenericArguments();
          var key = genericArguments[0];
          var value = genericArguments[1];
          var read = GetReadStaticSerializer(typeInfo, key, value);
          var write = GetWriteProtocolStaticMethod(typeInfo);
          return SerializerPair.CreateFromMethods(read, write, getInstanceSerializer(key), getInstanceSerializer(value));
        }
        default:
          throw new ArgumentOutOfRangeException();
      }
    }

    public static bool HasBuiltInAttribute(TypeInfo t)
    {
      var rdScalar = t.GetCustomAttribute<RdScalarAttribute>();
      return rdScalar != null && rdScalar.Marshaller != null;
    }

    public static bool HasBuiltInFields(TypeInfo t)
    {
      foreach (var member in t.GetFields(BindingFlags.Static | BindingFlags.Public))
      {
        if (member.Name == "Read" || member.Name == "Write")
        {
          return true;
        }
      }

      return false;

    }

    private static SerializerPair? GetPairFromFields(TypeInfo typeInfo)
    {
      var readField = typeInfo.GetField("Read", BindingFlags.Public | BindingFlags.Static);
      var writeField = typeInfo.GetField("Write", BindingFlags.Public | BindingFlags.Static);
      if (readField == null)
        Assertion.Fail($"Invalid BuiltIn serializer for type {typeInfo}. Static field 'Read' with type {typeof(CtxReadDelegate<>).ToString(true)} not found");
      if (writeField == null)
        Assertion.Fail($"Invalid BuiltIn serializer for type {typeInfo}. Static field 'Write' with type {typeof(CtxWriteDelegate<>).ToString(true)} not found");
      var reader = readField.GetValue(null);
      var writer = writeField.GetValue(null);
      return new SerializerPair(reader, writer);
    }


    public static MethodInfo GetReadStaticProtocolMethod(TypeInfo typeInfo)
    {
      var types = new[]
      {
        typeof(SerializationCtx),
        typeof(UnsafeReader),
      };
      var methodInfo = typeInfo.GetMethod("Read", types);

      if (methodInfo == null)
      {
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public static Read({nameof(SerializationCtx)}, {nameof(UnsafeReader)}");
      }

      return methodInfo;
    }

    public static MethodInfo GetReadStaticMethod(TypeInfo typeInfo)
    {
      var types = new[]
      {
        typeof(UnsafeReader),
      };
      var methodInfo = typeInfo.GetMethod("Read", types);

      if (methodInfo == null)
      {
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public static Read({nameof(UnsafeReader)})");
      }

      return methodInfo;
    }

    public static MethodInfo GetReadStaticSerializer(TypeInfo typeInfo, Type argumentType)
    {
      var types = new[]
      {
        typeof(SerializationCtx),
        typeof(UnsafeReader),
        typeof(CtxReadDelegate<>).MakeGenericType(argumentType),
        typeof(CtxWriteDelegate<>).MakeGenericType(argumentType)
      };
      var methodInfo = typeInfo.GetMethod("Read", types);

      if (methodInfo == null)
      {
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public static Read({String.Join(", ", types.Select(t => t.ToString(true)).ToArray())})");
      }

      return methodInfo;
    }

    public static MethodInfo GetReadStaticSerializer(TypeInfo typeInfo, Type key, Type value)
    {
      var types = new[]
      {
        typeof(SerializationCtx),
        typeof(UnsafeReader),
        typeof(CtxReadDelegate<>).MakeGenericType(key),
        typeof(CtxWriteDelegate<>).MakeGenericType(key),
        typeof(CtxReadDelegate<>).MakeGenericType(value),
        typeof(CtxWriteDelegate<>).MakeGenericType(value)
      };
      var methodInfo = typeInfo.GetMethod("Read", types);

      if (methodInfo == null)
      {
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public static Read({String.Join(", ", types.Select(t => t.ToString(true)).ToArray())})");
      }

      return methodInfo;
    }

    public static MethodInfo GetWriteProtocolMethod(TypeInfo typeInfo)
    {
      var types = new[]
      {
        typeof(SerializationCtx),
        typeof(UnsafeWriter),
      };
      var instanceMethod = typeInfo.GetMethod("Write", types, null);
      if (instanceMethod == null)
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public Write({String.Join(", ", types.Select(t => t.ToString(true)).ToArray())})");
      return instanceMethod;
    }


    public static MethodInfo GetWriteProtocolStaticMethod(TypeInfo typeInfo)
    {
      var types = new[]
      {
        typeof(SerializationCtx),
        typeof(UnsafeWriter),
        typeInfo.AsType(),
      };
      var methodInfo = typeInfo.GetMethod("Write", types, null);
      if (methodInfo == null)
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public static Write({String.Join(", ", types.Select(t => t.ToString(true)).ToArray())})");
      return methodInfo;
    }

    private static MethodInfo GetWriteMethod(TypeInfo typeInfo)
    {
      var instanceWrite = typeInfo.GetMethod("Write", new[]
      {
        typeof(UnsafeWriter),
      }, null);
      if (instanceWrite == null)
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature: void Write({nameof(UnsafeWriter)})");
      return instanceWrite;
    }

    public static MethodInfo GetWriteStaticMethod(TypeInfo typeInfo)
    {
      var staticWrite = typeInfo.GetMethod("Write", new[]
      {
        typeof(UnsafeWriter),
        typeInfo.AsType()
      }, null);
      if (staticWrite == null)
        Assertion.Fail($"Unable to found static void Write({nameof(UnsafeWriter)}, {typeInfo.ToString(true)}) method in {typeInfo.ToString(true)} ");
      return staticWrite;
    }
  }
}