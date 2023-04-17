using System;
using JetBrains.Rd.Base;
using JetBrains.Serialization;

#nullable disable

namespace JetBrains.Rd.Impl
{
  internal class LightSingleContextHandler<T> : ISingleContextHandler<T>
  {
    public RdContext<T> Context { get; }

    public RdContextBase ContextBase => Context;

    public LightSingleContextHandler(RdContext<T> context)
    {
      Context = context;
    }


    public object ReadValueBoxed(SerializationCtx context, UnsafeReader reader)
    {
      return ReadValue(context, reader);
    }

    public void WriteValue(SerializationCtx context, UnsafeWriter writer)
    {
      var value = Context.Value;
      writer.Write(value != null);
      if (value != null)
        Context.WriteDelegate(context, writer, value);
    }

    public void RegisterValueInValueSet()
    {
      // no-op
    }

    public T ReadValue(SerializationCtx context, UnsafeReader reader)
    {
      var hasValue = reader.ReadBool();
      if (!hasValue)
        return default;
      return Context.ReadDelegate(context, reader);
    }
  }
}