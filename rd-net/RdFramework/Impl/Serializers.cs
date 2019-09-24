using System;
using System.Collections.Generic;
using System.Linq;
using System.Linq.Expressions;
using JetBrains.Annotations;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;
using JetBrains.Threading;
using JetBrains.Util.Util;

namespace JetBrains.Rd.Impl
{

  public static class Polymorphic<T>
  {
    public static readonly CtxReadDelegate<T> Read = (ctx, reader) => ctx.Serializers.Read<T>(ctx, reader);
    public static readonly CtxWriteDelegate<T> Write = (ctx, writer, value) => ctx.Serializers.Write(ctx, writer, value);

    public static CtxReadDelegate<T> ReadAbstract(CtxReadDelegate<T> unknownInstanceReader)
    {
      return (ctx, reader) => ctx.Serializers.Read<T>(ctx, reader, unknownInstanceReader);
    }
  }
  
  
  public class Serializers : ISerializers
  {
    private readonly Dictionary<Type, RdId> myTypeMapping = new Dictionary<Type, RdId>();
    private readonly Dictionary<RdId, CtxReadDelegate<object>> myReaders = new Dictionary<RdId, CtxReadDelegate<object>>();
    private readonly Dictionary<RdId, CtxWriteDelegate<object>> myWriters = new Dictionary<RdId, CtxWriteDelegate<object>>();

    [CanBeNull] private readonly IPolymorphicTypesCatalog myPolymorphicCatalog;

    struct ToplevelRegistration
    {
      internal Type Type { get; }
      internal Action<ISerializers> Action { get; }

      public ToplevelRegistration(Type type, Action<ISerializers> action)
      {
        Type = type;
        Action = action;
      }
    }
    
  
#if !NET35
    private readonly Actor<ToplevelRegistration> myBackgroundRegistrar;

    public Serializers()
    {
      myBackgroundRegistrar = new Actor<ToplevelRegistration>("RegisterSerializers", Lifetime.Eternal, RegisterToplevelInternal);
      myBackgroundRegistrar.SendBlocking(new ToplevelRegistration(typeof(Serializers), RegisterFrameworkMarshallers));
    }
#else
    public Serializers() => RegisterFrameworkMarshallers(this);
#endif

