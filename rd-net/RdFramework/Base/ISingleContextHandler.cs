using System;
using JetBrains.Serialization;

namespace JetBrains.Rd.Base
{
  internal interface ISingleContextHandler
  {
    IDisposable ReadValueIntoContext(SerializationCtx context, UnsafeReader reader);

    void WriteValue(SerializationCtx context, UnsafeWriter writer);

    void RegisterValueInValueSet();
    
    RdContextBase ContextBase { get; }
  }
  
  internal interface ISingleContextHandler<T> : ISingleContextHandler
  {
    RdContext<T> Context { get; }

    T? ReadValue(SerializationCtx context, UnsafeReader reader);
  }
}