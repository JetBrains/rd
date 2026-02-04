using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading.Tasks;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Serialization;
using JetBrains.Util.Util;

namespace JetBrains.Rd.Impl
{
  public static class Polymorphic<T>
  {
    public static readonly CtxReadDelegate<T> Read = (ctx, reader) => ctx.Serializers.Read<T>(ctx, reader)!;
    public static readonly CtxWriteDelegate<T> Write = (ctx, writer, value) => ctx.Serializers.Write(ctx, writer, value);

    public static CtxReadDelegate<T?> ReadAbstract(CtxReadDelegate<T> unknownInstanceReader)
    {
      return (ctx, reader) => ctx.Serializers.Read<T>(ctx, reader, unknownInstanceReader);
    }
  }
  
  
  public class Serializers : ISerializers
  {
    private readonly Dictionary<Type, RdId> myTypeMapping = new Dictionary<Type, RdId>();
    private readonly Dictionary<RdId, CtxReadDelegate<object?>> myReaders = new Dictionary<RdId, CtxReadDelegate<object?>>();
    private readonly Dictionary<RdId, CtxWriteDelegate<object?>> myWriters = new Dictionary<RdId, CtxWriteDelegate<object?>>();

    private readonly ITypesRegistrar? myRegistrar;
    private readonly object myLock = new object();
    
    private readonly StealingScheduler myBackgroundRegistrar;

    public Serializers() : this(null, null)
    {
    }

    public Serializers(TaskScheduler? scheduler, ITypesRegistrar? registrar)
    {
      myRegistrar = registrar;
      myBackgroundRegistrar = new StealingScheduler(new ConcurrentExclusiveSchedulerPair(scheduler ?? TaskScheduler.Default).ExclusiveScheduler, false);
      RegisterToplevelOnce(typeof(Serializers), RegisterFrameworkMarshallers);
    }

    public Serializers(ITypesRegistrar? registrar)
      : this()
    {
      myRegistrar = registrar;
    }

    [Obsolete("Lifetime is not required anymore", false)]
    public Serializers(Lifetime lifetime, TaskScheduler? scheduler, ITypesRegistrar? registrar)
    : this(scheduler, registrar)
    {
    }

    //readers
    public static readonly CtxReadDelegate<byte> ReadByte = (ctx, reader) => reader.ReadByte();
    public static readonly CtxReadDelegate<short> ReadShort = (ctx, reader) => reader.ReadShort();
    public static readonly CtxReadDelegate<int> ReadInt = (ctx, reader) => reader.ReadInt();
    public static readonly CtxReadDelegate<long> ReadLong = (ctx, reader) => reader.ReadLong();
    public static readonly CtxReadDelegate<float> ReadFloat = (ctx, reader) => reader.ReadFloat();
    public static readonly CtxReadDelegate<double> ReadDouble = (ctx, reader) => reader.ReadDouble();
    public static readonly CtxReadDelegate<char> ReadChar = (ctx, reader) => reader.ReadChar();
    public static readonly CtxReadDelegate<bool> ReadBool = (ctx, reader) => reader.ReadBool();
    public static readonly CtxReadDelegate<Unit> ReadVoid = (ctx, reader) => reader.ReadVoid();

    public static readonly CtxReadDelegate<string?> ReadString = (ctx, reader) => reader.ReadString();
    public static readonly CtxReadDelegate<Guid> ReadGuid = (ctx, reader) => reader.ReadGuid();
    public static readonly CtxReadDelegate<DateTime> ReadDateTime = (ctx, reader) => reader.ReadDateTime();
    public static readonly CtxReadDelegate<TimeSpan> ReadTimeSpan = (ctx, reader) => reader.ReadTimeSpan();
    public static readonly CtxReadDelegate<Uri> ReadUri = (ctx, reader) => reader.ReadUri();
    public static readonly CtxReadDelegate<RdId> ReadRdId = (ctx, reader) => reader.ReadRdId();
    
    public static readonly CtxReadDelegate<RdSecureString> ReadSecureString = (ctx, reader) => reader.ReadSecureString();