    public Serializers([CanBeNull] IPolymorphicTypesCatalog polymorphicCatalog)
      : this()
    {
      myPolymorphicCatalog = polymorphicCatalog;
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

    public static readonly CtxReadDelegate<string> ReadString = (ctx, reader) => reader.ReadString();
    public static readonly CtxReadDelegate<Guid> ReadGuid = (ctx, reader) => reader.ReadGuid();
    public static readonly CtxReadDelegate<DateTime> ReadDateTime = (ctx, reader) => reader.ReadDateTime();
    public static readonly CtxReadDelegate<Uri> ReadUri = (ctx, reader) => reader.ReadUri();
    public static readonly CtxReadDelegate<RdId> ReadRdId = (ctx, reader) => reader.ReadRdId();
    
    public static readonly CtxReadDelegate<RdSecureString> ReadSecureString = (ctx, reader) => new RdSecureString(reader.ReadString());

    public static readonly CtxReadDelegate<byte[]> ReadByteArray = (ctx, reader) => reader.ReadArray(ReadByte, ctx);
    public static readonly CtxReadDelegate<short[]> ReadShortArray = (ctx, reader) => reader.ReadArray(ReadShort, ctx);
    public static readonly CtxReadDelegate<int[]> ReadIntArray = (ctx, reader) => reader.ReadArray(ReadInt, ctx);
    public static readonly CtxReadDelegate<long[]> ReadLongArray = (ctx, reader) => reader.ReadArray(ReadLong, ctx);
    public static readonly CtxReadDelegate<float[]> ReadFloatArray = (ctx, reader) => reader.ReadArray(ReadFloat, ctx);
    public static readonly CtxReadDelegate<double[]> ReadDoubleArray = (ctx, reader) => reader.ReadArray(ReadDouble, ctx);
    public static readonly CtxReadDelegate<char[]> ReadCharArray = (ctx, reader) => reader.ReadArray(ReadChar, ctx);
    public static readonly CtxReadDelegate<bool[]> ReadBoolArray = (ctx, reader) => reader.ReadArray(ReadBool, ctx);
    
    
    
    public static readonly CtxReadDelegate<byte> ReadUByte = (ctx, reader) => reader.ReadUByte();
    public static readonly CtxReadDelegate<ushort> ReadUShort = (ctx, reader) => reader.ReadUShort();
    public static readonly CtxReadDelegate<uint> ReadUInt = (ctx, reader) => reader.ReadUInt();
    public static readonly CtxReadDelegate<ulong> ReadULong = (ctx, reader) => reader.ReadULong();
    
    public static readonly CtxReadDelegate<byte[]> ReadUByteArray = (ctx, reader) => reader.ReadArray(ReadByte, ctx);
    public static readonly CtxReadDelegate<ushort[]> ReadUShortArray = (ctx, reader) => reader.ReadArray(ReadUShort, ctx);
    public static readonly CtxReadDelegate<uint[]> ReadUIntArray = (ctx, reader) => reader.ReadArray(ReadUInt, ctx);
    public static readonly CtxReadDelegate<ulong[]> ReadULongArray = (ctx, reader) => reader.ReadArray(ReadULong, ctx);
    


    //writers
    public static readonly CtxWriteDelegate<byte> WriteByte = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<short> WriteShort = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<int> WriteInt = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<long> WriteLong = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<float> WriteFloat = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<double> WriteDouble = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<char> WriteChar = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<bool> WriteBool = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<Unit> WriteVoid = (ctx, writer, value) => writer.Write(value);

    public static readonly CtxWriteDelegate<string> WriteString = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<Guid> WriteGuid = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<DateTime> WriteDateTime = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<Uri> WriteUri = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<RdId> WriteRdId = (ctx, writer, value) => writer.Write(value);
    
    public static readonly CtxWriteDelegate<RdSecureString> WriteSecureString = (ctx, writer, value) => writer.Write(value.Contents);

    public static readonly CtxWriteDelegate<byte[]> WriteByteArray = (ctx, writer, value) => writer.WriteArray(WriteByte, ctx, value);
    public static readonly CtxWriteDelegate<short[]> WriteShortArray = (ctx, writer, value) => writer.WriteArray(WriteShort, ctx, value);
    public static readonly CtxWriteDelegate<int[]> WriteIntArray = (ctx, writer, value) => writer.WriteArray(WriteInt, ctx, value);
    public static readonly CtxWriteDelegate<long[]> WriteLongArray = (ctx, writer, value) => writer.WriteArray(WriteLong, ctx, value);
    public static readonly CtxWriteDelegate<float[]> WriteFloatArray = (ctx, writer, value) => writer.WriteArray(WriteFloat, ctx, value);
    public static readonly CtxWriteDelegate<double[]> WriteDoubleArray = (ctx, writer, value) => writer.WriteArray(WriteDouble, ctx, value);
    public static readonly CtxWriteDelegate<char[]> WriteCharArray = (ctx, writer, value) => writer.WriteArray(WriteChar, ctx, value);
    public static readonly CtxWriteDelegate<bool[]> WriteBoolArray = (ctx, writer, value) => writer.WriteArray(WriteBool, ctx, value);


    public static readonly CtxWriteDelegate<byte> WriteUByte = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<ushort> WriteUShort = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<uint> WriteUInt = (ctx, writer, value) => writer.Write(value);
    public static readonly CtxWriteDelegate<ulong> WriteULong = (ctx, writer, value) => writer.Write(value);
    
    public static readonly CtxWriteDelegate<byte[]> WriteUByteArray = (ctx, writer, value) => writer.WriteArray(WriteByte, ctx, value);
    public static readonly CtxWriteDelegate<ushort[]> WriteUShortArray = (ctx, writer, value) => writer.WriteArray(WriteUShort, ctx, value);
    public static readonly CtxWriteDelegate<uint[]> WriteUIntArray = (ctx, writer, value) => writer.WriteArray(WriteUInt, ctx, value);
    public static readonly CtxWriteDelegate<ulong[]> WriteULongArray = (ctx, writer, value) => writer.WriteArray(WriteULong, ctx, value);
    
    public static void RegisterFrameworkMarshallers([NotNull] ISerializersContainer serializers)
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

    public static T ReadEnum<T>(SerializationCtx ctx, UnsafeReader reader) where T: unmanaged, Enum
    {
      Assertion.Assert(typeof(T).IsSubclassOf(typeof(Enum)), "{0}", typeof(T));
      return Cast32BitEnum<T>.FromInt(reader.ReadInt());
    }

    public static void WriteEnum<T>(SerializationCtx ctx, UnsafeWriter writer, T value) where T: unmanaged, Enum
    {
      writer.Write(Cast32BitEnum<T>.ToInt(value));
    }

    public void RegisterEnum<T>() where T: unmanaged, Enum
    {
      Register(ReadEnum<T>, WriteEnum<T>);
    }

    public void Register<T>(CtxReadDelegate<T> reader, CtxWriteDelegate<T> writer, int? predefinedId = null)
    {
      #if !NET35
      if (!myBackgroundRegistrar.IsInsideProcessing)
      {
        myBackgroundRegistrar.SendBlocking(new ToplevelRegistration(typeof(T), szr => szr.Register(reader, writer, predefinedId)));
        myBackgroundRegistrar.WaitForEmpty();
      }
      #endif
        
      var typeId = RdId.Define<T>(predefinedId);
      RdId existing;
      if (myTypeMapping.TryGetValue(typeof(T), out existing))
      {
        Assertion.Require(existing == typeId, "Type {0} already present with id={1}, but now is set with {2}", typeof(T).FullName, existing, typeId);
      }
      else
      {
        if (myReaders.ContainsKey(typeId))
        {
          var existingType = myTypeMapping.First(p => p.Value == typeId).Key;
          throw new ArgumentException(string.Format("Can't register {0} with id {1}. Already registered {2}", typeof(T).FullName, typeId, existingType));
        }
        Protocol.InitializationLogger.Trace("Registering type {0}, id={1}", typeof(T).Name, typeId);
        
        myTypeMapping[typeof(T)] = typeId;
        myReaders[typeId] = (ctx, unsafeReader) => reader(ctx, unsafeReader);
        myWriters[typeId] = (ctx, unsafeWriter, value) => writer(ctx, unsafeWriter, (T)value);
      }

    }

    public T Read<T>(SerializationCtx ctx, UnsafeReader reader, [CanBeNull] CtxReadDelegate<T> unknownInstanceReader = null)
    {
#if !NET35
      myBackgroundRegistrar.WaitForEmpty();
#endif
      
      var typeId = RdId.Read(reader);
      if (typeId.IsNil)
        return default(T);
      var size = reader.ReadInt();

      CtxReadDelegate<object> ctxReadDelegate;
      if (!myReaders.TryGetValue(typeId, out ctxReadDelegate))
      {
        if (unknownInstanceReader == null)
        {
          myPolymorphicCatalog?.TryDiscoverRegister(typeId, this);
          if (!myReaders.TryGetValue(typeId, out ctxReadDelegate))
          {
            var realType = myTypeMapping.SingleOrDefault(c => Equals(c.Value, typeId)); //ok because it's rarely needed
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

      var uncasted = ctxReadDelegate(ctx, reader);
      Assertion.Assert(uncasted is T, "Bad cast for id {0}. Expected: {1}, actual: {2}", typeId, typeof(T).Name, uncasted.GetType().Name);
      return (T)uncasted;
    }

    public void Write<T>(SerializationCtx ctx, UnsafeWriter writer, T value)
    {
#if !NET35
      myBackgroundRegistrar.WaitForEmpty();
#endif

      if (value == null)
      {
        // ReSharper disable once ImpureMethodCallOnReadonlyValueField
        RdId.Nil.Write(writer);
        return;
      }

      RdId typeId;
      var type = value.GetType();
      if (!myTypeMapping.TryGetValue(type, out typeId))
      {
        myPolymorphicCatalog?.TryDiscoverRegister(type, this);
        if (!myTypeMapping.TryGetValue(type, out typeId))
        {
          throw new KeyNotFoundException($"Type {type.FullName} have not registered");
        }
      }
      typeId.Write(writer);

      // Don't dispose this cookie, otherwise it will delete all written data
      var cookie = new UnsafeWriter.Cookie(writer);
      writer.Write(0);

      var writerDelegate = myWriters[typeId];
      writerDelegate(ctx, writer, value);

      cookie.WriteIntLengthToCookieStart();
    }

    
    private readonly HashSet<Type> myRegisteredToplevels = new HashSet<Type>();
    public void RegisterToplevelOnce(Type toplevelType, Action<ISerializers> registerDeclaredTypesSerializers)
    {
      var r = new ToplevelRegistration(toplevelType, registerDeclaredTypesSerializers);
#if !NET35
      var task = myBackgroundRegistrar.SendOrExecuteInline(r);
      Assertion.Assert(task.IsCompleted, "task.IsCompleted: {0}", task.Status);
#else
      RegisterToplevelInternal(r);
 #endif
    }

    private void RegisterToplevelInternal(ToplevelRegistration r)
    {
      if (!myRegisteredToplevels.Add(r.Type)) return;
      Protocol.InitializationLogger.Trace("REGISTER serializers for {0}", r.Type.Name);

      r.Action(this);
    }
  }
}