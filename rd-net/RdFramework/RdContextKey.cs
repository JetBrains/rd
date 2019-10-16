using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Rd.Util;

namespace JetBrains.Rd
{
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

    public void PushContext(T value)
    {
      ourContextStacks.Value.GetOrCreate(Key, () => new Stack<T>()).Push(Value);
      Value = value;
    }

    public void PopContext()
    {
      Value = ourContextStacks.Value[Key].Pop();
    }
  }
}