    public static readonly CtxReadDelegate<byte[]?> ReadByteArray = (ctx, reader) => reader.ReadArray(ReadByte, ctx);
    public static readonly CtxReadDelegate<short[]?> ReadShortArray = (ctx, reader) => reader.ReadArray(ReadShort, ctx);
    public static readonly CtxReadDelegate<int[]?> ReadIntArray = (ctx, reader) => reader.ReadArray(ReadInt, ctx);
    public static readonly CtxReadDelegate<long[]?> ReadLongArray = (ctx, reader) => reader.ReadArray(ReadLong, ctx);
    public static readonly CtxReadDelegate<float[]?> ReadFloatArray = (ctx, reader) => reader.ReadArray(ReadFloat, ctx);
    public static readonly CtxReadDelegate<double[]?> ReadDoubleArray = (ctx, reader) => reader.ReadArray(ReadDouble, ctx);
    public static readonly CtxReadDelegate<char[]?> ReadCharArray = (ctx, reader) => reader.ReadArray(ReadChar, ctx);
    public static readonly CtxReadDelegate<bool[]?> ReadBoolArray = (ctx, reader) => reader.ReadArray(ReadBool, ctx);
    
    
    
    public static readonly CtxReadDelegate<byte> ReadUByte = (ctx, reader) => reader.ReadUByte();
    public static readonly CtxReadDelegate<ushort> ReadUShort = (ctx, reader) => reader.ReadUShort();
    public static readonly CtxReadDelegate<uint> ReadUInt = (ctx, reader) => reader.ReadUInt();
    public static readonly CtxReadDelegate<ulong> ReadULong = (ctx, reader) => reader.ReadULong();
    
    public static readonly CtxReadDelegate<byte[]?> ReadUByteArray = (ctx, reader) => reader.ReadArray(ReadByte, ctx);
    public static readonly CtxReadDelegate<ushort[]?> ReadUShortArray = (ctx, reader) => reader.ReadArray(ReadUShort, ctx);
    public static readonly CtxReadDelegate<uint[]?> ReadUIntArray = (ctx, reader) => reader.ReadArray(ReadUInt, ctx);
    public static readonly CtxReadDelegate<ulong[]?> ReadULongArray = (ctx, reader) => reader.ReadArray(ReadULong, ctx);
    


    //writers
    public static readonly CtxWriteDelegate<byte> WriteByte = (ctx, writer, value) => writer.WriteByte(value);
    public static readonly CtxWriteDelegate<short> WriteShort = (ctx, writer, value) => writer.WriteInt16(value);
    public static readonly CtxWriteDelegate<int> WriteInt = (ctx, writer, value) => writer.WriteInt32(value);
    public static readonly CtxWriteDelegate<long> WriteLong = (ctx, writer, value) => writer.WriteInt64(value);
    public static readonly CtxWriteDelegate<float> WriteFloat = (ctx, writer, value) => writer.WriteFloat(value);
    public static readonly CtxWriteDelegate<double> WriteDouble = (ctx, writer, value) => writer.WriteDouble(value);
    public static readonly CtxWriteDelegate<char> WriteChar = (ctx, writer, value) => writer.WriteChar(value);
    public static readonly CtxWriteDelegate<bool> WriteBool = (ctx, writer, value) => writer.WriteBoolean(value);
    public static readonly CtxWriteDelegate<Unit> WriteVoid = (ctx, writer, value) => writer.Write(value);

    public static readonly CtxWriteDelegate<string?> WriteString = (ctx, writer, value) => writer.WriteString(value);
    public static readonly CtxWriteDelegate<Guid> WriteGuid = (ctx, writer, value) => writer.WriteGuid(value);
    public static readonly CtxWriteDelegate<DateTime> WriteDateTime = (ctx, writer, value) => writer.WriteDateTime(value);
    public static readonly CtxWriteDelegate<TimeSpan> WriteTimeSpan = (ctx, writer, value) => writer.WriteTimeSpan(value);
    public static readonly CtxWriteDelegate<Uri> WriteUri = (ctx, writer, value) => writer.WriteUri(value);
    public static readonly CtxWriteDelegate<RdId> WriteRdId = (ctx, writer, value) => writer.Write(value);
    
    public static readonly CtxWriteDelegate<RdSecureString> WriteSecureString = (ctx, writer, value) => writer.WriteString(value.Contents);

    public static readonly CtxWriteDelegate<byte[]?> WriteByteArray = (ctx, writer, value) => writer.WriteArray(WriteByte, ctx, value);
    public static readonly CtxWriteDelegate<short[]?> WriteShortArray = (ctx, writer, value) => writer.WriteArray(WriteShort, ctx, value);
    public static readonly CtxWriteDelegate<int[]?> WriteIntArray = (ctx, writer, value) => writer.WriteArray(WriteInt, ctx, value);
    public static readonly CtxWriteDelegate<long[]?> WriteLongArray = (ctx, writer, value) => writer.WriteArray(WriteLong, ctx, value);
    public static readonly CtxWriteDelegate<float[]?> WriteFloatArray = (ctx, writer, value) => writer.WriteArray(WriteFloat, ctx, value);
    public static readonly CtxWriteDelegate<double[]?> WriteDoubleArray = (ctx, writer, value) => writer.WriteArray(WriteDouble, ctx, value);
    public static readonly CtxWriteDelegate<char[]?> WriteCharArray = (ctx, writer, value) => writer.WriteArray(WriteChar, ctx, value);
    public static readonly CtxWriteDelegate<bool[]?> WriteBoolArray = (ctx, writer, value) => writer.WriteArray(WriteBool, ctx, value);


