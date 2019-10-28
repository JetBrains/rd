using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

namespace JetBrains.Rd
{
  public class RdContextBase : IEquatable<RdContextBase>
  {
    [NotNull] public readonly string Key;
    public readonly bool IsHeavy;

    public RdContextBase(string key, bool isHeavy)
    {
      Key = key;
      IsHeavy = isHeavy;
    }

    public static RdContextBase Read(SerializationCtx ctx, UnsafeReader reader)
    {
      var keyId = reader.ReadString();
      var isHeavy = reader.ReadBoolean();
      var serializerId = RdId.Read(reader);
      var actualType = ctx.Serializers.GetTypeForId(serializerId);

      return (RdContextBase) (new Func<string, bool, ISerializers, RdId, RdContextBase>(CreateContext<object>)).Method
        .GetGenericMethodDefinition().MakeGenericMethod(actualType)
        .Invoke(null, new object[] {keyId, isHeavy, ctx.Serializers, serializerId});
    }

    private static RdContext<T> CreateContext<T>(string key, bool isHeavy, ISerializers serializers, RdId typeId)
    {
      return new RdContext<T>(key, isHeavy, serializers.GetReaderForId<T>(typeId), serializers.GetWriterForId<T>(typeId));
    }

    public bool Equals(RdContextBase other)
    {
      if (ReferenceEquals(null, other)) return false;
      if (ReferenceEquals(this, other)) return true;
      return Key == other.Key;
    }

    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != this.GetType()) return false;
      return Equals((RdContextBase) obj);
    }

    public override int GetHashCode()
    {
      return Key.GetHashCode();
    }

    public static bool operator ==(RdContextBase left, RdContextBase right)
    {
      return Equals(left, right);
    }

    public static bool operator !=(RdContextBase left, RdContextBase right)
    {
      return !Equals(left, right);
    }
  }
  /// <summary>
  /// Describes a context and provides access to value associated with this context.
  /// The associated value is thread-local and synchronized between send/advise pairs on <see cref="IWire"/>. The associated value will be the same in handler method in <see cref="IWire.Advise"/> as it was in <see cref="IWire.Send"/>.
  /// Instances of this class with the same <see cref="RdContext.Key"/> will share the associated value.
  /// Best practice is to declare contexts in toplevel entities in protocol model using <c>Toplevel.context</c>. Manual declaration is also possible.
  /// </summary>
  /// <typeparam name="T">The type of value stored by this context</typeparam>
  public class RdContext<T> : RdContextBase
  {
    [NotNull] public readonly CtxReadDelegate<T> ReadDelegate;
    [NotNull] public readonly CtxWriteDelegate<T> WriteDelegate;

    /// <summary>
    /// 
    /// </summary>
    /// <param name="key">Textual name of this context. This is used to match this with protocol counterparts</param>
    /// <param name="isHeavy">Whether or not this context is heavy. A heavy context maintains a value set and interns values. A light context sends values as-is and does not maintain a value set.</param>
    /// <param name="readDelegate">Serializer to be used with this context.</param>
    /// <param name="writeDelegate">Serializer to be used with this context.</param>
    public RdContext(string key, bool isHeavy, [NotNull] CtxReadDelegate<T> readDelegate, [NotNull] CtxWriteDelegate<T> writeDelegate) : base(key, isHeavy)
    {
      ReadDelegate = readDelegate;
      WriteDelegate = writeDelegate;
    }
    
    private static readonly ThreadLocal<Dictionary<string, Stack<T>>> ourContextStacks = new ThreadLocal<Dictionary<string, Stack<T>>>(() => new Dictionary<string, Stack<T>>());

#if NET35
    private static readonly ConcurrentDictionary<string, ThreadLocal<T>> ourValues = new ConcurrentDictionary<string, ThreadLocal<T>>();
#else
    private static readonly ConcurrentDictionary<string, AsyncLocal<T>> ourValues = new ConcurrentDictionary<string, AsyncLocal<T>>();
#endif
    
     /// <summary>
     /// Current (thread- or async-local) value for this context
     /// </summary>
    public T Value
    {
      get
      {
        return ourValues.TryGetValue(Key, out var asyncLocal) ? asyncLocal.Value : default;
      }
      set
      {
        if (!ourValues.ContainsKey(Key))
        {
          #if NET35
          ourValues.TryAdd(Key, new ThreadLocal<T>());
          #else
          ourValues.TryAdd(Key, new AsyncLocal<T>());
          #endif
        }

        ourValues[Key].Value = value;
      }
    }

     /// <summary>
     /// Pushes current context value to a thread-local stack and sets new value
     /// </summary>
     public void PushContext(T value)
    {
      ourContextStacks.Value.GetOrCreate(Key, () => new Stack<T>()).Push(Value);
      Value = value;
    }

     /// <summary>
     /// Restores previous context value from a thread-local stack
     /// </summary>
    public void PopContext()
    {
      Value = ourContextStacks.Value[Key].Pop();
    }

     public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdContext<T> value)
     {
       writer.Write(value.Key);
       writer.Write(value.IsHeavy);
       RdId.Write(writer, ctx.Serializers.GetIdForType(typeof(T)));
     }
  }
}