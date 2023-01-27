using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Runtime.Serialization;
using JetBrains.Collections;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;
using JetBrains.Util;
using JetBrains.Util.Util;
using static System.String;

namespace JetBrains.Rd.Reflection;

#if NET35
using TypeInfo = System.Type;
#endif

/// <summary>
/// Creates and provides access to Reflection-generated serializers for Rd, thread safe
/// </summary>
public class ReflectionSerializers : ISerializers, ISerializersSource
{
  /// <summary>
  /// Collection static serializers (serializers is not possible here! Only instance serializer can be serializers)
  /// </summary>
  private readonly Dictionary<Type, SerializerPair> myStaticSerializers = new();

  /// <summary>
  /// Collection of specific serializers serializers and user-registred custom serializers.
  /// User registred serializers should be added before activating any other serializers serializer to guarantee
  /// consistency of serializers across all Rd objects.
  ///
  /// Techincally, this restriction can be lifted to lazy initialization. It only exists to reduce the
  /// amount of races in consumers.
  /// </summary>
  private readonly Dictionary<Type, SerializerPair> myInstanceSerializers = new();

  /// <summary>
  /// A flag to enforce consistency of serializers. New specific poly serializer can't be registered after first query
  /// of serializers serializer from outer world.
  /// </summary>
  private bool myPolySerializersSealed = false;

  private readonly ITypesCatalog myCatalog;

  private readonly IScalarSerializers myScalars;

  private readonly object myLock = new object();


  /// <summary>
  /// current serialization stack.
  ///
  /// used to provide diagnostics about circular dependencies only.
  /// </summary>
  private readonly Queue<Type> myCurrentSerializersChain = new Queue<Type>();

  public IScalarSerializers Scalars => myScalars;

  /// <summary>
  /// An extension point to override lazy creation of serializers instance serializers.
  /// You add your custom serializer for generic types via this extension point.
  /// You can also use <see cref="Register"/> method ahead of time when lazy initialization isn't necessary.
  /// </summary>
  public ISignal<Type> BeforeCreation { get; } = new Signal<Type>();

  public ReflectionSerializers(ITypesCatalog typeCatalog, IScalarSerializers? scalars = null, Predicate<Type>? blackListChecker = null, bool withExtensions = true)
  {
    myCatalog = typeCatalog;
    myScalars = scalars ?? new ScalarSerializer(typeCatalog, blackListChecker);
    Serializers.RegisterFrameworkMarshallers(this);
    
    if (withExtensions)
    {
      ScalarCollectionExtension.AttachCollectionSerializers(this);
    }
  }

  public SerializerPair GetOrRegisterSerializerPair(Type type, bool instance = false)
  {
    lock (myLock)
    {
      var implementingType = ReflectionSerializerVerifier.GetImplementingType(type.GetTypeInfo()).GetTypeInfo();
      var isRdType = implementingType != type;

      if (!isRdType && instance && SerializerReflectionUtil.CanBePolymorphic(type))
      {
        myCatalog.AddType(type);
        if (myInstanceSerializers.TryGetValue(type, out var pair))
          return pair;

        BeforeCreation.Fire(type);

        if (myInstanceSerializers.TryGetValue(type, out pair))
          return pair;

        return GetPolymorphic(type);
      }

      if (!myStaticSerializers.TryGetValue(type, out var serializerPair))
      {
        using var info = new FirstChanceExceptionInterceptor.ThreadLocalDebugInfo(type);

        myCatalog.AddType(type);
        
        BeforeCreation.Fire(type);

        if (myStaticSerializers.TryGetValue(type, out serializerPair))
          return serializerPair;

        if (Mode.IsAssertion) myCurrentSerializersChain.Enqueue(type);
        try
        {
          myStaticSerializers.Add(type, null!);

          if (isRdType)
          {
            var intrinsic = Intrinsic.TryGetIntrinsicSerializer(implementingType, t => GetOrRegisterSerializerPair(t, true));
            Assertion.Assert(intrinsic != null, "Unable to get intrinsic serializer for type {0}, thought it should be implemented for Rd-types.", type);
            var pair = SerializerReflectionUtil.ConvertPair(intrinsic, type);

            myStaticSerializers[type] = pair;
          }
          else if (ReflectionSerializerVerifier.IsScalar(type))
          {
            myStaticSerializers[type] = CreateScalar(type, instance);
          }
          else if (ReflectionSerializerVerifier.HasIntrinsic(type.GetTypeInfo()))
          {
            var intrinsic = Intrinsic.TryGetIntrinsicSerializer(
              type.GetTypeInfo(),
              t => GetOrRegisterSerializerPair(t, true));
            Assertion.Assert(intrinsic != null,
              "Unable to get intrinsic serializer for type {0}, thought API detect the presense of it. Probably it was only partially implemented",
              type);
            myStaticSerializers[type] = intrinsic;
          }
          else
          {
            ReflectionUtil.InvokeGenericThis(this, nameof(RegisterModelSerializer), type);
          }
        }
        finally
        {
          if (Mode.IsAssertion) myCurrentSerializersChain.Dequeue();
        }

        if (!myStaticSerializers.TryGetValue(type, out serializerPair))
        {
          throw new KeyNotFoundException($"Unable to register type {type.ToString(true)}: serializer can't be found");
        }
      }

      if (Mode.IsAssertion && serializerPair == null)
        Assertion.Fail(
          $"Unable to create serializer for {type.ToString(true)}: circular dependency detected: {Join(" -> ", myCurrentSerializersChain.Select(t => t.ToString(true)).ToArray())}");
      else
        Assertion.AssertNotNull(serializerPair,
          $"Unable to register type: {type.ToString(true)}, undetected circular dependency. Enable Mode.IsAssertion to get the cycle.");

      return serializerPair;
    }
  }