    public static readonly CtxWriteDelegate<byte> WriteUByte = (ctx, writer, value) => writer.WriteByte(value);
    public static readonly CtxWriteDelegate<ushort> WriteUShort = (ctx, writer, value) => writer.WriteUInt16(value);
    public static readonly CtxWriteDelegate<uint> WriteUInt = (ctx, writer, value) => writer.WriteUInt32(value);
    public static readonly CtxWriteDelegate<ulong> WriteULong = (ctx, writer, value) => writer.WriteUInt64(value);
    
    public static readonly CtxWriteDelegate<byte[]?> WriteUByteArray = (ctx, writer, value) => writer.WriteArray(WriteByte, ctx, value);
    public static readonly CtxWriteDelegate<ushort[]?> WriteUShortArray = (ctx, writer, value) => writer.WriteArray(WriteUShort, ctx, value);
    public static readonly CtxWriteDelegate<uint[]?> WriteUIntArray = (ctx, writer, value) => writer.WriteArray(WriteUInt, ctx, value);
    public static readonly CtxWriteDelegate<ulong[]?> WriteULongArray = (ctx, writer, value) => writer.WriteArray(WriteULong, ctx, value);
    
    public static void RegisterFrameworkMarshallers(ISerializersContainer serializers)
    {
      serializers.Register(ReadByte, WriteByte, 1);
      serializers.Register(ReadShort, WriteShort, 2);
      serializers.Register(ReadInt, WriteInt, 3);
      serializers.Register(ReadLong, WriteLong, 4);
      
      serializers.Register(ReadFloat, WriteFloat, 5);
      serializers.Register(ReadDouble, WriteDouble, 6);
      serializers.Register(ReadChar, WriteChar, 7);
      serializers.Register(ReadBool, WriteBool, 8);
      serializers.Register(ReadVoid, WriteVoid, 9);

      serializers.Register(ReadString, WriteString, 10);
      serializers.Register(ReadGuid, WriteGuid, 11);
      serializers.Register(ReadDateTime, WriteDateTime, 12);
      serializers.Register(ReadUri, WriteUri, 13);
      serializers.Register(ReadRdId, WriteRdId, 14);

      serializers.Register(ReadSecureString, WriteSecureString, 15);

      serializers.Register(ReadByteArray, WriteByteArray, 31);
      serializers.Register(ReadShortArray, WriteShortArray, 32);
      serializers.Register(ReadIntArray, WriteIntArray, 33);
      serializers.Register(ReadLongArray, WriteLongArray, 34);
      
      serializers.Register(ReadFloatArray, WriteFloatArray, 35);
      serializers.Register(ReadDoubleArray, WriteDoubleArray, 36);
      serializers.Register(ReadCharArray, WriteCharArray, 37);
      serializers.Register(ReadBoolArray, WriteBoolArray, 38);
      
      //unsigned
      
      //clashes with Byte
//      serializers.Register(ReadUByte, WriteUByte, 41);
      serializers.Register(ReadUShort, WriteUShort, 42);
      serializers.Register(ReadUInt, WriteUInt, 43);
      serializers.Register(ReadULong, WriteULong, 44);
      
      //clashes with ByteArray
//      serializers.Register(ReadUByteArray, WriteUByteArray, 45);
      serializers.Register(ReadUShortArray, WriteUShortArray, 46);
      serializers.Register(ReadUIntArray, WriteUIntArray, 47);
      serializers.Register(ReadULongArray, WriteULongArray, 48);
    }

    public static T ReadEnum<T>(SerializationCtx ctx, UnsafeReader reader) where T :
    unmanaged,
     Enum
    {
      if (Mode.IsAssertion) Assertion.Assert(typeof(T).IsSubclassOf(typeof(Enum)), "{0}", typeof(T));
      return Cast32BitEnum<T>.FromInt(reader.ReadInt());
    }

    public static void WriteEnum<T>(SerializationCtx ctx, UnsafeWriter writer, T value) where T :
    unmanaged,
     Enum
    {
      writer.WriteInt32(Cast32BitEnum<T>.ToInt(value));
    }

    public void RegisterEnum<T>() where T :
    unmanaged,
     Enum
    {
      Register(ReadEnum<T>, WriteEnum<T>);
    }

