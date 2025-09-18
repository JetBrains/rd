using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;

namespace JetBrains.Rd.Util
{
  static class DictionaryEx
  {
    [Pure]
    internal static TValue? GetOrDefault<TKey, TValue>(
      this Dictionary<TKey, TValue> dictionary, [DisallowNull] TKey key, TValue? @default = default(TValue))
    {
      if (dictionary == null) throw new ArgumentNullException(nameof(dictionary));

      return dictionary.TryGetValue(key, out var result) ? result : @default;
    }
    
    
    [MustUseReturnValue]
    internal static TValue GetOrCreate<TKey, TValue>(
      this IDictionary<TKey, TValue> dictionary, [DisallowNull] TKey key, [InstantHandle] Func<TValue> factory)
    {
      if (dictionary == null) throw new ArgumentNullException(nameof(dictionary));
      if (factory == null) throw new ArgumentNullException(nameof(factory));

      if (!dictionary.TryGetValue(key, out var value))
        dictionary.Add(key, value = factory());
      return value;
    }

    public static void BlockingAddUnique<TKey, TValue>(
      this IDictionary<TKey, TValue> dictionary, Lifetime lifetime, object @lock, TKey key,
      TValue value) where TKey: notnull
    {
      const int timeoutMs = 50;
      
      var lockTaken = false;
      try
      {
        do
        {
          // to avoid `ExecuteIfAlive after termination of Lifetime took too much time`
          if (lifetime.IsNotAlive)
            return;

          Monitor.TryEnter(@lock, timeoutMs, ref lockTaken);
        } while (!lockTaken);


        lifetime.TryBracket(() =>
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
        }, () =>
        {
          lock (@lock)
          {
            Assertion.Require(dictionary.Remove(key), "No value by key {0}", key);
          }
        });
      }
      finally
      {
        if (lockTaken)
          Monitor.Exit(@lock);
      }
    }
  }
}