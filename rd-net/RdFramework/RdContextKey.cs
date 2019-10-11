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

#if NET35
    private static readonly ThreadLocal<Dictionary<string, Stack<T>>> ourValues = new ThreadLocal<Dictionary<string, Stack<T>>>();
#else
    private static readonly AsyncLocal<Dictionary<string, Stack<T>>> ourValues = new AsyncLocal<Dictionary<string, Stack<T>>>();
#endif
    

    public T Value
    {
      get
      {
        var valuesDict = ourValues.Value;
        if (valuesDict != null && valuesDict.TryGetValue(Key, out var value))
          return value.Count == 0 ? default : value.Peek();
        return default;
      }
      set
      {
        var valuesDict = ourValues.Value;
        if (valuesDict == null)
          ourValues.Value = valuesDict = new Dictionary<string, Stack<T>>();
        ReplaceTopOrPush(valuesDict.GetOrCreate(Key, () => new Stack<T>()), value);
      }
    }

    public void PushContext(T value)
    {
      var valuesDict = ourValues.Value;
      if (valuesDict == null)
        ourValues.Value = valuesDict = new Dictionary<string, Stack<T>>();
      valuesDict.GetOrCreate(Key, () => new Stack<T>()).Push(value);
    }

    public void PopContext()
    {
      ourValues.Value[Key].Pop();
    }

    private static void ReplaceTopOrPush(Stack<T> stack, T value)
    {
      if (stack.Count != 0) 
        stack.Pop();
      stack.Push(value);
    }
  }
}