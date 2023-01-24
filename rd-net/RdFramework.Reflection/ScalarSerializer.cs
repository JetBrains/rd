using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Expressions;
using System.Reflection;
using System.Runtime.Serialization;
using JetBrains.Diagnostics;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Util;
using JetBrains.Util.Util;

#if NET35
using TypeInfo = System.Type;
#endif

namespace JetBrains.Rd.Reflection
{
  public class ScalarSerializer : IScalarSerializers
  {
    /// <summary>
    /// Types catalog required for providing information about statically discovered types during concrete serializer
    /// construction for sake of possibility for Rd serializers to lookup real type by representing RdId
    /// </summary>
    private readonly ITypesCatalog? myTypesCatalog;

    /// <summary>
    /// Black listed type. Any attempt to create serializer for these types should throw exception.
    /// Used to prevent attempts to pass an object which is well-known as non-serializable.
    /// For example, any component of tree-like structure or object graph should not be passed to
    /// serializer
    ///
    /// This predicate should return true only for blacklisted type
    /// </summary>
    private readonly Predicate<Type> myBlackListChecker;

    public ScalarSerializer(ITypesCatalog? typesCatalog, Predicate<Type>? blackListChecker = null)
    {
      myTypesCatalog = typesCatalog;
      myBlackListChecker = blackListChecker ?? (_ => false);
    }

    /// <summary>
    /// Creates static serializers for type
    /// </summary>
    /// <param name="type"></param>
    /// <param name="serializers"></param>
    /// <returns></returns>
    public SerializerPair CreateSerializer(Type type, ISerializersSource serializers)
    {
      if (type == typeof(IntPtr))
      {
        throw new ArgumentException($"Unable to serialize {type.ToString(true)}. Platform-specific types cannot be serialized.");
      }

      if (typeof(Delegate).IsAssignableFrom(type))
      {
        throw new ArgumentException($"Unable to serialize {type.ToString(true)}. Delegates cannot be serialized.");
      }

      if (myBlackListChecker(type))
      {
        Assertion.Fail($"Attempt to create serializer for black-listed type: {type.ToString(true)}");
      }


      var typeInfo = type.GetTypeInfo();

      var intrinsic = Intrinsic.TryGetIntrinsicSerializer(typeInfo, t1 => serializers.GetOrRegisterSerializerPair(t1, true));
      if (intrinsic != null)
      {
        myTypesCatalog?.AddType(type);
        return intrinsic;
      }

      else if (type.IsEnum)
      {
        var serializer = ReflectionUtil.InvokeGenericThis(this, nameof(CreateEnumSerializer), type);
        return (SerializerPair) serializer!;
      }
      else if (ReflectionSerializerVerifier.IsValueTuple(typeInfo))
      {
        return (SerializerPair) ReflectionUtil.InvokeGenericThis(this, nameof(CreateValueTupleSerializer), type, new object?[] { serializers })!;
      }
      else if (typeInfo.IsGenericType && typeInfo.GetGenericTypeDefinition() == typeof(Nullable<>))
      {
        var genericTypeArgument = typeInfo.GetGenericArguments()[0];
        var nullableSerializer = (SerializerPair) ReflectionUtil.InvokeGenericThis(this, nameof(RegisterNullable), genericTypeArgument, new object?[] { serializers })!;
        return nullableSerializer;
        // return CreateGenericSerializer(member, typeInfo, implementingType, implementingTypeInfo);
      }
      else
      {
        myTypesCatalog?.AddType(type);
        var serializer = ReflectionUtil.InvokeGenericThis(this, nameof(CreateCustomScalar), type, new object[] { serializers });
        return (SerializerPair) serializer!;
      }
    }

