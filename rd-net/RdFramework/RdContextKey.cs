using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Rd.Util;

namespace JetBrains.Rd
{
  /// <summary>
  /// Describes a context key. RdContextLocals with matching registered keys will be synchronized via protocol.
  /// A heavy key maintains a value set and interns values. A light key sends values as-is and does not maintain a value set.
  /// </summary>
  /// <typeparam name="T">The type of value stored by this key</typeparam>
  public class RdContextKey<T>
  {
    [NotNull] public readonly string Key;
    public readonly bool IsHeavy;
    [CanBeNull] public readonly CtxReadDelegate<T> ReadDelegate;
    [CanBeNull] public readonly CtxWriteDelegate<T> WriteDelegate;

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