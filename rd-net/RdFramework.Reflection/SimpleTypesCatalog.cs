using System;
using System.Collections.Concurrent;

namespace JetBrains.Rd.Reflection
{
  public class SimpleTypesCatalog : ITypesCatalog
  {
    private readonly ConcurrentDictionary<RdId, Type> myRdIdToTypeMapping = new();

    public Type? GetById(RdId id)
    {
      if (myRdIdToTypeMapping.TryGetValue(id, out var type))
      {
        return type;
      }

      return null;
    }

    public void AddType(Type type)
    {
      myRdIdToTypeMapping[RdId.DefineByFqn(type)] = type;
    }

    public void Register<T>() => AddType(typeof(T));
  }
}