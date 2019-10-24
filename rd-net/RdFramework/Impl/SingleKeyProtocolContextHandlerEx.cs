using JetBrains.Rd.Base;

namespace JetBrains.Rd.Impl
{
  public static class SingleKeyProtocolContextHandlerEx
  {
    internal static T GetTransformedValue<T>(this ISingleContextHandler<T> handler)
    {
      var value = handler.Context.Value;
      return handler.TransformToProtocol(value);
    }

    internal static T TransformToProtocol<T>(this ISingleContextHandler<T> handler, T value)
    {
      return handler.ValueTransformer != null ? handler.ValueTransformer(value, ContextValueTransformerDirection.WriteToProtocol) : value;
    }
    
    internal static T TransformFromProtocol<T>(this ISingleContextHandler<T> handler, T value)
    {
      return handler.ValueTransformer != null ? handler.ValueTransformer(value, ContextValueTransformerDirection.ReadFromProtocol) : value;
    }
  }
}