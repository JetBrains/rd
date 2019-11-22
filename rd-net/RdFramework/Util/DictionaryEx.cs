using System;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;

namespace JetBrains.Rd.Util
{
  static class DictionaryEx
  {
    [Pure]
    internal static TValue GetOrDefault<TKey, TValue>(
      [NotNull] this Dictionary<TKey, TValue> dictionary, [NotNull] TKey key, TValue @default = default(TValue))
    {
      if (dictionary == null) throw new ArgumentNullException(nameof(dictionary));

      return dictionary.TryGetValue(key, out var result) ? result : @default;
    }
    
    
    [MustUseReturnValue]
    internal static TValue GetOrCreate<TKey, TValue>(
      [NotNull] this IDictionary<TKey, TValue> dictionary, [NotNull] TKey key, [InstantHandle, NotNull] Func<TValue> factory)
    {
      if (dictionary == null) throw new ArgumentNullException(nameof(dictionary));
      if (factory == null) throw new ArgumentNullException(nameof(factory));

      if (!dictionary.TryGetValue(key, out var value))
        dictionary.Add(key, value = factory());
      return value;
    }

    public static void BlockingAddUnique<TKey, TValue>(
      [NotNull] this IDictionary<TKey, TValue> dictionary, Lifetime lifetime, [NotNull] object @lock, TKey key,
      TValue value)
    {

      lifetime.TryBracket(() =>
      {
        lock (@lock)
        {
          try
          {
            dictionary.Add(key, value);
          }
          catch (Exception e)
          {
            e.Data.Add("MyKey", key.ToString());
            throw;
          }
        }
      }, () =>
      {
        lock (@lock)
        {
          Assertion.Require(dictionary.Remove(key), "No value by key {0}", key);
        }
      });
    }
  }
}