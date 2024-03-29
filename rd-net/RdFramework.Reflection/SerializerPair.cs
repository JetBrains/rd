using System;
using System.Diagnostics;
using System.Reflection;
using JetBrains.Diagnostics;
using JetBrains.Serialization;
using JetBrains.Util;
using JetBrains.Util.Util;

namespace JetBrains.Rd.Reflection;

[DebuggerDisplay("TR: {ReaderTypeString}, TW: {WriterTypeString}, Polymorphic: {IsPolymorphic}")]
public sealed class SerializerPair
{
  private readonly object myReader;
  private readonly object myWriter;

  public object Reader => myReader;
  public object Writer => myWriter;

  public bool IsPolymorphic { get; }

  public Type ReaderType => myReader.GetType().GetGenericArguments()[0];
  public Type WriterType => myWriter.GetType().GetGenericArguments()[0];

  private string WriterTypeString => WriterType.ToString(false);
  private string ReaderTypeString => ReaderType.ToString(false);

  public SerializerPair(object reader, object writer, bool isPolymorphic = false)
  {
    if (reader == null) throw new ArgumentNullException(nameof(reader));
    if (writer == null) throw new ArgumentNullException(nameof(writer));

    if (Mode.IsAssertion)
    {
      Assertion.Assert(reader.GetType().GetGenericTypeDefinition() == typeof(CtxReadDelegate<>),
        $"Invalid type: expected CtxReaderDelegate, but was {reader.GetType().ToString(true)}");
      Assertion.Assert(writer.GetType().GetGenericTypeDefinition() == typeof(CtxWriteDelegate<>),
        $"Invalid type: expected CtxWriteDelegate, but was {writer.GetType().ToString(true)}");
    }

    myReader = reader;
    myWriter = writer;
    IsPolymorphic = isPolymorphic;
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
    return (SerializerPair) ReflectionUtil.InvokeStaticGeneric(typeof(SerializerPair), nameof(CreateFromMethodsImpl0), readMethod.ReturnType, readMethod, writeMethod)!;
  }

  /// <summary>
  /// Create serializer from Read  and Write method without <see cref="SerializationCtx"/>
  /// </summary>
  public static SerializerPair CreateFromNonProtocolMethods(MethodInfo readMethod, MethodInfo writeMethod)
  {
    return (SerializerPair) ReflectionUtil.InvokeStaticGeneric(typeof(SerializerPair), nameof(CreateFromNonProtocolMethodsT), readMethod.ReturnType, readMethod, writeMethod)!;
  }

  public static SerializerPair CreateFromMethods(MethodInfo readMethod, MethodInfo writeMethod, SerializerPair argumentSerializer)
  {
    return (SerializerPair) ReflectionUtil.InvokeStaticGeneric(typeof(SerializerPair),
      nameof(CreateFromMethodsImpl1),
      readMethod.ReturnType,
      readMethod,
      writeMethod,
      argumentSerializer)!;
  }

  public static SerializerPair CreateFromMethods(MethodInfo readMethod, MethodInfo writeMethod, SerializerPair keySerializer, SerializerPair valueSerializer)
  {
    return (SerializerPair) ReflectionUtil.InvokeStaticGeneric(typeof(SerializerPair),
      nameof(CreateFromMethodsImpl2),
      readMethod.ReturnType,
      readMethod,
      writeMethod,
      keySerializer,
      valueSerializer)!;
  }

  private static SerializerPair CreateFromMethodsImpl0<T>(MethodInfo readMethod, MethodInfo writeMethod)
  {
    void WriterDelegate(SerializationCtx ctx, UnsafeWriter writer, T value) =>
      writeMethod.Invoke(value, new object?[] { ctx, writer });

    void WriterDelegateStatic(SerializationCtx ctx, UnsafeWriter writer, T value) =>
      writeMethod.Invoke(null, new object?[] { ctx, writer, value, });

    T ReaderDelegate(SerializationCtx ctx, UnsafeReader reader) =>
      (T) readMethod.Invoke(null, new object?[] { ctx, reader });

    CtxReadDelegate<T> ctxReadDelegate = ReaderDelegate;
    CtxWriteDelegate<T> ctxWriteDelegate = writeMethod.IsStatic ? WriterDelegateStatic : WriterDelegate;
    return new SerializerPair(ctxReadDelegate, ctxWriteDelegate);
  }

