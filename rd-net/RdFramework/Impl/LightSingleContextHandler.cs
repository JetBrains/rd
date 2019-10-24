using JetBrains.Rd.Base;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
  internal class LightSingleContextHandler<T> : ISingleContextHandler<T>
  {
    public RdContext<T> Context { get; }
    public ContextValueTransformer<T> ValueTransformer { get; set; }
    private readonly CtxReadDelegate<T> myReadDelegate;
    private readonly CtxWriteDelegate<T> myWriteDelegate;

    public LightSingleContextHandler(RdContext<T> key)
    {
      Context = key;
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
      Context.PushContext(ReadValue(context, reader));
    }

    public void PopValue()
    {
      Context.PopContext();
    }
  }
}