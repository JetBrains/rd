using JetBrains.Serialization;

namespace JetBrains.Rd.Reflection
{
  public interface IIntrinsicMarshaller<T>
  {
    T Read(SerializationCtx ctx, UnsafeReader reader);
    void Write(SerializationCtx ctx, UnsafeWriter writer, T value);
  }
}