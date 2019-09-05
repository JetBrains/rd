using System;
using System.Linq.Expressions;
using JetBrains.Annotations;

namespace JetBrains.Util.Util
{
  /// <summary>
  /// !!! Use it with caution. Main purpose is enum to/from int casting without boxing !!!
  /// </summary>
  public static class CastTo<TTo>
  {
    /// <summary>
    /// Caution, for experienced coders only!!! TFrom and TTo should have same field layout (and better be POD structs).
    /// The same as LINQ Expression based <see cref="From{TFrom}"/> but without any compiler checks and warm up penalty.
    /// </summary>
    /// <param name="original"></param>
    /// <typeparam name="TFrom"></typeparam>
    /// <returns>(TTo)original</returns>
    [PublicAPI] internal static unsafe TTo ReinterpretCastFrom<TFrom>(TFrom original)
    {
      TTo placeholder = default(TTo);
      var trPlaceholder = __makeref(placeholder);
      var trOriginal = __makeref(original);
      
      // Sick shit. The original reference is a pointer to a struct which has a pointer to the target in its
      // first field (“placeholder” stack var in our case), for a fixed-address thing that's about all it got.
      // We replace this pointer with an equivalent fixed pointer to our “original” stack var,
      // whose target value is then used for extracting the value
      *(void**)&trPlaceholder = *(void**)&trOriginal; 
      return __refvalue(trPlaceholder, TTo);

      // NOTE: if this stops working, backup plan #1 is to have an array of type TTo[] in some generic struct,
      // to get hold of it with a pin-handle and write the raw data into it (or two arrays as needed)
      // NOTE: backup plan #2: use SafeBuffer::PtrToStructureNative, but it's not easy to call from outside code
      // (have to make up a shared SafeBuffer and do Write<T> / Read<T> calls with it, for POD structs they are efficient enough)
    }
    
    
    /// <summary>
    /// /// https://stackoverflow.com/questions/1189144/c-sharp-non-boxing-conversion-of-generic-enum-to-int/23391746#23391746
    /// Casts <see cref="TFrom"/> to TTo  without boxing for value types. Useful in generic methods.
    /// The only problem is that this method requires around 50ms on startup to warm up. So for real hardcore see <see cref="ReinterpretCastFrom{TFrom}"/>  
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