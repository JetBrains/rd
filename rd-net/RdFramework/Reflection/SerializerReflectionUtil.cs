using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using JetBrains.Annotations;
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
    private static readonly MethodInfo ourConvertTypedCtxRead = typeof(SerializerReflectionUtil).GetTypeInfo().GetMethod(nameof(CtxReadTypedToObject), BindingFlags.Static | BindingFlags.NonPublic);
    private static readonly MethodInfo ourConvertTypedCtxWrite = typeof(SerializerReflectionUtil).GetTypeInfo().GetMethod(nameof(CtxWriteTypedToObject), BindingFlags.Static | BindingFlags.NonPublic);

    [NotNull]
    public static MethodInfo GetReadStaticSerializer([NotNull] TypeInfo typeInfo)
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

    [NotNull]
    public static MethodInfo GetReadStaticSerializer([NotNull] TypeInfo typeInfo, Type argumentType)
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

    [NotNull]
    public static MethodInfo GetWriteStaticDeserializer([NotNull] TypeInfo typeInfo)
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

    /// <summary>
    /// Get lists of members which take part in object serialization.
    /// Can be used for RdExt, RdModel and any RdScalar.
    /// </summary>
    [NotNull]
    internal static FieldInfo[] GetBindableMembers(TypeInfo typeInfo)
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

      bool isRdExt = baseType == typeof(RdExtReflectionBindableBase);

      var members = GetFields(typeInfo, baseType);
      var list = new List<FieldInfo>();
      foreach (var mi in members)
      {
        if (
          mi.MemberType == MemberTypes.Field &&
          (mi.DeclaringType != null && !mi.DeclaringType.GetTypeInfo().IsAssignableFrom(baseType)) &&
          mi.GetCustomAttribute<NonSerializedAttribute>() == null &&

          // arbitrary data is allowed in RdExt since they don't have to be serializable
          !(isRdExt && ReflectionSerializerVerifier.IsScalar(ReflectionSerializerVerifier.GetImplementingType(mi.FieldType.GetTypeInfo())))
        )
        {
          list.Add(mi);
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

    internal static CtxReadDelegate<object> ConvertReader(Type returnType, object reader)
    {
      if (reader is CtxReadDelegate<object> objReader)
        return objReader;

      var genericTypedRead = ourConvertTypedCtxRead.MakeGenericMethod(returnType);
      var result = genericTypedRead.Invoke(null, new[] { reader });
      return (CtxReadDelegate<object>)result;
    }

    internal static CtxWriteDelegate<object> ConvertWriter(Type returnType, object writer)
    {
      if (writer is CtxWriteDelegate<object> objWriter)
        return objWriter;

      return (CtxWriteDelegate<object>)ourConvertTypedCtxWrite.MakeGenericMethod(returnType).Invoke(null, new[] { writer });
    }


    private static CtxReadDelegate<object> CtxReadTypedToObject<T>(CtxReadDelegate<T> typedDelegate)
    {
      return (ctx, unsafeReader) => typedDelegate(ctx, unsafeReader);
    }

    private static CtxWriteDelegate<object> CtxWriteTypedToObject<T>(CtxWriteDelegate<T> typedDelegate)
    {
      return (ctx, unsafeWriter, value) => typedDelegate(ctx, unsafeWriter, (T)value);
    }

  }
}