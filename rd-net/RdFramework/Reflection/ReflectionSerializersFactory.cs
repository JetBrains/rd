using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Runtime.InteropServices;
using System.Runtime.Serialization;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using JetBrains.Util;
using JetBrains.Util.Util;
using static JetBrains.Rd.Reflection.ReflectionSerializersFactory;


#if NET35
using TypeInfo = System.Type;
#endif

namespace JetBrains.Rd.Reflection
{
  [AttributeUsage(AttributeTargets.Class, Inherited = false), MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  [BaseTypeRequired(typeof(RdExtReflectionBindableBase))]
  public class RdExtAttribute : Attribute
  {
    [CanBeNull]
    public Type RdRpcInterface { get; }

    public RdExtAttribute() { }

    /// <summary>
    /// Mark RdExt as implementing contract from specific RdRpc interface. That means that this RdExt will be exposed by
    /// interface name, not by the type itself. It may be used when explicit marking of RdRpc is undesirable.
    /// </summary>
    /// <param name="rdRpcInterface">
    ///   RdRpc interface type. Must be implemented by type, which marked by this RdExt attribute.
    /// </param>
    public RdExtAttribute(Type rdRpcInterface)
    {
      RdRpcInterface = rdRpcInterface;
    }
  }

  /// <summary>
  /// Mark implementing interface of RdExt by this attribute to indicate intent to use this interface for proxy generation
  /// </summary>
  [AttributeUsage(AttributeTargets.Interface), MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  public class RdRpcAttribute : Attribute { }

  [MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  [AttributeUsage(AttributeTargets.Class | AttributeTargets.Enum, Inherited = false)]
  [BaseTypeRequired(typeof(RdReflectionBindableBase))]
  public class RdModelAttribute : Attribute { }

  /// <summary>
  /// It has no special semantic. Used only to tell ReSharper about ImplicitUse.
  /// </summary>
  [MeansImplicitUse(ImplicitUseTargetFlags.WithMembers)]
  [AttributeUsage(AttributeTargets.Class | AttributeTargets.Enum | AttributeTargets.Struct | AttributeTargets.Interface, Inherited = false)]
  public class RdScalarAttribute : Attribute
  {
    public Type Marshaller { get; }

    public RdScalarAttribute()
    {
    }

    /// <summary>
    /// Provides external marshaller for this type
    /// </summary>
    /// <param name="marshaller">
    /// A type which implements <see cref="IIntrinsicMarshaller{T}"/> for this type or any base interface.
    /// Keep in mind that if you provide an serializer for base interface a runtime casting error is possible on the
    /// receiver side if receiver want to have an inheritor from this interface
    /// </param>
    public RdScalarAttribute(Type marshaller)
    {
      Marshaller = marshaller;
    }
  }

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
    /// <summary>
    /// Collection true type to non-polymorphic serializer
    /// </summary>
    private readonly Dictionary<Type, SerializerPair> mySerializers = new Dictionary<Type, SerializerPair>();


    [NotNull] private readonly IScalarSerializers myScalars;

    private readonly object myLock = new object();


#if JET_MODE_ASSERT
    /// <summary>
    /// current serialization stack.
    ///
    /// used to provide diagnostics about circular dependencies only.
    /// </summary>
    private readonly Queue<Type> myCurrentSerializersChain = new Queue<Type>();

#endif

    [NotNull]
    public IScalarSerializers Scalars => myScalars;

    public ISerializersContainer Cache { get; }

    public ReflectionSerializersFactory([NotNull] ITypesCatalog typeCatalog, IScalarSerializers scalars = null, Predicate<Type> blackListChecker = null)
    {
      myScalars = scalars ?? new ScalarSerializer(typeCatalog, blackListChecker);
      Cache = new SerializersContainer(mySerializers);
      Serializers.RegisterFrameworkMarshallers(Cache);
    }


    [NotNull]
    public SerializerPair GetOrRegisterSerializerPair([NotNull] Type type, bool instance = false)
    {
      lock (myLock)
      {
#if JET_MODE_ASSERT
        myCurrentSerializersChain.Clear();
#endif
        return GetOrRegisterStaticSerializerInternal(type, instance);
      }
    }

    private SerializerPair GetOrRegisterStaticSerializerInternal(Type type, bool instance)
    {
      if (ReflectionSerializerVerifier.IsScalar(type))
        return GetOrCreateScalar(type, instance);
      
      // RdModels only
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

      if (instance && !type.IsSealed)
        return GetPolymorphic(type);

      return serializerPair;
    }

    internal SerializerPair GetOrCreateMemberSerializer([NotNull] MemberInfo mi, [NotNull] Type serializerType, bool allowNullable)
    {
      if (!allowNullable)
        ReflectionSerializerVerifier.AssertMemberDeclaration(mi);
      else
        ReflectionSerializerVerifier.AssertDataMemberDeclaration(mi);

      var typeInfo = serializerType.GetTypeInfo();
      var implementingType = ReflectionSerializerVerifier.GetImplementingType(typeInfo);
      var implementingTypeInfo = implementingType.GetTypeInfo();

      if (typeInfo.IsGenericType && !(ReflectionSerializerVerifier.HasRdModelAttribute(typeInfo) || ReflectionSerializerVerifier.HasRdExtAttribute(typeInfo)))
      {
        return CreateGenericSerializer(mi, typeInfo, implementingType);
      }
      else if (ReflectionSerializerVerifier.IsScalar(serializerType))
      {
        return GetOrCreateScalar(serializerType, false);
      }
      else
      {
        var serializerPair = GetOrRegisterStaticSerializerInternal(serializerType, false);
        Assertion.AssertNotNull(serializerPair != null, $"Unable to Create serializer for type {serializerType.ToString(true)}");
        if (serializerPair == null)
        {
#if JET_MODE_ASSERT
          Assertion.Fail($"Unable to create serializer for {serializerType.ToString(true)}: circular dependency detected: {String.Join(" -> ", myCurrentSerializersChain.Select(t => t.ToString(true)).ToArray())}");
#endif
          throw new Assertion.AssertionException($"Undetected circular dependency during serializing {serializerType.ToString(true)}");
        }

        return serializerPair;
      }
    }

    public SerializerPair GetOrCreateScalar(Type serializerType, bool instanceSerializer)
    {
      if (instanceSerializer && myScalars.CanBePolymorphic(serializerType))
      {
        return myScalars.GetInstanceSerializer(serializerType);
      }

      if (!mySerializers.TryGetValue(serializerType, out var serializerPair))
      {
        try
        {
          ReflectionUtil.InvokeGenericThis(this, nameof(RegisterScalar), serializerType);
        }
        catch (Exception e)
        {
#if JET_MODE_ASSERT
          throw new Exception($"Unable to create serializer for {string.Join(" -> ", myCurrentSerializersChain.Select(t => t.ToString(true)).ToArray())}. {e.Message}", e);
#else
          throw new Exception($"Unable to create serializer for {serializerType.ToString(true)}. {e.Message}", e);
#endif
        }
        serializerPair = mySerializers[serializerType];
        if (serializerPair == null)
          Assertion.Fail($"Unable to Create serializer for scalar type {serializerType.ToString(true)}");
        else 
          Assertion.Assert(!serializerPair.IsPolymorphic, "Polymorphic serializer can't be stored in staticSerializers");
        return serializerPair;
      }

      return serializerPair;
    }

    private SerializerPair GetPolymorphic(Type type)
    {
      var polymorphicClass = typeof(Polymorphic<>).MakeGenericType(type);
      var reader = polymorphicClass.GetTypeInfo().GetField("Read", BindingFlags.Public | BindingFlags.Static).NotNull().GetValue(type);
      var writer = polymorphicClass.GetTypeInfo().GetField("Write", BindingFlags.Public | BindingFlags.Static).NotNull().GetValue(type);
      return new SerializerPair(reader, writer);
    }

    private void RegisterScalar<T>()
    {
      myScalars.GetOrCreate<T>(out var reader, out var writer);
      var serializerPair = new SerializerPair(reader, writer);
      Assertion.Assert(!serializerPair.IsPolymorphic, "Polymorphic serializer can't be stored in staticSerializers");
      mySerializers.Add(typeof(T), serializerPair);
    }

    [CanBeNull]
    public static Type GetRpcInterface(TypeInfo typeInfo)
    {
      if (typeInfo.GetCustomAttribute<RdExtAttribute>() is RdExtAttribute rdExt && rdExt.RdRpcInterface != null)
        return rdExt.RdRpcInterface;

      foreach (var @interface in typeInfo.GetInterfaces()) 
        if (ReflectionSerializerVerifier.IsRpcAttributeDefined(@interface)) 
          return @interface;

      return null;
    }

    /// <summary>
    /// Register serializers for either <see cref="RdExtAttribute"/> or <see cref="RdModelAttribute"/>
    /// </summary>
    private void RegisterModelSerializer<T>()
    {
      Assertion.Assert(!ReflectionSerializerVerifier.IsScalar(typeof(T)), "Type {0} should be either RdModel or RdExt.", typeof(T));
      // place null marker to detect circular dependencies
      mySerializers.Add(typeof(T), null);

      TypeInfo typeInfo = typeof(T).GetTypeInfo();
      ReflectionSerializerVerifier.AssertRoot(typeInfo);
      var isScalar = ReflectionSerializerVerifier.IsScalar(typeInfo);
      bool allowNullable = ReflectionSerializerVerifier.HasRdModelAttribute(typeInfo) || (isScalar && ReflectionSerializerVerifier.CanBeNull(typeInfo));

/*      var intrinsicSerializer = TryGetIntrinsicSerializer(typeInfo);
      if (intrinsicSerializer != null)
      {
        mySerializers[typeof(T)] = intrinsicSerializer;
        return;
      }*/

      var memberInfos = SerializerReflectionUtil.GetBindableMembers(typeInfo);
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
        memberDeserializers[index] = SerializerReflectionUtil.ConvertReader(returnType, serPair.Reader);
        memberSerializers[index] = SerializerReflectionUtil.ConvertWriter(returnType, serPair.Writer);
      }

      var type = typeInfo.AsType();

      CtxReadDelegate<T> readerDelegate = (ctx, unsafeReader) =>
      {
        if (allowNullable && !unsafeReader.ReadNullness())
          return default;

        object instance;
        if (isScalar)
        {
          instance = FormatterServices.GetUninitializedObject(type);
        }
        else
        {
          instance = Activator.CreateInstance(type);
        }

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
        if (allowNullable)
        {
          unsafeWriter.Write(value != null);
          if (value == null)
            return;
        }

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

    public SerializerPair CreateGenericSerializer(MemberInfo member, TypeInfo typeInfo, Type implementingType)
    {
      var genericDefinition = implementingType.GetGenericTypeDefinition();

      var intrinsic = Intrinsic.TryGetIntrinsicSerializer(implementingType.GetTypeInfo(), t => GetOrRegisterStaticSerializerInternal(t, true));
      if (intrinsic != null)
        return intrinsic;

      if (typeof(IViewableProperty<>).IsAssignableFrom(genericDefinition))
      {
        return CreateStaticReaderSingleGeneric(member, typeInfo.AsType(), implementingType, allowNullable: true);
      }

      if (genericDefinition == typeof(RdSignal<>) ||
          genericDefinition == typeof(RdList<>) ||
          genericDefinition == typeof(RdSet<>))
      {
        return CreateStaticReaderSingleGeneric(member, typeInfo.AsType(), implementingType, allowNullable: false);
      }

      if (genericDefinition == typeof(RdMap<,>))
      {
        return CreateStaticReaderTwoGeneric(member, typeInfo.AsType(), implementingType);
      }

      throw new Exception($"Unable to register generic type: {typeInfo}");
    }

    private SerializerPair CreateStaticReaderSingleGeneric(MemberInfo memberInfo, Type type, Type implementingType, bool allowNullable)
    {
      var argumentType = type.GetTypeInfo().GetGenericArguments()[0];

      var readPropertyMethod = SerializerReflectionUtil.GetReadStaticSerializer(implementingType.GetTypeInfo(), argumentType);
      var writePropertyMethod = SerializerReflectionUtil.GetWriteStaticDeserializer(implementingType.GetTypeInfo());

      var argumentSerializerPair = GetOrCreateMemberSerializer(memberInfo, argumentType, true);
      return SerializerPair.CreateFromMethods(readPropertyMethod, writePropertyMethod, argumentSerializerPair);
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

      var writeStaticDeserializer = SerializerReflectionUtil.GetWriteStaticDeserializer(implementingType.GetTypeInfo());

      var keySerializer = GetOrCreateMemberSerializer(memberInfo, keyType, true);
      var valueSerializer = GetOrCreateMemberSerializer(valueType, valueType, true);
      return SerializerPair.CreateFromMethods(readPropertyMethod, writeStaticDeserializer, keySerializer, valueSerializer);
    }


    private class SerializersContainer : ISerializersContainer
    {
      private readonly IDictionary<Type, SerializerPair> myStore;

      public SerializersContainer(IDictionary<Type, SerializerPair> store)
      {
        myStore = store;
      }

      public void Register<T>(CtxReadDelegate<T> reader, CtxWriteDelegate<T> writer, long? predefinedType = null)
      {
        myStore[typeof(T)] = new SerializerPair(reader, writer);
      }

      public void RegisterEnum<T>() where T :
#if !NET35
    unmanaged, 
#endif
        Enum
      {
      }

      public void RegisterToplevelOnce(Type toplevelType, Action<ISerializers> registerDeclaredTypesSerializers)
      {
      }
    }
  }

  public class SerializerPair
  {
    private readonly object myReader;
    private readonly object myWriter;

    public object Reader => myReader;

    public object Writer => myWriter;

    public bool IsPolymorphic
    {
      get
      {
        var ctxReadDelegate = (Delegate) myReader;
        return ctxReadDelegate.Method.DeclaringType?.DeclaringType?.Name.Contains(nameof(Polymorphic)) == true;
      }
    }

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
      return (SerializerPair) ReflectionUtil.InvokeStaticGeneric(typeof(SerializerPair), nameof(CreateFromMethods2), readMethod.ReturnType, readMethod, writeMethod);
    }

    /// <summary>
    /// Create serializer from Read  and Write method without <see cref="SerializationCtx"/>
    /// </summary>
    public static SerializerPair CreateFromNonProtocolMethods(MethodInfo readMethod, MethodInfo writeMethod)
    {
      return (SerializerPair) ReflectionUtil.InvokeStaticGeneric(typeof(SerializerPair), nameof(CreateFromNonProtocolMethodsT), readMethod.ReturnType, readMethod, writeMethod);
    }

    public static SerializerPair CreateFromMethods2<T>(MethodInfo readMethod, MethodInfo writeMethod)
    {
      void WriterDelegate(SerializationCtx ctx, UnsafeWriter writer, T value) =>
        writeMethod.Invoke(null, new[] { ctx, writer, (object)value, });

      T ReaderDelegate(SerializationCtx ctx, UnsafeReader reader) =>
        (T) readMethod.Invoke(null, new object[] { ctx, reader });

      CtxReadDelegate<T> ctxReadDelegate = ReaderDelegate;
      CtxWriteDelegate<T> ctxWriteDelegate = WriterDelegate;
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

    public static SerializerPair FromMarshaller<T>(IIntrinsicMarshaller<T> marshaller)
    {
      CtxReadDelegate<T> ctxReadDelegate = marshaller.Read;
      CtxWriteDelegate<T> ctxWriteDelegate = marshaller.Write;
      return new SerializerPair(ctxReadDelegate, ctxWriteDelegate);
    }

    private static SerializerPair CreateFromNonProtocolMethodsT<T>(MethodInfo readMethod, MethodInfo writeMethod)
    {
      Assertion.Assert(readMethod.IsStatic, $"Read method should be static ({readMethod.DeclaringType.ToString(true)})");
      Assertion.Assert(!writeMethod.IsStatic, $"Read method should not be static ({readMethod.DeclaringType.ToString(true)})");
      
      void WriterDelegate(SerializationCtx ctx, UnsafeWriter writer, T value)
      {
        if (!typeof(T).IsValueType && !writer.WriteNullness(value as object))
          return;
        writeMethod.Invoke(value, new object[] {writer});
      }

      T ReaderDelegate(SerializationCtx ctx, UnsafeReader reader)
      {
        if (!typeof(T).IsValueType && !reader.ReadNullness())
          return default;

        return (T) readMethod.Invoke(null, new object[] {reader});
      }

      CtxReadDelegate<T> ctxReadDelegate = ReaderDelegate;
      CtxWriteDelegate<T> ctxWriteDelegate = WriterDelegate;
      return new SerializerPair(ctxReadDelegate, ctxWriteDelegate);
    }

    public static SerializerPair Polymorphic(Type type)
    {
      var poly = typeof(Polymorphic<>).MakeGenericType(type);
      var reader = poly.GetField(nameof(Polymorphic<int>.Read), BindingFlags.Static | BindingFlags.Public).NotNull().GetValue(null);
      var writer = poly.GetField(nameof(Polymorphic<int>.Write), BindingFlags.Static | BindingFlags.Public).NotNull().GetValue(null);
      return new SerializerPair(reader, writer);
    }
  }

  public interface IScalarSerializers
  {
    /// <summary>
    /// Return static serializers for type Static serializer is the serializer which always return provided type and
    /// never any of the inheritors. It makes sense to ask these serializers for struct, enums and sealed classes
    /// </summary>
    /// <param name="type"></param>
    /// <returns></returns>
    SerializerPair GetOrCreate(Type type);

    /// <summary>
    /// Return instance serializers for the type.
    /// Instance means that Polymorphic types is possible, you can ask here for serializer for interface, for example
    /// </summary>
    /// <param name="t"></param>
    /// <returns></returns>
    SerializerPair GetInstanceSerializer(Type t);

    /// <summary>
    /// Register custom serializer for provided polymorphic type. It will be used instead of default <see
    /// cref="Polymorphic{T}"/>. Be aware, that you can register your custom serializer only before any serializer was
    /// asked via <see cref="GetInstanceSerializer"/>.
    /// </summary>
    /// <param name="type"></param>
    /// <param name="serializers"></param>
    void RegisterPolymorphicSerializer([NotNull] Type type, SerializerPair serializers);

    void GetOrCreate<T>(out CtxReadDelegate<T> reader, out CtxWriteDelegate<T> writer);

    bool CanBePolymorphic(Type type);
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