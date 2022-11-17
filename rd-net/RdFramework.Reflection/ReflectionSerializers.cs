using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Runtime.Serialization;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Util;
using JetBrains.Util.Util;

namespace JetBrains.Rd.Reflection;

#if NET35
using TypeInfo = System.Type;
#endif

/// <summary>
/// Creates and provides access to Reflection-generated serializers for Rd, thread safe
/// </summary>
public class ReflectionSerializers
{
  /// <summary>
  /// Collection true type to non-polymorphic serializer (it is called static serializers)
  /// </summary>
  private readonly Dictionary<Type, SerializerPair> mySerializers = new();


  private readonly IScalarSerializers myScalars;

  private readonly object myLock = new object();


  /// <summary>
  /// current serialization stack.
  ///
  /// used to provide diagnostics about circular dependencies only.
  /// </summary>
  private readonly Queue<Type> myCurrentSerializersChain = new Queue<Type>();

  public IScalarSerializers Scalars => myScalars;

  public ReflectionSerializers(ITypesCatalog typeCatalog, IScalarSerializers? scalars = null, Predicate<Type>? blackListChecker = null)
  {
    myScalars = scalars ?? new ScalarSerializer(typeCatalog, blackListChecker);
  }

  public SerializerPair GetOrRegisterSerializerPair(Type type, bool instance = false)
  {
    lock (myLock)
    {
      if (Mode.IsAssertion)
        myCurrentSerializersChain.Clear();
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
      if (Mode.IsAssertion)
        myCurrentSerializersChain.Enqueue(type);

      using (new FirstChanceExceptionInterceptor.ThreadLocalDebugInfo(type))
      {
        if (ReflectionSerializerVerifier.HasIntrinsic(type.GetTypeInfo()))
        {
          var intrinsic = Intrinsic.TryGetIntrinsicSerializer(
            type.GetTypeInfo(), 
            t => GetOrRegisterStaticSerializerInternal(t, true));
          Assertion.Assert(intrinsic != null, "Unable to get intrinsic serializer for type {0}, thought API detect the presense of it. Probably it was only partially implemented", type);
          mySerializers.Add(type, intrinsic);
        }
        else
        {
          ReflectionUtil.InvokeGenericThis(this, nameof(RegisterModelSerializer), type);
        }
      }

      if (Mode.IsAssertion)
        myCurrentSerializersChain.Dequeue();

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

  internal SerializerPair GetOrCreateMemberSerializer(Type serializerType, bool allowNullable, bool instanceSerializer, MemberInfo? mi)
  {
    if (mi != null && !ReflectionSerializerVerifier.HasIntrinsic(serializerType.GetTypeInfo()))
    {
      if (!allowNullable)
        ReflectionSerializerVerifier.AssertMemberDeclaration(mi);
      else
        ReflectionSerializerVerifier.AssertDataMemberDeclaration(mi);
    }

    var serializerTypeInfo = serializerType.GetTypeInfo();
    var implementingTypeInfo = ReflectionSerializerVerifier.GetImplementingType(serializerTypeInfo).GetTypeInfo();

    if (serializerTypeInfo.IsGenericType && !(ReflectionSerializerVerifier.HasRdModelAttribute(serializerTypeInfo) || ReflectionSerializerVerifier.HasRdExtAttribute(serializerTypeInfo))
                                         && !ReflectionSerializerVerifier.IsScalar(implementingTypeInfo))
    {
      return CreateGenericSerializer(serializerTypeInfo, implementingTypeInfo);
    }
    else if (ReflectionSerializerVerifier.IsScalar(serializerType))
    {
      return GetOrCreateScalar(serializerType, instanceSerializer);
    }
    else
    {
      var serializerPair = GetOrRegisterStaticSerializerInternal(serializerType, instanceSerializer);
      Assertion.AssertNotNull(serializerPair, $"Unable to Create serializer for type {serializerType.ToString(true)}");
      if (serializerPair == null)
      {
        if (Mode.IsAssertion)
          Assertion.Fail($"Unable to create serializer for {serializerType.ToString(true)}: circular dependency detected: {String.Join(" -> ", myCurrentSerializersChain.Select(t => Types.ToString(t, true)).ToArray())}");
        throw new Assertion.AssertionException($"Undetected circular dependency during serializing {serializerType.ToString(true)}. Enable Assertion mode to get detailed information.");
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
      using (new FirstChanceExceptionInterceptor.ThreadLocalDebugInfo(serializerType.FullName))
        ReflectionUtil.InvokeGenericThis(this, nameof(RegisterScalar), serializerType);
     
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

  /// <summary>
  /// Register serializers for either <see cref="RdExtAttribute"/> or <see cref="RdModelAttribute"/>
  /// </summary>
  private void RegisterModelSerializer<T>()
  {
    Assertion.Assert(!ReflectionSerializerVerifier.IsScalar(typeof(T)), "Type {0} should be either RdModel or RdExt.", typeof(T));
    // place null marker to detect circular dependencies
    mySerializers.Add(typeof(T), null!);

    var typeInfo = typeof(T).GetTypeInfo();
    ReflectionSerializerVerifier.AssertRoot(typeInfo);
    var isScalar = ReflectionSerializerVerifier.IsScalar(typeInfo);
    bool allowNullable = ReflectionSerializerVerifier.HasRdModelAttribute(typeInfo) || (isScalar && ReflectionSerializerVerifier.CanBeNull(typeInfo));

/*      var intrinsicSerializer = TryGetIntrinsicSerializer(typeInfo);
      if (intrinsicSerializer != null)
      {
        mySerializers[typeof(T)] = intrinsicSerializer;
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
      var serPair = GetOrCreateMemberSerializer(serializerType: returnType, allowNullable: allowNullable, instanceSerializer: false, mi: mi);
      memberDeserializers[index] = SerializerReflectionUtil.ConvertReader(returnType, serPair.Reader);
      memberSerializers[index] = SerializerReflectionUtil.ConvertWriter(returnType, serPair.Writer);
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

      return (T) instance;
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

    mySerializers[type] = new SerializerPair(readerDelegate, writerDelegate);
  }

  private SerializerPair CreateGenericSerializer(TypeInfo type, TypeInfo implementation)
  {
    var intrinsic = Intrinsic.TryGetIntrinsicSerializer(implementation, t => GetOrRegisterStaticSerializerInternal(t, true));
    if (intrinsic != null)
    {
      if (type != implementation)
      {
        return new SerializerPair(
          SerializerReflectionUtil.ConvertReader(implementation, intrinsic.Reader),
          SerializerReflectionUtil.ConvertWriter(implementation, intrinsic.Writer));
      }
      return intrinsic;
    }

    throw new Exception($"Unable to register generic type: {type}. Generics types are expected to have Read and Write static methods for serialization.");
  }
}