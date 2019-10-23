using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Rd.Util;

namespace JetBrains.Rd
{
  /// <summary>
  /// Describes a context key and provides access to value associated with this key.
  /// The associated value is thread-local and synchronized between send/advise pairs on <see cref="IWire"/>. The associated value will be the same in handler method in <see cref="IWire.Advise"/> as it was in <see cref="IWire.Send"/>.
  /// Instances of this class with the same [key] will share the associated value.
  /// Best practice is to declare context keys in toplevel entities in protocol model using <c>Toplevel.contextKey</c>. Manual declaration is also possible.
  /// </summary>
  /// <typeparam name="T">The type of value stored by this key</typeparam>
  public class RdContextKey<T>
  {
    [NotNull] public readonly string Key;
    public readonly bool IsHeavy;
    [CanBeNull] public readonly CtxReadDelegate<T> ReadDelegate;
    [CanBeNull] public readonly CtxWriteDelegate<T> WriteDelegate;

    /// <summary>
    /// 
    /// </summary>
    /// <param name="key">Textual name of this key. This is used to match this with protocol counterparts</param>
    /// <param name="isHeavy">Whether or not this key is heavy. A heavy key maintains a value set and interns values. A light key sends values as-is and does not maintain a value set.</param>
    /// <param name="readDelegate">Serializer to be used with this key.</param>
    /// <param name="writeDelegate">Serializer to be used with this key.</param>
    public RdContextKey(string key, bool isHeavy, [CanBeNull] CtxReadDelegate<T> readDelegate, [CanBeNull] CtxWriteDelegate<T> writeDelegate)
    {
      Key = key;
      IsHeavy = isHeavy;
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
     /// Current (thread- or async-local) value for this key
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
  }
}