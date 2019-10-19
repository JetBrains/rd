using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Expressions;
using System.Reflection;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using JetBrains.Util;
using JetBrains.Util.Util;


#if NET35
using TypeInfo = System.Type;
#endif

namespace JetBrains.Rd.Reflection
{
  [AttributeUsage(AttributeTargets.Class, Inherited = false), MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  [BaseTypeRequired(typeof(RdReflectionBindableBase))]
  public class RdExtAttribute : Attribute { }

  /// <summary>
  /// Mark implementing interface of RdExt by this attribute to indicate intent to use this interface for proxy generation
  /// </summary>
  [AttributeUsage(AttributeTargets.Interface), MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  public class RdRpcAttribute : Attribute { }

  [MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  [AttributeUsage(AttributeTargets.Class | AttributeTargets.Enum, Inherited = false)]
  // [BaseTypeRequired(typeof(RdBindableBase))] // todo: should RdModel only exits for live models?
  public class RdModelAttribute : Attribute { }

  [Obsolete("RdAsync enabled by default for everything")]
  [AttributeUsage(AttributeTargets.Field | AttributeTargets.Property | AttributeTargets.Method)]
  public class RdAsyncAttribute : Attribute { }

  /// <summary>
  /// Marker interface for proxy types.
  /// Used to distinguish between proxy-implemented methods, for which we should only initialize RdCall fields and other reactive properties
  /// and real methods in types, for which we should Bind appropriate RdEndpoint.
  /// </summary>
  public interface IProxyTypeMarker
  {
  }

  /// <summary>
  /// Creates and provides access to Reflection-generated serializers for Rd, thread safe
  /// </summary>
  public class ReflectionSerializersFactory
  {
    private static readonly MethodInfo ConvertTypedCtxRead = typeof(ReflectionSerializersFactory).GetTypeInfo().GetMethod(nameof(CtxReadTypedToObject), BindingFlags.Static | BindingFlags.NonPublic);
    private static readonly MethodInfo ConvertTypedCtxWrite = typeof(ReflectionSerializersFactory).GetTypeInfo().GetMethod(nameof(CtxWriteTypedToObject), BindingFlags.Static | BindingFlags.NonPublic);

    /// <summary>
    /// Collection true type to non-polymorphic serializer
    /// </summary>
    private readonly Dictionary<Type, SerializerPair> mySerializers = new Dictionary<Type, SerializerPair>();

    private readonly object myLock = new object();

    public bool KnownType(Type type)
    {
      lock (myLock)
      {
        return mySerializers.ContainsKey(type);
      }
    }

#if JET_MODE_ASSERT
    /// <summary>
    /// current serialization stack.
    ///
    /// used to provide diagnostics about circular dependencies only.
    /// </summary>
    private readonly Queue<Type> myCurrentSerializersChain = new Queue<Type>();
#endif

    public ReflectionSerializersFactory()
    {
      Serializers.RegisterFrameworkMarshallers(new SerializersContainer(mySerializers));
    }

    [NotNull]
    public SerializerPair GetOrRegisterSerializerPair([NotNull] Type type)
    {
      lock (myLock)
      {
#if JET_MODE_ASSERT
        myCurrentSerializersChain.Clear();
#endif
        return GetOrRegisterSerializerInternal(type);
      }
    }

    private SerializerPair GetOrRegisterSerializerInternal(Type type)
    {
      if (!mySerializers.TryGetValue(type, out var serializerPair))
      {
#if JET_MODE_ASSERT
        myCurrentSerializersChain.Enqueue(type);
#endif

        ReflectionUtil.InvokeGenericThis(this, nameof(RegisterModelSerializer), type);

#if JET_MODE_ASSERT
        myCurrentSerializersChain.Dequeue();
#endif

        if (!mySerializers.TryGetValue(type, out serializerPair))
        {
          throw new KeyNotFoundException($"Unable to register type {type.ToString(true)}: serializer can't be found");
        }
      }

      Assertion.AssertNotNull(serializerPair, $"Unable to register type: {type.ToString(true)}, undetected circular dependency.");

      return serializerPair;
    }

    private SerializerPair GetOrCreateMemberSerializer([NotNull] MemberInfo mi, [NotNull] Type serializerType, bool allowNullable)
    {
      if (!allowNullable)
        ReflectionSerializerVerifier.AssertMemberDeclaration(mi);
      else
        ReflectionSerializerVerifier.AssertDataMemberDeclaration(mi);

      if (!mySerializers.TryGetValue(serializerType, out var serializerPair))
      {
        serializerPair = GetMemberSerializer(mi, serializerType.GetTypeInfo());
        Assertion.AssertNotNull(serializerPair != null, $"Unable to Create serializer for type {serializerType.ToString(true)}");
        mySerializers[serializerType] = serializerPair;
      }

      if (serializerPair == null)
      {
#if JET_MODE_ASSERT
        Assertion.Fail($"Unable to create serializer for {serializerType.ToString(true)}: circular dependency detected: {string.Join(" -> ", myCurrentSerializersChain.Select(t => Types.ToString(t, true)).ToArray())}");
#endif
        throw new Assertion.AssertionException($"Undetected circular dependency during serializing {serializerType.ToString(true)}");
      }

      return serializerPair;
    }

    public static Type GetRpcInterface(TypeInfo typeInfo)
    {
      foreach (var @interface in typeInfo.GetInterfaces())
        if (@interface.IsDefined(typeof(RdRpcAttribute), false))
          return @interface;

      return null;
    }

    [NotNull]
    internal static MemberInfo[] GetBindableMembers(TypeInfo typeInfo)
    {
/*
      var rpcInterface = GetRpcInterface();
      if (rpcInterface != null)
      {
        var rpcInterfaceMap = typeInfo.GetInterfaceMap(rpcInterface);
        //members = rpcInterfaceMap.TargetMethods;
      }
*/

      IEnumerable<MemberInfo> members;
      members = typeInfo.GetMembers(BindingFlags.Public | BindingFlags.Instance);

      var list = new List<MemberInfo>();
      foreach (var mi in members)
      {
        if (mi.DeclaringType != null && !mi.DeclaringType.GetTypeInfo().IsAssignableFrom(typeof(RdReflectionBindableBase)))
        {
          if ((mi.MemberType == MemberTypes.Property && ReflectionUtil.TryGetSetter(mi) != null) ||
              mi.MemberType == MemberTypes.Field)
          {
            list.Add(mi);
          }
        }
      }

      return list.ToArray();
    }

    /// <summary>
    /// Register serializers for either <see cref="RdExtAttribute"/> or <see cref="RdModelAttribute"/>
    /// </summary>
    private void RegisterModelSerializer<T>()
    {
      // place null marker to detect circular dependencies
      mySerializers.Add(typeof(T), null);

      TypeInfo typeInfo = typeof(T).GetTypeInfo();
      ReflectionSerializerVerifier.AssertRoot(typeInfo);
      bool allowNullable = ReflectionSerializerVerifier.HasRdModelAttribute(typeInfo) || ReflectionSerializerVerifier.IsScalar(typeInfo);

      var intrinsicSerializer = TryGetIntrinsicSerializer(typeInfo);
      if (intrinsicSerializer != null)
      {
        mySerializers[typeof(T)] = intrinsicSerializer;
        return;
      }

      var memberInfos = GetBindableMembers(typeInfo);
      var memberSetters = memberInfos.Select(ReflectionUtil.GetSetter).ToArray();
      var memberGetters = memberInfos.Select(ReflectionUtil.GetGetter).ToArray();

      // todo: consider using IL emit
      var memberDeserializers = new CtxReadDelegate<object>[memberInfos.Length];
      var memberSerializers = new CtxWriteDelegate<object>[memberInfos.Length];
      for (var index = 0; index < memberInfos.Length; index++)
      {
        var mi = memberInfos[index];
        var returnType = ReflectionUtil.GetReturnType(mi);
        var serPair = GetOrCreateMemberSerializer(mi, serializerType: returnType, allowNullable: allowNullable);
        memberDeserializers[index] = ConvertReader(returnType, serPair.Reader);
        memberSerializers[index] = ConvertWriter(returnType, serPair.Writer);
      }

      var type = typeInfo.AsType();
      CtxReadDelegate<T> readerDelegate = (ctx, unsafeReader) =>
      {
        // todo: support non-default constructors
        var instance = Activator.CreateInstance(type);

        var bindableInstance = instance as IRdBindable;
        RdId id = default(RdId);
        if (bindableInstance != null)
          id = unsafeReader.ReadRdId();

        for (var index = 0; index < memberDeserializers.Length; index++)
        {
          var value = memberDeserializers[index](ctx, unsafeReader);
          memberSetters[index](instance, value);
        }

        bindableInstance?.WithId(id);

        return (T) instance;
      };

      CtxWriteDelegate<T> writerDelegate = (ctx, unsafeWriter, value) =>
      {
        if (value is IRdBindable bindableInstance)
        {
          unsafeWriter.Write(bindableInstance.RdId);
        }

        for (var i = 0; i < memberDeserializers.Length; i++)
        {
          var memberValue = memberGetters[i](value);
          memberSerializers[i](ctx, unsafeWriter, memberValue);
        }
      };

      mySerializers[type] = new SerializerPair(readerDelegate, writerDelegate);
    }

    [CanBeNull]
    private SerializerPair TryGetIntrinsicSerializer(TypeInfo typeInfo)
    {
      if (!ReflectionSerializerVerifier.HasIntrinsicMethods(typeInfo))
        return null;

      var genericArguments = typeInfo.GetGenericArguments();
      if (genericArguments.Length == 1)
      {
        var argument = genericArguments[0];
        var staticRead = GetReadStaticSerializer(typeInfo, argument);
        var staticWrite = GetWriteStaticDeserializer(typeInfo);
        return SerializerPair.CreateFromMethods(staticRead, staticWrite, GetOrRegisterSerializerInternal(argument));
      }

      if (genericArguments.Length == 0)
      {
        var staticRead = GetReadStaticSerializer(typeInfo);
        var staticWrite = GetWriteStaticDeserializer(typeInfo);
        return SerializerPair.CreateFromMethods(staticRead, staticWrite);
      }

      return null;
    }

    /// <summary>
    /// Register serializer for ValueTuples
    /// </summary>
    private SerializerPair CreateValueTupleSerializer<T>()
    {
      TypeInfo typeInfo = typeof(T).GetTypeInfo();
      ReflectionSerializerVerifier.AssertRoot(typeInfo);

      var argumentTypes = typeInfo.GetGenericArguments();

      var memberGetters = typeInfo.GetFields().Select(ReflectionUtil.GetGetter).ToArray();

      var memberDeserializers = new CtxReadDelegate<object>[argumentTypes.Length];
      var memberSerializers = new CtxWriteDelegate<object>[argumentTypes.Length];
      for (var index = 0; index < argumentTypes.Length; index++)
      {
        var argumentType = argumentTypes[index];
        var serPair = GetOrRegisterSerializerInternal(argumentType);
        memberDeserializers[index] = ConvertReader(argumentType, serPair.Reader);
        memberSerializers[index] = ConvertWriter(argumentType, serPair.Writer);
      }

      var type = typeInfo.AsType();
      CtxReadDelegate<T> readerDelegate = (ctx, unsafeReader) =>
      {
        // todo: consider using IL emit
        var activatorArgs = new object[argumentTypes.Length];
        for (var index = 0; index < argumentTypes.Length; index++)
        {
          var value = memberDeserializers[index](ctx, unsafeReader);
          activatorArgs[index] = value;
        }

        var instance = Activator.CreateInstance(type, activatorArgs);
        return (T) instance;
      };

      CtxWriteDelegate<T> writerDelegate = (ctx, unsafeWriter, value) =>
      {
        for (var i = 0; i < argumentTypes.Length; i++)
        {
          var memberValue = memberGetters[i](value);
          memberSerializers[i](ctx, unsafeWriter, memberValue);
        }
      };

      return new SerializerPair(readerDelegate, writerDelegate);
    }

    private SerializerPair GetMemberSerializer(MemberInfo member, TypeInfo typeInfo)
    {
      var implementingType = ReflectionSerializerVerifier.GetImplementingType(typeInfo);
      var implementingTypeInfo = implementingType.GetTypeInfo();

      if (mySerializers.TryGetValue(typeInfo.AsType(), out var pair))
      {
        return pair;
      }

      if (ReflectionSerializerVerifier.IsValueTuple(implementingTypeInfo))
      {
        return (SerializerPair) ReflectionUtil.InvokeGenericThis(this, nameof(CreateValueTupleSerializer), typeInfo.AsType());
      }

      var hasRdAttribute = ReflectionSerializerVerifier.HasRdModelAttribute(typeInfo) || ReflectionSerializerVerifier.HasRdExtAttribute(typeInfo);
      if (typeInfo.IsGenericType && !hasRdAttribute)
      {
        return CreateGenericSerializer(member, typeInfo, implementingType, implementingTypeInfo);
      }

      if (typeInfo.IsEnum)
      {
        var serializer = ReflectionUtil.InvokeGenericThis(this, nameof(CreateEnumSerializer), typeInfo.AsType());
        return (SerializerPair) serializer;
      }

      if (typeInfo.IsArray)
      {
        var serializer = ReflectionUtil.InvokeGenericThis(this, nameof(CreateArraySerializer), typeInfo.GetElementType());
        return (SerializerPair) serializer;
      }

      if (hasRdAttribute)
      {
        return GetOrRegisterSerializerInternal(typeInfo.AsType());
      }

      Assertion.Fail($"Unable to serialize member: {member.DeclaringType?.ToString(true)}.{member.Name} of type {typeInfo.ToString(true)}");

      return null;
    }

    private SerializerPair CreateEnumSerializer<T>()
    {
      if (mySerializers.TryGetValue(typeof(T), out var serializerPair))
        return serializerPair;

      var readerParameter = Expression.Parameter(typeof(int), "reader");
      var readerConvert = Expression.ConvertChecked(readerParameter, typeof(T));
      var readerCaster = Expression.Lambda<Func<int, T>>(readerConvert, readerParameter).Compile();

      var writerParameter = Expression.Parameter(typeof(T), "writer");
      var writerConvert = Expression.ConvertChecked(writerParameter, typeof(int));
      var writerCaster = Expression.Lambda<Func<T, int>>(writerConvert, writerParameter).Compile();

      Assertion.Assert(typeof(T).IsSubclassOf(typeof(Enum)), "{0}", typeof(T));
      var result = new SerializerPair(
        (CtxReadDelegate<T>) ((ctx, reader) => readerCaster(reader.ReadInt())),
        (CtxWriteDelegate<T>) ((ctx, w, o) => w.Write(writerCaster(o))));
      mySerializers[typeof(T)] = result;
      return result;
    }

    private SerializerPair GetPrimitiveSerializer<T>()
    {
      Assertion.Assert(ReflectionSerializerVerifier.IsPrimitive(typeof(T)), $"{typeof(T).ToString(true)} expected to be primitive type");
      return mySerializers[typeof(T)];
    }

    private SerializerPair CreateArraySerializer<T>()
    {
      var primitiveSerializer = GetPrimitiveSerializer<T>();
      var valueReader = primitiveSerializer.GetReader<T>();
      var valueWriter = primitiveSerializer.GetWriter<T>();

      CtxReadDelegate<T[]> reader = (ctx, unsafeReader) => unsafeReader.ReadArray(valueReader, ctx);
      CtxWriteDelegate<T[]> writer = (ctx, unsafeWriter, value) => unsafeWriter.WriteArray(valueWriter, ctx, value);
      return new SerializerPair(reader, writer);
    }

    private SerializerPair CreateGenericSerializer(MemberInfo member, TypeInfo typeInfo, Type implementingType, TypeInfo implementingTypeInfo)
    {
      var genericDefinition = implementingType.GetGenericTypeDefinition();

      if (genericDefinition == typeof(RdProperty<>))
      {
        return CreateStaticReaderSingleGeneric(member, typeInfo.AsType(), implementingType, allowNullable: true);
      }

      if (genericDefinition == typeof(RdSignal<>) ||
          genericDefinition == typeof(RdList<>) ||
          genericDefinition == typeof(RdSet<>))
      {
        return CreateStaticReaderSingleGeneric(member, typeInfo.AsType(), implementingType, allowNullable: false);
      }

      if (genericDefinition == typeof(RdMap<,>)
        // || genericDefinition == typeof(RdCall<,>) not supported yet
      )
      {
        return CreateStaticReaderTwoGeneric(member, typeInfo.AsType(), implementingType);
      }

      if (genericDefinition == typeof(Nullable<>))
      {
        var genericTypeArgument = implementingTypeInfo.GetGenericArguments()[0];
        var nullableSerializer = (SerializerPair) ReflectionUtil.InvokeGenericThis(this, nameof(RegisterNullable), genericTypeArgument);
        return nullableSerializer;
      }

      throw new Exception($"Unable to register generic type: {typeInfo}");
    }

    private SerializerPair RegisterNullable<T>() where T : struct
    {
      var serPair = GetMemberSerializer(null, typeof(T).GetTypeInfo());
      var ctxReadDelegate = serPair.GetReader<T>();
      var ctxWriteDelegate = serPair.GetWriter<T>();
      return new SerializerPair(ctxReadDelegate.NullableStruct(), ctxWriteDelegate.NullableStruct());
    }

    private SerializerPair CreateStaticReaderTwoGeneric([NotNull] MemberInfo memberInfo, [NotNull] Type type, [NotNull] Type implementingType)
    {
      var keyType = type.GetTypeInfo().GetGenericArguments()[0];
      var valueType = type.GetTypeInfo().GetGenericArguments()[1];

      var types = new[]
      {
        typeof(SerializationCtx),
        typeof(UnsafeReader),
        typeof(CtxReadDelegate<>).MakeGenericType(keyType),
        typeof(CtxWriteDelegate<>).MakeGenericType(keyType),
        typeof(CtxReadDelegate<>).MakeGenericType(valueType),
        typeof(CtxWriteDelegate<>).MakeGenericType(valueType)
      };
      var methodInfo = implementingType.GetTypeInfo().GetMethod("Read", types);
      var readPropertyMethod = methodInfo.NotNull();

      var writeStaticDeserializer = GetWriteStaticDeserializer(implementingType.GetTypeInfo());

      var keySerializer = GetOrCreateMemberSerializer(memberInfo, serializerType: keyType, allowNullable: false);
      var valueSerializer = GetOrCreateMemberSerializer(memberInfo, serializerType: valueType, allowNullable: true);
      return SerializerPair.CreateFromMethods(readPropertyMethod, writeStaticDeserializer, keySerializer, valueSerializer);
    }

    private SerializerPair CreateStaticReaderSingleGeneric(MemberInfo memberInfo, Type type, Type implementingType, bool allowNullable)
    {
      var argumentType = type.GetTypeInfo().GetGenericArguments()[0];

      var readPropertyMethod = GetReadStaticSerializer(implementingType.GetTypeInfo(), argumentType);
      var writePropertyMethod = GetWriteStaticDeserializer(implementingType.GetTypeInfo());

      var argumentSerializerPair = GetOrCreateMemberSerializer(memberInfo, serializerType: argumentType, allowNullable: allowNullable);
      return SerializerPair.CreateFromMethods(readPropertyMethod, writePropertyMethod, argumentSerializerPair);
    }

    private CtxReadDelegate<object> ConvertReader(Type returnType, object reader)
    {
      if (reader is CtxReadDelegate<object> objReader)
        return objReader;

      var genericTypedRead = ConvertTypedCtxRead.MakeGenericMethod(returnType);
      var result = genericTypedRead.Invoke(null, new[] { reader });
      return (CtxReadDelegate<object>)result;
    }

    private CtxWriteDelegate<object> ConvertWriter(Type returnType, object writer)
    {
      if (writer is CtxWriteDelegate<object> objWriter)
        return objWriter;

      return (CtxWriteDelegate<object>)ConvertTypedCtxWrite.MakeGenericMethod(returnType).Invoke(null, new[] { writer });
    }

    private static CtxReadDelegate<object> CtxReadTypedToObject<T>(CtxReadDelegate<T> typedDelegate)
    {
      return (ctx, unsafeReader) => typedDelegate(ctx, unsafeReader);
    }

    private static CtxWriteDelegate<object> CtxWriteTypedToObject<T>(CtxWriteDelegate<T> typedDelegate)
    {
      return (ctx, unsafeWriter, value) => typedDelegate(ctx, unsafeWriter, (T)value);
    }

    [NotNull]
    private static MethodInfo GetReadStaticSerializer([NotNull] TypeInfo typeInfo)
    {
      var types = new[]
      {
        typeof(SerializationCtx),
        typeof(UnsafeReader),
      };
      var methodInfo = typeInfo.GetMethod("Read", types);

      if (methodInfo == null)
      {
        Console.WriteLine($"Something {Console.BackgroundColor}");
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public static Read({nameof(SerializationCtx)}, {nameof(UnsafeReader)}");
      }

      return methodInfo;
    }


    [NotNull]
    private static MethodInfo GetReadStaticSerializer([NotNull] TypeInfo typeInfo, Type argumentType)
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
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public static Read({string.Join(", ", types.Select(t=>t.ToString(true)).ToArray())})");
      }

      return methodInfo;
    }

    [NotNull]
    private static MethodInfo GetWriteStaticDeserializer([NotNull] TypeInfo typeInfo)
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
        Assertion.Fail($"Unable to found method in {typeInfo.ToString(true)} with requested signature : public static Write({string.Join(", ", types.Select(t => t.ToString(true)).ToArray())})");
      }

      return methodInfo;
    }

    public class SerializerPair
    {
      private readonly object myReader;
      private readonly object myWriter;

      public object Reader => myReader;

      public object Writer => myWriter;

      public SerializerPair([NotNull] object reader, [NotNull] object writer)
      {
        if (reader == null) throw new ArgumentNullException(nameof(reader));
        if (writer == null) throw new ArgumentNullException(nameof(writer));

        Assertion.Assert(reader.GetType().GetGenericTypeDefinition() == typeof(CtxReadDelegate<>),
          $"Invalid type: expected CtxReaderDelegate, but was {reader.GetType().ToString(true)}");
        Assertion.Assert(writer.GetType().GetGenericTypeDefinition() == typeof(CtxWriteDelegate<>),
          $"Invalid type: expected CtxWriteDelegate, but was {writer.GetType().ToString(true)}");

        myReader = reader;
        myWriter = writer;
      }

      public CtxReadDelegate<T> GetReader<T>()
      {
        return (CtxReadDelegate<T>) myReader;
      }

      public CtxWriteDelegate<T> GetWriter<T>()
      {
        return (CtxWriteDelegate<T>) myWriter;
      }

      public static SerializerPair CreateFromMethods(MethodInfo readMethod, MethodInfo writeMethod)
      {
        void WriterDelegate(SerializationCtx ctx, UnsafeWriter writer, object value) =>
          writeMethod.Invoke(null, new[] { ctx, writer, value, });

        object ReaderDelegate(SerializationCtx ctx, UnsafeReader reader) =>
          readMethod.Invoke(null, new object[] { ctx, reader });

        CtxReadDelegate<object> ctxReadDelegate = ReaderDelegate;
        CtxWriteDelegate<object> ctxWriteDelegate = WriterDelegate;
        return new SerializerPair(ctxReadDelegate, ctxWriteDelegate);
      }

      public static SerializerPair CreateFromMethods(MethodInfo readMethod, MethodInfo writeMethod, SerializerPair argumentSerializer)
      {
        void WriterDelegate(SerializationCtx ctx, UnsafeWriter writer, object value) =>
          writeMethod.Invoke(null, new[] { ctx, writer, value, });

        object ReaderDelegate(SerializationCtx ctx, UnsafeReader reader) =>
          readMethod.Invoke(null, new[] { ctx, reader, argumentSerializer.Reader, argumentSerializer.Writer });

        CtxReadDelegate<object> ctxReadDelegate = ReaderDelegate;
        CtxWriteDelegate<object> ctxWriteDelegate = WriterDelegate;
        return new SerializerPair(ctxReadDelegate, ctxWriteDelegate);
      }

      public static SerializerPair CreateFromMethods(MethodInfo readMethod, MethodInfo writeMethod, SerializerPair keySerializer, SerializerPair valueSerializer)
      {
        var ctxKeyReadDelegate = keySerializer.Reader;
        var ctxKeyWriteDelegate = keySerializer.Writer;
        var ctxValueReadDelegate = valueSerializer.Reader;
        var ctxValueWriteDelegate = valueSerializer.Writer;

        void WriterDelegate(SerializationCtx ctx, UnsafeWriter writer, object value)
        {
          writeMethod.Invoke(null, new[] {ctx, writer, value,});
        }

        object ReaderDelegate(SerializationCtx ctx, UnsafeReader reader)
        {
          return readMethod.Invoke(null,
            new[] {ctx, reader, ctxKeyReadDelegate, ctxKeyWriteDelegate, ctxValueReadDelegate, ctxValueWriteDelegate});
        }

        var ctxReadDelegate = (CtxReadDelegate<object>) ReaderDelegate;
        var ctxWriteDelegate = (CtxWriteDelegate<object>) WriterDelegate;
        return new SerializerPair(ctxReadDelegate, ctxWriteDelegate);
      }
    }

    private class SerializersContainer : ISerializersContainer
    {
      private readonly IDictionary<Type, SerializerPair> myStore;

      public SerializersContainer(IDictionary<Type, SerializerPair> store)
      {
        myStore = store;
      }

      public void Register<T>(CtxReadDelegate<T> reader, CtxWriteDelegate<T> writer, int? predefinedType = null)
      {
        myStore[typeof(T)] = new SerializerPair(reader, writer);
      }

      public void RegisterEnum<T>() where T: unmanaged, Enum
      {
      }

      public void RegisterToplevelOnce(Type toplevelType, Action<ISerializers> registerDeclaredTypesSerializers)
      {
      }
    }
  }


#if NET35
  public static class Net35Extensions
  {
    public static Type GetTypeInfo(this Type type)
    {
      return type;
    }

    public static Type AsType(this Type type)
    {
      return type;
    }

    public static T GetCustomAttribute<T>(this MemberInfo mi) where T : Attribute
    {
      return (T) Attribute.GetCustomAttribute(mi, typeof(T));
    }
  }
#endif
}