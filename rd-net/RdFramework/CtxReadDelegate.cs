using JetBrains.Serialization;

namespace JetBrains.Rd
{
  public delegate T CtxReadDelegate<out T>(SerializationCtx ctx, UnsafeReader reader);
  public delegate void CtxWriteDelegate<in T>(SerializationCtx ctx, UnsafeWriter writer, T value);
}