using System.Collections.Generic;
using System.Diagnostics;
using JetBrains.Annotations;

namespace JetBrains.Collections
{
  /// <summary>
  /// Facilitates <see cref="KeyValuePair{TKey,TValue}"/>
  /// </summary>
  public static class JetKeyValuePair
  {
    [Pure, DebuggerStepThrough]
    public static KeyValuePair<TKey, TValue> Of<TKey, TValue>(TKey key, TValue value)
    {
      return new KeyValuePair<TKey, TValue>(key, value);
    }

    public static void Deconstruct<TKey, TValue>(this KeyValuePair<TKey, TValue> pair, out TKey key, out TValue value)
    {
      key = pair.Key;
      value = pair.Value;
    }
  }
}