using JetBrains.Rd.Impl;
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
    ContextValueTransformer<T> ValueTransformer { get; set; }

    T ReadValue(SerializationCtx context, UnsafeReader reader);
  }
}