  private SerializerPair CreateScalar(Type serializerType, bool instanceSerializer)
  {
    if (instanceSerializer && SerializerReflectionUtil.CanBePolymorphic(serializerType))
    {
      return GetPolymorphic(serializerType);
    }

    var pair = myScalars.CreateSerializer(serializerType, this);
    if (pair == null)
      Assertion.Fail($"Unable to Create serializer for scalar type {serializerType.ToString(true)}");
    else
      Assertion.Assert(!pair.IsPolymorphic, "Polymorphic serializer can't be stored in staticSerializers");

    return pair;
  }

  /// <summary>
  /// Register serializers for either <see cref="RdExtAttribute"/> or <see cref="RdModelAttribute"/>
  /// </summary>
  private void RegisterModelSerializer<T>()
  {
    Assertion.Assert(!ReflectionSerializerVerifier.IsScalar(typeof(T)), "Type {0} should be either RdModel or RdExt.", typeof(T));

    var typeInfo = typeof(T).GetTypeInfo();
    ReflectionSerializerVerifier.AssertRoot(typeInfo);
    var isScalar = ReflectionSerializerVerifier.IsScalar(typeInfo);
    bool allowNullable = ReflectionSerializerVerifier.HasRdModelAttribute(typeInfo) || (isScalar && ReflectionSerializerVerifier.CanBeNull(typeInfo));

    /*      var intrinsicSerializer = TryGetIntrinsicSerializer(typeInfo);
          if (intrinsicSerializer != null)
          {
            myStaticSerializers[typeof(T)] = intrinsicSerializer;
            return;
          }*/

    var memberInfos = SerializerReflectionUtil.GetBindableFields(typeInfo);
    var memberSetters = memberInfos.Select(ReflectionUtil.GetSetter).ToArray();
    var memberGetters = memberInfos.Select(ReflectionUtil.GetGetter).ToArray();

    // todo: consider using IL emit
    var memberDeserializers = new CtxReadDelegate<object>[memberInfos.Length];
    var memberSerializers = new CtxWriteDelegate<object?>[memberInfos.Length];
    for (var index = 0; index < memberInfos.Length; index++)
    {
      var mi = memberInfos[index];
      var returnType = ReflectionUtil.GetReturnType(mi);
      var serPair = GetOrRegisterSerializerPair(returnType);
      memberDeserializers[index] = SerializerReflectionUtil.ConvertReader<object>(serPair.Reader);
      memberSerializers[index] = SerializerReflectionUtil.ConvertWriter<object?>(serPair.Writer);
    }

    var type = typeInfo.AsType();

    CtxReadDelegate<T?> readerDelegate = (ctx, unsafeReader) =>
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

      return (T)instance;
    };

    CtxWriteDelegate<T?> writerDelegate = (ctx, unsafeWriter, value) =>
    {
      if (allowNullable)
      {
        unsafeWriter.Write(value != null);
        if (value == null)
          return;
      }

      if (value == null)
        Assertion.Fail("Type {0} isn't expected to be null", typeof(T));

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

    myStaticSerializers[type] = new SerializerPair(readerDelegate, writerDelegate);
  }

  private SerializerPair CreateGenericSerializer(TypeInfo type, TypeInfo implementation)
  {
    var intrinsic = Intrinsic.TryGetIntrinsicSerializer(implementation, t => GetOrRegisterSerializerPair(t, true));
    if (intrinsic != null)
    {
      if (type != implementation)
      {
        return new SerializerPair(intrinsic.Reader, intrinsic.Writer);
      }
      return intrinsic;
    }

    throw new Exception($"Unable to register generic type: {type}. Generics types are expected to have Read and Write static methods for serialization.");
  }

