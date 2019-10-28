using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Expressions;
using System.Reflection;
using System.Runtime.Serialization;
using JetBrains.Annotations;
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
  public class ScalarSerializer : IScalarSerializers, ISerializersContainer
  {
    /// <summary>
    /// Types catalog required for providing information about statically discovered types during concrete serializer
    /// construction for sake of possibility for Rd serializers to lookup real type by representing RdId
    /// </summary>
    private readonly IPolymorphicTypesCatalog myTypesCatalog;

    /// <summary>
    /// Collection instance-specific serializers (polymorphic possible)
    /// </summary>
    private readonly Dictionary<Type, SerializerPair> myStaticSerializers = new Dictionary<Type, SerializerPair>();

    public ScalarSerializer([NotNull] IPolymorphicTypesCatalog typesCatalog)
    {
      myTypesCatalog = typesCatalog ?? throw new ArgumentNullException(nameof(typesCatalog));
      Serializers.RegisterFrameworkMarshallers(this);
    }

    /// <summary>
    /// Return static serializers for type
    /// </summary>
    /// <param name="type"></param>
    /// <returns></returns>
    private SerializerPair GetScalarSerializer(Type type)
    {
      if (myStaticSerializers.TryGetValue(type, out var pair))
      {
        return pair;
      }

      var result = CreateSerializer(type);
      myStaticSerializers[type] = result;
      return result;

      SerializerPair CreateSerializer(Type t)
      {
        var typeInfo = t.GetTypeInfo();

        var intrinsic = TryGetIntrinsicSerializer(typeInfo);
        if (intrinsic != null)
        {
          myTypesCatalog.AddType(type);
          return intrinsic;
        }

        if (IsList(t))
        {
          var genericTypeArgument = t.GetGenericArguments()[0];
          var argumentTypeSerializerPair = GetInstanceSerializer(genericTypeArgument);
          return (SerializerPair) ReflectionUtil.InvokeStaticGeneric(typeof(CollectionSerializers), nameof(CollectionSerializers.CreateListSerializerPair), genericTypeArgument, argumentTypeSerializerPair);
        }
        else if (t.IsArray)
        {
          return (SerializerPair) ReflectionUtil.InvokeGenericThis(this, nameof(CreateArraySerializer), t.GetElementType());
        }
        else if (t.IsEnum)
        {
          var serializer = ReflectionUtil.InvokeGenericThis(this, nameof(CreateEnumSerializer), t);
          return (SerializerPair) serializer;
        }
        else if (ReflectionSerializerVerifier.IsValueTuple(typeInfo))
        {
          return (SerializerPair) ReflectionUtil.InvokeGenericThis(this, nameof(CreateValueTupleSerializer), type);
        }
        else if (typeInfo.IsGenericType && typeInfo.GetGenericTypeDefinition() == typeof(Nullable<>))
        {
          var genericTypeArgument = typeInfo.GetGenericArguments()[0];
          var nullableSerializer = (SerializerPair) ReflectionUtil.InvokeGenericThis(this, nameof(RegisterNullable), genericTypeArgument);
          return nullableSerializer;
          // return CreateGenericSerializer(member, typeInfo, implementingType, implementingTypeInfo);
        }
        else
        {
          myTypesCatalog.AddType(type);
          var serializer = ReflectionUtil.InvokeGenericThis(this, nameof(CreateCustomScalar), t);
          return (SerializerPair) serializer;
        }
      }
    }

    private static bool IsList(Type t)
    {
      return t.IsGenericType && t.GetGenericTypeDefinition() is var generic  &&
             (generic == typeof(List<>) || generic == typeof(IReadOnlyList<>) || generic == typeof(IList<>) || generic == typeof(ICollection<>));
    }

    public bool CanBePolymorphic(Type type)
    {
      if (IsList(type))
        return false;

      return (type.IsClass && !type.IsSealed) || type.IsInterface;
      //&& typeof(RdReflectionBindableBase).IsAssignableFrom(typeInfo);
      //&& ReflectionSerializerVerifier.HasRdModelAttribute(typeInfo);
    }

    private SerializerPair CreateCustomScalar<T>()
    {
      if (typeof(IRdBindable).IsAssignableFrom(typeof(T)))
        Assertion.Fail($"Invalid scalar type: {typeof(T).ToString(true)}. Scalar types cannot be IRdBindable.");
      if (typeof(T).IsInterface || typeof(T).IsAbstract)
        Assertion.Fail($"Invalid scalar type: {typeof(T).ToString(true)}. Scalar types should be concrete types.");

      TypeInfo typeInfo = typeof(T).GetTypeInfo();
      var allowNullable = ReflectionSerializerVerifier.CanBeNull(typeInfo);

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
        var serPair = GetInstanceSerializer(returnType);
        memberDeserializers[index] = SerializerReflectionUtil.ConvertReader(returnType, serPair.Reader);
        memberSerializers[index] = SerializerReflectionUtil.ConvertWriter(returnType, serPair.Writer);
      }

      CtxReadDelegate<T> readerDelegate = (ctx, unsafeReader) =>
      {
        if (allowNullable && !unsafeReader.ReadNullness())
          return default;

        object instance = FormatterServices.GetUninitializedObject(typeof(T));


        for (var index = 0; index < memberDeserializers.Length; index++)
        {
          var value = memberDeserializers[index](ctx, unsafeReader);
          memberSetters[index](instance, value);
        }
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
        for (var i = 0; i < memberDeserializers.Length; i++)
        {
          var memberValue = memberGetters[i](value);
          memberSerializers[i](ctx, unsafeWriter, memberValue);
        }
      };

      return new SerializerPair(readerDelegate, writerDelegate);
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

    private SerializerPair CreateArraySerializer<T>()
    {
      GetInstanceSerializer(out var valueReader, out CtxWriteDelegate<T> valueWriter);

      CtxReadDelegate<T[]> reader = (ctx, unsafeReader) => unsafeReader.ReadArray(valueReader, ctx);
      CtxWriteDelegate<T[]> writer = (ctx, unsafeWriter, value) => unsafeWriter.WriteArray(valueWriter, ctx, value);
      return new SerializerPair(reader, writer);
    }

    private void GetInstanceSerializer<T>(out CtxReadDelegate<T> reader, out CtxWriteDelegate<T> writer)
    {
      if (CanBePolymorphic(typeof(T)))
      {
        reader = Polymorphic<T>.Read;
        writer = Polymorphic<T>.Write;
      }
      else
      {
        var serializer = GetScalarSerializer(typeof(T));
        reader = serializer.GetReader<T>();
        writer = serializer.GetWriter<T>();
      }
    }

    private SerializerPair GetInstanceSerializer(Type t)
    {
      if (CanBePolymorphic(t))
      {
        return SerializerPair.Polymorphic(t);
      }
      else
      {
        return GetScalarSerializer(t);
      }
    }

    private SerializerPair RegisterNullable<T>() where T : struct
    {
      // nullable can be only for value tuple, no need to aks for polymorphic serializer here
      var serPair = GetScalarSerializer(typeof(T));
      var ctxReadDelegate = serPair.GetReader<T>();
      var ctxWriteDelegate = serPair.GetWriter<T>();
      return new SerializerPair(ctxReadDelegate.NullableStruct(), ctxWriteDelegate.NullableStruct());
    }


    [CanBeNull]
    private SerializerPair TryGetIntrinsicSerializer(TypeInfo typeInfo)
    {
      if (ReflectionSerializerVerifier.HasIntrinsicMethods(typeInfo))
      {
        var genericArguments = typeInfo.GetGenericArguments();
        if (genericArguments.Length == 1)
        {
          var argument = genericArguments[0];
          var staticRead = SerializerReflectionUtil.GetReadStaticSerializer(typeInfo, argument);
          var staticWrite = SerializerReflectionUtil.GetWriteStaticDeserializer(typeInfo);
          return SerializerPair.CreateFromMethods(staticRead, staticWrite, GetInstanceSerializer(argument));
        }

        if (genericArguments.Length == 0)
        {
          var staticRead = SerializerReflectionUtil.GetReadStaticSerializer(typeInfo);
          var staticWrite = SerializerReflectionUtil.GetWriteStaticDeserializer(typeInfo);
          return SerializerPair.CreateFromMethods(staticRead, staticWrite);
        }

        return null;
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
        var serPair = GetInstanceSerializer(argumentType);
        memberDeserializers[index] = SerializerReflectionUtil.ConvertReader(argumentType, serPair.Reader);
        memberSerializers[index] = SerializerReflectionUtil.ConvertWriter(argumentType, serPair.Writer);
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

    public void Create<T>(out CtxReadDelegate<T> reader, out CtxWriteDelegate<T> writer)
    {
      var scalarSerializer = GetScalarSerializer(typeof(T));
      reader = scalarSerializer.GetReader<T>();
      writer = scalarSerializer.GetWriter<T>();
    }

    public void Register<T>(CtxReadDelegate<T> reader, CtxWriteDelegate<T> writer, int? predefinedType = null)
    {
      myStaticSerializers[typeof(T)] = new SerializerPair(reader, writer);
    }

    public void RegisterEnum<T>() where T : unmanaged, Enum
    {
      myStaticSerializers[typeof(T)] = this.CreateEnumSerializer<T>();
    }

    public void RegisterToplevelOnce(Type toplevelType, Action<ISerializers> registerDeclaredTypesSerializers)
    {
    }
  }
}