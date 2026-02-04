using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using JetBrains.Diagnostics;
using JetBrains.Util.Util;

namespace JetBrains.Rd.Reflection
{
  public class SimpleTypesCatalog : ITypesCatalog
  {
    private readonly ConcurrentDictionary<RdId, Type> myRdIdToTypeMapping = new();
    public ICollection<Type> Types => myRdIdToTypeMapping.Values;

    public Type? GetById(RdId id)
    {
      if (myRdIdToTypeMapping.TryGetValue(id, out var type))
      {
        return type;
      }

      return null;
    }

    public RdId GetByType(Type type) => RdIdUtil.DefineByFqn(type);

    public void AddType(Type type)
    {
      /*
       * predifined ID can be ignored. We use generated RdId in C#-C# communications even for primitive types.
       * if (predefinedId.HasValue)
          {
            var t = myRdIdToTypeMapping.GetOrAdd(new RdId(predefinedId.Value), type);
            Assertion.Assert(t == type, $"Unable to register {type.ToString(false)} with id: {predefinedId.Value}: this id already registered as {t.ToString(false)}");
          }
    */
      myRdIdToTypeMapping[RdIdUtil.DefineByFqn(type)] = type;
    }

    public void Register<T>() => AddType(typeof(T));
  }
}