  /// <summary>
  /// Register custom serializer for provided serializers type. It will be used instead of default <see
  /// cref="Polymorphic{T}"/>. Be aware, that you can register your custom serializer only before any serializer was
  /// asked via <see cref="GetInstanceSerializer"/>.
  /// </summary>
  public void Register<T>(CtxReadDelegate<T> reader, CtxWriteDelegate<T> writer, long? predefinedId = null)
  {
    var pair = new SerializerPair(reader, writer, false);
    Register(typeof(T), pair);
  }

  public void Register(Type type, SerializerPair pair)
  {
    myCatalog.AddType(type); // predefined type intentionally isn't used. RdId defined by FQN is used even for framework types (like int, string etc).
    lock (myLock)
    {
      if (SerializerReflectionUtil.CanBePolymorphic(type))
      {
        Assertion.Assert(!myStaticSerializers.ContainsKey(type),
          $"Unable to register serializers serializer: a static serializer for type {type.ToString(true)} already exists");
        Assertion.Assert(!myPolySerializersSealed,
          $"Unable to register serializers serializer for type {type.ToString(true)}. It is too late to register a serializers serializer as one or more models were already activated.");

        myInstanceSerializers.Add(type, pair);
      }
      else
      {
        myStaticSerializers.Add(type, pair);
      }
    }
  }

  public void RegisterEnum<T>() where T :
#if !NET35
    unmanaged, 
#endif
    Enum
  {
    // enums are static sized, so no need for additional registration
  }

  public void RegisterToplevelOnce(Type toplevelType, Action<ISerializers> registerDeclaredTypesSerializers)
  {
    // throw new NotImplementedException();
    if (typeof(RdExtReflectionBindableBase).IsAssignableFrom(toplevelType))
    {
      
    }
  }

  public T? Read<T>(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<T>? unknownInstanceReader = null)
  {
    var ctxReadDelegate = GetOrRegisterSerializerPair(typeof(T), true).GetReader<T>();
    return ctxReadDelegate(ctx, reader);
  }

  public void Write<T>(SerializationCtx ctx, UnsafeWriter writer, T value)
  {
    var ctxWriteDelegate = GetOrRegisterSerializerPair(typeof(T), true).GetWriter<T>();
    ctxWriteDelegate(ctx, writer, value);
  }

  public SerializerPair GetPolymorphic(Type type)
  {
    return ReflectionUtil.Call<SerializerPair>(ourGetPolymorphicGenericMethodInfo.MakeGenericMethod(type), this)!;
  }

  private static readonly MethodInfo ourGetPolymorphicGenericMethodInfo = typeof(ReflectionSerializers).GetMethods()
    .Single(m => String.Equals(m.Name, nameof(GetPolymorphic), StringComparison.Ordinal) && m.GetGenericArguments().Length == 1);
  public SerializerPair GetPolymorphic<T>()
  {
    if (Mode.IsAssertion) Assertion.Assert(SerializerReflectionUtil.CanBePolymorphic(typeof(T)));

    CtxReadDelegate<T?> reader = ReadPolymorphic<T>;
    CtxWriteDelegate<T> writer = WritePolymorphic;

    return new SerializerPair(reader, writer, isPolymorphic: true);
  }

  private T? ReadPolymorphic<T>(SerializationCtx ctx, UnsafeReader reader)
  {
    var typeId = RdId.Read(reader);
    if (typeId.IsNil)
      return default;

    var type = myCatalog.GetById(typeId);
    if (type == null)
    {
      myCatalog.AddType(typeof(T));
      type = myCatalog.GetById(typeId);
      if (type == null)
        throw new KeyNotFoundException($"Unknown inheritor from base type '{typeof(T).FullName}', RdId = {typeId}. All types which participate in RdReflection communications should be explicitly known to TypeCatalog.");
    }

    var serializers = GetOrRegisterSerializerPair(type, false);

    var ctxReadDelegate = serializers.GetReader<T>();
    return ctxReadDelegate(ctx, reader);
  }

  private void WritePolymorphic<T>(SerializationCtx ctx, UnsafeWriter writer, T value)
  {
    if (value == null)
    {
      RdId.Nil.Write(writer);
      return;
    }

    var type = value.GetType();
    var serializers = GetOrRegisterSerializerPair(type, false);

    var typeId = myCatalog.GetByType(type);
    typeId.Write(writer);

    var ctxWrite = SerializerReflectionUtil.ConvertWriter<T>(serializers.Writer); // TODO: cache?
    ctxWrite(ctx, writer, value);
  }
}