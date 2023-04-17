using JetBrains.Serialization;

namespace JetBrains.Rd.Base
{
  internal interface ISingleContextHandler
  {
    void WriteValue(SerializationCtx context, UnsafeWriter writer);

    void RegisterValueInValueSet();

    object ReadValueBoxed(SerializationCtx context, UnsafeReader reader);
    RdContextBase ContextBase { get; }
  }
  
  internal interface ISingleContextHandler<T> : ISingleContextHandler
  {
    RdContext<T> Context { get; }

    T? ReadValue(SerializationCtx context, UnsafeReader reader);
  }
}