using JetBrains.Serialization;

namespace JetBrains.Rd.Base
{
  internal interface ISingleContextHandler
  {
    void ReadValueAndPush(SerializationCtx context, UnsafeReader reader);
    void PopValue();
    
    void WriteValue(SerializationCtx context, UnsafeWriter writer);
  }
  
  internal interface ISingleContextHandler<T> : ISingleContextHandler
  {
    RdContext<T> Context { get; }

    T ReadValue(SerializationCtx context, UnsafeReader reader);
  }
}