  private static SerializerPair CreateFromMethodsImpl1<T>(MethodInfo readMethod, MethodInfo writeMethod, SerializerPair keySerializer)
  {
    var ctxKeyReadDelegate = keySerializer.Reader;
    var ctxKeyWriteDelegate = keySerializer.Writer;
      
    void WriterDelegate(SerializationCtx ctx, UnsafeWriter writer, T value) => 
      writeMethod.Invoke(null, new object?[] {ctx, writer, value});

    T ReaderDelegate(SerializationCtx ctx, UnsafeReader reader)
    {
      return (T)readMethod.Invoke(null,
        new[] {ctx, reader, ctxKeyReadDelegate, ctxKeyWriteDelegate});
    }

    CtxReadDelegate<T> ctxReadDelegate = ReaderDelegate;
    CtxWriteDelegate<T> ctxWriteDelegate = WriterDelegate;
    return new SerializerPair(ctxReadDelegate, ctxWriteDelegate);
  }


  private static SerializerPair CreateFromMethodsImpl2<T>(MethodInfo readMethod, MethodInfo writeMethod, SerializerPair keySerializer, SerializerPair valueSerializer)
  {
    var ctxKeyReadDelegate = keySerializer.Reader;
    var ctxKeyWriteDelegate = keySerializer.Writer;
    var ctxValueReadDelegate = valueSerializer.Reader;
    var ctxValueWriteDelegate = valueSerializer.Writer;

    void WriterDelegate(SerializationCtx ctx, UnsafeWriter writer, T value) => 
      writeMethod.Invoke(null, new object?[] {ctx, writer, value});

    T ReaderDelegate(SerializationCtx ctx, UnsafeReader reader)
    {
      return (T)readMethod.Invoke(null,
        new[] {ctx, reader, ctxKeyReadDelegate, ctxKeyWriteDelegate, ctxValueReadDelegate, ctxValueWriteDelegate});
    }

    CtxReadDelegate<T> ctxReadDelegate = ReaderDelegate;
    CtxWriteDelegate<T> ctxWriteDelegate = WriterDelegate;
    return new SerializerPair(ctxReadDelegate, ctxWriteDelegate);
  }

  public static SerializerPair FromMarshaller<T>(IBuiltInMarshaller<T> marshaller)
  {
    CtxReadDelegate<T> ctxReadDelegate = marshaller.Read;
    CtxWriteDelegate<T> ctxWriteDelegate = marshaller.Write;
    return new SerializerPair(ctxReadDelegate, ctxWriteDelegate);
  }

  private static SerializerPair CreateFromNonProtocolMethodsT<T>(MethodInfo readMethod, MethodInfo writeMethod)
  {
    Assertion.Require(readMethod.IsStatic, $"Read method should be static ({readMethod.DeclaringType.ToString(true)})");

    void WriterDelegate(SerializationCtx ctx, UnsafeWriter writer, T value)
    {
      if (!typeof(T).IsValueType && !writer.WriteNullness(value as object))
        return;
      writeMethod.Invoke(value, new object[] {writer});
    }

    void WriterDelegateStatic(SerializationCtx ctx, UnsafeWriter writer, T value)
    {
      if (!typeof(T).IsValueType && !writer.WriteNullness(value as object))
        return;
      writeMethod.Invoke(null, new object?[] {writer, value});
    }

    T? ReaderDelegate(SerializationCtx ctx, UnsafeReader reader)
    {
      if (!typeof(T).IsValueType && !reader.ReadNullness())
        return default;

      return (T) readMethod.Invoke(null, new object[] {reader});
    }

    CtxReadDelegate<T?> ctxReadDelegate = ReaderDelegate;
    CtxWriteDelegate<T> ctxWriteDelegate = writeMethod.IsStatic ? WriterDelegateStatic : WriterDelegate;
    return new SerializerPair(ctxReadDelegate, ctxWriteDelegate);
  }
}