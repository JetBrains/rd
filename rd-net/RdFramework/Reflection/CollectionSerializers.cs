using System.Collections.Generic;

namespace JetBrains.Rd.Reflection
{
  public class CollectionSerializers
  {
    public static SerializerPair CreateListSerializerPair<T>(SerializerPair itemSerializer)
    {
      return new SerializerPair(
        CreateReadListSerializer(itemSerializer.GetReader<T>()),
        CreateWriteCollectionSerializer(itemSerializer.GetWriter<T>())
        );
    }

    public static SerializerPair CreateIListSerializerPair<T>(SerializerPair itemSerializer)
    {
      CtxReadDelegate<IList<T>> readListSerializer = CreateReadListSerializer(itemSerializer.GetReader<T>());
      CtxWriteDelegate<IList<T>> writeListSerializer =(ctx, writer, value) => CreateWriteCollectionSerializer(itemSerializer.GetWriter<T>());
      return new SerializerPair(
        readListSerializer,
        writeListSerializer
      );
    }

    public static CtxReadDelegate<List<T>> CreateReadListSerializer<T>(CtxReadDelegate<T> readItem)
    {
      return (ctx, reader) =>
      {
        var count = reader.ReadInt();
        var list = new List<T>(count);
        for (int i = 0; i < count; i++)
        {
          list.Add(readItem(ctx, reader));
        }

        return list;
      };
    }

    public static CtxWriteDelegate<ICollection<T>> CreateWriteCollectionSerializer<T>(CtxWriteDelegate<T> writeItem)
    {
      return (ctx, writer, value) =>
      {
        writer.Write((int)value.Count);
        foreach (var item in value)
        {
          writeItem(ctx, writer, item);
        }
      };
    }
  }
}