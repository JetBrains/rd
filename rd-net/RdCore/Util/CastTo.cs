using System;
using System.Linq.Expressions;

namespace JetBrains.Util.Util
{
  /// <summary>
  /// !!! Use it with caution. Main purpose is enum to/from int casting without boxing !!!
  /// /// https://stackoverflow.com/questions/1189144/c-sharp-non-boxing-conversion-of-generic-enum-to-int/23391746#23391746
  /// </summary>
  public static class CastTo<TTo>
  {
    /// <summary>    
    /// Casts <see cref="TFrom"/> to TTo  without boxing for value types. Useful in generic methods.    
    /// </summary>
    /// <typeparam name="TFrom">Source type to cast from. Usually a generic type.</typeparam>
    public static TTo From<TFrom>(TFrom s)
    {
      return Cache<TFrom>.Caster(s);
    }    

    private static class Cache<T>
    {
      internal static readonly Func<T, TTo> Caster = Get();

      private static Func<T, TTo> Get()
      {
        var p = Expression.Parameter(typeof(T), "forCSharp35");
        var c = Expression.Convert(p, typeof(TTo));
        return Expression.Lambda<Func<T, TTo>>(c, p).Compile();
      }
    }
  }
}