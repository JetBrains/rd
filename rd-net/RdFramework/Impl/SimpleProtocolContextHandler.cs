using JetBrains.Rd.Base;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
  internal class SimpleProtocolContextHandler<T> : ISingleKeyProtocolContextHandler<T>
  {
    public RdContextKey<T> Key { get; }
    public ContextValueTransformer<T> ValueTransformer { get; set; }
    private readonly CtxReadDelegate<T> myReadDelegate;
    private readonly CtxWriteDelegate<T> myWriteDelegate;

    public SimpleProtocolContextHandler(RdContextKey<T> key)
    {
      Key = key;
      myReadDelegate = key.ReadDelegate;
      myWriteDelegate = key.WriteDelegate;
    }

    
    public void WriteValue(SerializationCtx context, UnsafeWriter writer)
    {
      var value = this.GetTransformedValue();
      writer.Write(value != null);
      if (value != null)
        myWriteDelegate(context, writer, value);
    }

    public T ReadValue(SerializationCtx context, UnsafeReader reader)
    {
      var hasValue = reader.ReadBool();
      if (!hasValue)
        return default;
      return this.TransformFromProtocol(myReadDelegate(context, reader));
    }

    public void ReadValueAndPush(SerializationCtx context, UnsafeReader reader)
    {
      Key.PushContext(ReadValue(context, reader));
    }

    public void PopValue()
    {
      Key.PopContext();
    }
  }
}