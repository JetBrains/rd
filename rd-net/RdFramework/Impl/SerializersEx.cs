using System.Collections.Generic;
using System.Runtime.CompilerServices;
using JetBrains.Core;
using JetBrains.Serialization;
using JetBrains.Util;

namespace JetBrains.Rd.Impl
{
  public static class SerializersEx
  {
    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static List<T> ReadList<T>(this UnsafeReader reader, CtxReadDelegate<T> itemReader, SerializationCtx ctx)
    {
      int count = reader.ReadInt32();
      if (count < 0) return null;
      var res = new List<T>(count);


      for (var i=0; i < count; i++) res.Add(itemReader(ctx, reader));
      return res;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void WriteList<T>(this UnsafeWriter writer, CtxWriteDelegate<T> itemWriter, SerializationCtx ctx, List<T> value)
    {
      if (value == null)
      {
        writer.Write(-1);
        return;
      }

      writer.Write(value.Count);
      // ReSharper disable once ForCanBeConvertedToForeach
      for (var i = 0; i < value.Count; i++)
      {
        itemWriter(ctx, writer, value[i]);
      }
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void WriteCollection<T>(this UnsafeWriter writer, CtxWriteDelegate<T> itemWriter, SerializationCtx ctx, ICollection<T> value)
    {
      if (value == null)
      {
        writer.Write(-1);
        return;
      }

      writer.Write(value.Count);
      foreach (var item in value)
      {
        itemWriter(ctx, writer, item);
      }
    }



    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static T[] ReadArray<T>(this UnsafeReader reader, CtxReadDelegate<T> itemReader, SerializationCtx ctx)
    {
      int count = reader.ReadInt32();
      if (count < 0) return null;
      var res = new T[count];


      for (var i=0; i < count; i++) res[i] = itemReader(ctx, reader);
      return res;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void WriteArray<T>(this UnsafeWriter writer, CtxWriteDelegate<T> itemWriter, SerializationCtx ctx, T[] value)
    {
      if (value == null)
      {
        writer.Write(-1);
        return;
      }

      writer.Write(value.Length);
      // ReSharper disable once ForCanBeConvertedToForeach
      for (var i = 0; i < value.Length; i++)
      {
        itemWriter(ctx, writer, value[i]);
      }
    }


    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static T ReadNullableClass<T>(this UnsafeReader reader, CtxReadDelegate<T> itemReader, SerializationCtx ctx) where T:class
    {
      return reader.ReadNullness() ? itemReader(ctx, reader) : null;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static T? ReadNullableStruct<T>(this UnsafeReader reader, CtxReadDelegate<T> itemReader, SerializationCtx ctx) where T:struct
    {
      return reader.ReadNullness() ? itemReader(ctx, reader).ToNullable() : null;
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void WriteNullableClass<T>(this UnsafeWriter writer, CtxWriteDelegate<T> itemWriter, SerializationCtx ctx, T value) where T:class
    {
      if (writer.WriteNullness(value)) itemWriter(ctx, writer, value);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void WriteNullableStruct<T>(this UnsafeWriter writer, CtxWriteDelegate<T> itemWriter, SerializationCtx ctx, T? value) where T:struct
    {
      if (writer.WriteNullness(value)) itemWriter(ctx, writer, value.Value);
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static RdSecureString ReadSecureString(this UnsafeReader reader)
    {
      return new RdSecureString(reader.ReadString());
    }

    [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
    public static void Write(this UnsafeWriter writer, RdSecureString @string)
    {
      writer.Write(@string.Contents);
    }
    
    public static RdId ReadRdId(this UnsafeReader reader)
    {
      return RdId.Read(reader);
    }

    public static void Write(this UnsafeWriter writer, RdId id)
    {
      id.Write(writer);
    }

    public static Unit ReadVoid(this UnsafeReader reader)
    {
      return Unit.Instance;
    }

    public static void Write(this UnsafeWriter writer, Unit value) {}


    // Composition functors

    //Readers
    public static CtxReadDelegate<T[]> Array<T>(this CtxReadDelegate<T> inner)
    {
      return (ctx, reader) => reader.ReadArray(inner, ctx);
    }

    public static CtxReadDelegate<List<T>> List<T>(this CtxReadDelegate<T> inner)
    {
      return (ctx, reader) => reader.ReadList(inner, ctx);
    }

    public static CtxReadDelegate<T> NullableClass<T>(this CtxReadDelegate<T> inner) where T : class
    {
      return (ctx, reader) => reader.ReadNullableClass(inner, ctx);
    }

    public static CtxReadDelegate<T?> NullableStruct<T>(this CtxReadDelegate<T> inner) where T : struct
    {
      return (ctx, reader) => reader.ReadNullableStruct(inner, ctx);
    }

    public static CtxReadDelegate<T> Interned<T>(this CtxReadDelegate<T> inner, string internKey)
    {
      return (ctx, reader) => ctx.ReadInterned(reader, internKey, inner);
    }


    //Writers
    public static CtxWriteDelegate<T[]> Array<T>(this CtxWriteDelegate<T> inner)
    {
      return (ctx, reader, value) => reader.WriteArray(inner, ctx, value);
    }

    public static CtxWriteDelegate<List<T>> List<T>(this CtxWriteDelegate<T> inner)
    {
      return (ctx, reader, value) => reader.WriteList(inner, ctx, value);
    }

    public static CtxWriteDelegate<T> NullableClass<T>(this CtxWriteDelegate<T> inner) where T:class
    {
      return (ctx, reader, value) => reader.WriteNullableClass(inner, ctx, value);
    }

    public static CtxWriteDelegate<T?> NullableStruct<T>(this CtxWriteDelegate<T> inner) where T : struct
    {
      return (ctx, reader, value) => reader.WriteNullableStruct(inner, ctx, value);
    }
    
    public static CtxWriteDelegate<T> Interned<T>(this CtxWriteDelegate<T> inner, string internKey)
    {
      return (ctx, reader, value) => ctx.WriteInterned(reader, value, internKey, inner);
    }


  }
}