using JetBrains.Rd.Impl;
using JetBrains.Serialization;

namespace JetBrains.Rd.Base
{
  internal interface ISingleKeyProtocolContextHandler
  {
    void ReadValueAndPush(SerializationCtx context, UnsafeReader reader);
    void PopValue();
    
    void WriteValue(SerializationCtx context, UnsafeWriter writer);
  }
  
  internal interface ISingleKeyProtocolContextHandler<T> : ISingleKeyProtocolContextHandler
  {
    RdContextKey<T> Key { get; }
    ContextValueTransformer<T> ValueTransformer { get; set; }

    T ReadValue(SerializationCtx context, UnsafeReader reader);
  }
}