    private SerializerPair CreateCustomScalar<T>(ISerializersSource serializers)
    {
      if (typeof(IRdBindable).IsAssignableFrom(typeof(T)))
        Assertion.Fail($"Invalid scalar type: {typeof(T).ToString(true)}. Scalar types cannot be IRdBindable.");
      if (typeof(T).IsInterface || typeof(T).IsAbstract)
        Assertion.Fail($"Invalid scalar type: {typeof(T).ToString(true)}. Scalar types should be concrete types.");

      TypeInfo typeInfo = typeof(T).GetTypeInfo();
      var allowNullable = ReflectionSerializerVerifier.CanBeNull(typeInfo);

      var memberInfos = SerializerReflectionUtil.GetBindableFields(typeInfo);
      var memberSetters = memberInfos.Select(ReflectionUtil.GetSetter).ToArray();
      var memberGetters = memberInfos.Select(ReflectionUtil.GetGetter).ToArray();

      // todo: consider using IL emit
      CtxReadDelegate<object>[]? memberDeserializers = null;
      CtxWriteDelegate<object>[]? memberSerializers = null;

      CtxReadDelegate<T?> readerDelegate = (ctx, unsafeReader) =>
      {
        if (memberDeserializers == null)
          using (new FirstChanceExceptionInterceptor.ThreadLocalDebugInfo(typeof(T)))
            InitMemberSerializers();
        Assertion.AssertNotNull(memberDeserializers);

        if (allowNullable && !unsafeReader.ReadNullness())
          return default;

        object instance = FormatterServices.GetUninitializedObject(typeof(T));

        try
        {
          for (var index = 0; index < memberDeserializers.Length; index++)
          {
            var memberValue = memberDeserializers[index](ctx, unsafeReader);
            memberSetters[index](instance, memberValue);
          }
        }
        catch (ArgumentException e)
        {
          e.Data["Type:" + typeof(T).ToString(true)] = "";
          throw;
        }

        return (T) instance;
      };

      CtxWriteDelegate<T> writerDelegate = (ctx, unsafeWriter, value) =>
      {
        if (memberSerializers == null)
          using (new FirstChanceExceptionInterceptor.ThreadLocalDebugInfo(typeof(T)))
            InitMemberSerializers();
        Assertion.AssertNotNull(memberSerializers);

        if (allowNullable)
        {
          unsafeWriter.Write(value != null);
          if (value == null)
            return;
        }

        try
        {
          for (var i = 0; i < memberSerializers.Length; i++)
          {
            var memberValue = memberGetters[i](value!);
            memberSerializers[i](ctx, unsafeWriter, memberValue!);
          }
        }
        catch (ArgumentException e)
        {
          e.Data["Type:" + typeof(T).ToString(true)] = "";
          throw;
        }
      };

      return new SerializerPair(readerDelegate, writerDelegate);

      // Lazy resolve cyclic depencencies and give ability to serialize tree-like structures.
      void InitMemberSerializers()
      {
        var read = new CtxReadDelegate<object>[memberInfos.Length];
        var write = new CtxWriteDelegate<object>[memberInfos.Length];

        for (var index = 0; index < memberInfos.Length; index++)
        {
          var mi = memberInfos[index];
          var returnType = ReflectionUtil.GetReturnType(mi);
          var serPair = serializers.GetOrRegisterSerializerPair(returnType, true);
          read[index] = SerializerReflectionUtil.ConvertReader<object>(serPair.Reader);
          write[index] = SerializerReflectionUtil.ConvertWriter<object>(serPair.Writer);
        }

        memberSerializers = write;
        memberDeserializers = read;
      }
    }

    private SerializerPair CreateEnumSerializer<T>()
    {
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

      return result;
    }

    private SerializerPair RegisterNullable<T>(ISerializersSource serializers) where T : struct
    {
      // nullable can be only for value tuple, no need to aks for serializers serializer here
      var serPair = CreateSerializer(typeof(T), serializers);
      var ctxReadDelegate = serPair.GetReader<T>();
      var ctxWriteDelegate = serPair.GetWriter<T>();
      return new SerializerPair(ctxReadDelegate.NullableStruct(), ctxWriteDelegate.NullableStruct());
    }


    /// <summary>
    /// Register serializer for ValueTuples
    /// </summary>
    /// <param name="serializers"></param>
    private SerializerPair CreateValueTupleSerializer<T>(ISerializersSource serializers)
    {
      TypeInfo typeInfo = typeof(T).GetTypeInfo();
      ReflectionSerializerVerifier.AssertRoot(typeInfo);

      var argumentTypes = typeInfo.GetGenericArguments();

      var memberGetters = typeInfo.GetFields().Select(ReflectionUtil.GetGetter).ToArray();

      var memberDeserializers = new CtxReadDelegate<object>[argumentTypes.Length];
      var memberSerializers = new CtxWriteDelegate<object?>[argumentTypes.Length];
      for (var index = 0; index < argumentTypes.Length; index++)
      {
        var argumentType = argumentTypes[index];
        var serPair = serializers.GetOrRegisterSerializerPair(argumentType, true);
        memberDeserializers[index] = SerializerReflectionUtil.ConvertReader<object>(serPair.Reader);
        memberSerializers[index] = SerializerReflectionUtil.ConvertWriter<object?>(serPair.Writer);
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
        // nrt suppression: value tuple cannot be null
        for (var i = 0; i < argumentTypes.Length; i++)
        {
          var memberValue = memberGetters[i](value!);
          memberSerializers[i](ctx, unsafeWriter, memberValue);
        }
      };

      return new SerializerPair(readerDelegate, writerDelegate);
    }
  }
}