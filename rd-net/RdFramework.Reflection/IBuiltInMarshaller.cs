using JetBrains.Serialization;

namespace JetBrains.Rd.Reflection
{
  public interface IBuiltInMarshaller<T>
  {
    T Read(SerializationCtx ctx, UnsafeReader reader);
    void Write(SerializationCtx ctx, UnsafeWriter writer, T value);
  }
}