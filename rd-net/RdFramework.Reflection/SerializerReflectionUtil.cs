﻿using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices.ComTypes;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Serialization;
using JetBrains.Util.Util;

#if NET35
using TypeInfo = System.Type;
#endif

namespace JetBrains.Rd.Reflection
{
  public class SerializerReflectionUtil
  {
    public static MethodInfo GetReadStaticSerializer(TypeInfo typeInfo)
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

    public static MethodInfo GetReadStaticNonProtocolSerializer(TypeInfo typeInfo)
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
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public static Read({String.Join(", ", types.Select(t=>t.ToString(true)).ToArray())})");
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

    public static MethodInfo GetWriteStaticDeserializer(TypeInfo typeInfo)
    {
      var types = new[]
      {
        typeof(SerializationCtx),
        typeof(UnsafeWriter),
        typeInfo.AsType(),
      };
      var methodInfo = typeInfo.GetMethod("Write",  types, null);

      if (methodInfo == null)
      {
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public static Write({String.Join(", ", types.Select(t => t.ToString(true)).ToArray())})");
      }

      return methodInfo;
    }

    public static MethodInfo GetWriteNonProtocolDeserializer(TypeInfo typeInfo)
    {
      var instanceWrite = typeInfo.GetMethod("Write", new[]
      {
        typeof(UnsafeWriter),
      }, null);

      if (instanceWrite != null)
        return instanceWrite;

      var staticWrite = typeInfo.GetMethod("Write", new[]
      {
        typeof(UnsafeWriter),
        typeInfo.AsType()
      }, null);
      if (staticWrite != null)
        return staticWrite;

      Assertion.Fail($"Unable to found neither static Write({nameof(UnsafeWriter)}, value) nor value.Write({nameof(UnsafeWriter)}) method in {typeInfo.ToString(true)} ");

      return instanceWrite;
    }


    /// <summary>
    /// Get lists of members which take part in object serialization.
    /// Can be used for RdExt, RdModel and any RdScalar.
    /// </summary>
    internal static FieldInfo[] GetBindableFields(TypeInfo typeInfo)
    {
/*
      var rpcInterface = GetRpcInterface();
      if (rpcInterface != null)
      {
        var rpcInterfaceMap = typeInfo.GetInterfaceMap(rpcInterface);
        //members = rpcInterfaceMap.TargetMethods;
      }
*/
      Type baseType;
      if (ReflectionSerializerVerifier.HasRdExtAttribute(typeInfo))
        baseType = typeof(RdExtReflectionBindableBase);
      else if (ReflectionSerializerVerifier.HasRdModelAttribute(typeInfo))
        baseType = typeof(RdReflectionBindableBase);
      else
        baseType = typeof(RdBindableBase);

      bool isRdExtImpl = baseType == typeof(RdExtReflectionBindableBase) && !typeInfo.GetInterfaces().Contains(typeof(IProxyTypeMarker));
      bool isRdRpcInterface = typeInfo.IsInterface; // can be specified in RdExt // && typeInfo.GetCustomAttribute<RdRpcAttribute>() != null;

      var fields = GetFields(typeInfo, baseType);
      var list = new List<FieldInfo>();
      foreach (var mi in fields)
      {
        if (typeof(RdExtReflectionBindableBase).IsAssignableFrom(mi.FieldType))
          continue;

        if (
          mi.MemberType == MemberTypes.Field &&
          (mi.DeclaringType != null && !mi.DeclaringType.GetTypeInfo().IsAssignableFrom(baseType)) &&
          mi.GetCustomAttribute<NonSerializedAttribute>() == null &&

          // arbitrary data is allowed in RdExt implementations since they don't have to be serializable
          !(isRdExtImpl && ReflectionSerializerVerifier.IsScalar(ReflectionSerializerVerifier.GetImplementingType(mi.FieldType.GetTypeInfo())))
        )
        {
          list.Add(mi);
        }
        else if (isRdRpcInterface)
        {
          throw new Exception($"Invalid member in RdRpc interface: {typeInfo.ToString(true)}.{mi.Name}");
        }
      }

      return list.ToArray();
    }

    private static IEnumerable<FieldInfo> GetFields(Type type, Type baseType)
    {
      foreach (var field in type.GetFields(BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public))
        yield return field;

      // private fields only being returned for the current type
      while ((type = type.BaseType) != baseType && type != null)
      {
        // but protected fields are returned in first step
        foreach (var baseField in type.GetFields(BindingFlags.Instance | BindingFlags.NonPublic))
          if (baseField.IsPrivate)
            yield return baseField;
      }
    }

    internal static CtxReadDelegate<TOut> ConvertReader<TOut>(object reader)
    {
      if (reader is CtxReadDelegate<TOut> objReader)
        return objReader;

      var genericTypedRead = ourConvertTypedCtxRead.MakeGenericMethod(reader.GetType().GetGenericArguments()[0], typeof(object));
      var result = genericTypedRead.Invoke(null, new[] { reader });
      return (CtxReadDelegate<TOut>)result;
    }

    internal static CtxWriteDelegate<TOut> ConvertWriter<TOut>(object writer)
    {
      if (writer is CtxWriteDelegate<TOut> objWriter)
        return objWriter;

      return (CtxWriteDelegate<TOut>)ourConvertTypedCtxWrite.MakeGenericMethod(writer.GetType().GetGenericArguments()[0], typeof(TOut)).Invoke(null, new[] { writer });
    }

    private static readonly MethodInfo ourConvertTypedCtxRead = typeof(SerializerReflectionUtil).GetTypeInfo().GetMethod(nameof(CtxReadTypedToObject), BindingFlags.Static | BindingFlags.NonPublic)!;
    private static CtxReadDelegate<TOut> CtxReadTypedToObject<TIn, TOut>(CtxReadDelegate<TIn> typedDelegate)
    {
      return (ctx, unsafeReader) => (TOut) (object) typedDelegate(ctx, unsafeReader)!;
    }

    private static readonly MethodInfo ourConvertTypedCtxWrite = typeof(SerializerReflectionUtil).GetTypeInfo().GetMethod(nameof(CtxWriteTypedToObject), BindingFlags.Static | BindingFlags.NonPublic)!;
    private static CtxWriteDelegate<TOut> CtxWriteTypedToObject<TIn, TOut>(CtxWriteDelegate<TIn> typedDelegate)
    {
      return (ctx, unsafeWriter, value) => typedDelegate(ctx, unsafeWriter, (TIn) (object) value!) ;
    }

    public static bool CanBePolymorphic(Type type)
    {
      /*if (IsList(type) || IsDictionary(type))
        return false;
        */

      return (type.IsClass && !type.IsSealed) || type.IsInterface;
    }
  }
}