    public void Register<T>(CtxReadDelegate<T> reader, CtxWriteDelegate<T> writer, long? predefinedId = null)
    {
      lock (myLock)
      {
        var typeId = RdIdUtil.Define<T>(predefinedId);
        RdId existing;
        if (myTypeMapping.TryGetValue(typeof(T), out existing))
        {
          Assertion.Require(existing == typeId, "Type {0} already present with id={1}, but now is set with {2}", typeof(T).FullName, existing, typeId);
        }
        else
        {
          if (myReaders.ContainsKey(typeId))
          {
            Type existingType;
            lock(myLock)
              existingType = myTypeMapping.First(p => p.Value == typeId).Key;
            throw new ArgumentException(string.Format("Can't register {0} with id {1}. Already registered {2}", typeof(T).FullName, typeId, existingType));
          }
          Protocol.InitTrace?.Log($"Registering type {typeof(T).Name}, id={typeId}");
        
          myTypeMapping[typeof(T)] = typeId;
          myReaders[typeId] = (ctx, unsafeReader) => reader(ctx, unsafeReader);
          myWriters[typeId] = (ctx, unsafeWriter, value) => writer(ctx, unsafeWriter, (T)value!);
        }
      }
    }

    public T? Read<T>(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<T>? unknownInstanceReader = null)
    {
      bool TryGetReader(RdId rdId, out CtxReadDelegate<object?> readDelegate)
      {
        lock (myLock)
          return myReaders.TryGetValue(rdId, out readDelegate);
      }
      myBackgroundRegistrar.Join();
      
      var typeId = RdId.Read(reader);
      if (typeId.IsNil)
        return default;
      var size = reader.ReadInt();

      if (!TryGetReader(typeId, out var ctxReadDelegate))
      {
        if (unknownInstanceReader == null)
        {
          myRegistrar?.TryRegister(typeId, this);
          myRegistrar?.TryRegister(typeof(T), this);
          if (!TryGetReader(typeId, out ctxReadDelegate))
          {
            var realType = myTypeMapping.SingleOrDefault(c => EqualityComparer<RdId>.Default.Equals(c.Value, typeId)); //ok because it's rarely needed
            throw new KeyNotFoundException(string.Format("There is no readers found. Requested type '{0}'. Real type {1}", typeof(T).FullName, realType));
          }
        }
        else
        {
          var objectStart = reader.Position;
          var result = unknownInstanceReader(ctx, reader);
          var bytesRead = reader.Position - objectStart;
          reader.Skip(size - bytesRead);
          return result;
        }
      }

      var uncasted = ctxReadDelegate(ctx, reader)!;
      if (Mode.IsAssertion) Assertion.Assert(uncasted is T, "Bad cast for id {0}. Expected: {1}, actual: {2}", typeId, typeof(T).Name, uncasted.GetType().Name);
      return (T)uncasted;
    }

    public void Write<T>(SerializationCtx ctx, UnsafeWriter writer, T value)
    {
      bool TryGetTypeMapping(Type type1, out RdId rdId)
      {
        lock (myLock)
          return myTypeMapping.TryGetValue(type1, out rdId);
      }

      myBackgroundRegistrar.Join();

      if (value == null)
      {
        // ReSharper disable once ImpureMethodCallOnReadonlyValueField
        RdId.Nil.Write(writer);
        return;
      }

      var type = value.GetType();
      if (!TryGetTypeMapping(type, out var typeId))
      {
        myRegistrar?.TryRegister(type, this);
        if (!TryGetTypeMapping(type, out typeId))
        {
          throw new KeyNotFoundException($"Type {type.FullName} have not registered");
        }
      }
      typeId.Write(writer);

      var bookmark = new UnsafeWriter.Bookmark(writer);
      writer.WriteInt32(0);
      CtxWriteDelegate<object> writerDelegate;
      lock (myLock)
        writerDelegate = myWriters[typeId];
      writerDelegate(ctx, writer, value);

      bookmark.WriteIntLength();
    }

    
    private readonly HashSet<Type> myRegisteredToplevels = new HashSet<Type>();
    public void RegisterToplevelOnce(Type toplevelType, Action<ISerializers> registerDeclaredTypesSerializers)
    {
      new Task(() => RegisterToplevelInternal(toplevelType, registerDeclaredTypesSerializers)).Start(myBackgroundRegistrar);
    }

    private void RegisterToplevelInternal(Type type, Action<ISerializers> register)
    {
      if (!myRegisteredToplevels.Add(type)) return;
      Protocol.InitTrace?.Log($"REGISTER serializers for {type.Name}");

      register(this);
    }
  }
}