using System;
using System.Collections.Generic;

namespace JetBrains.Rd.Reflection
{
  public class SimpleTypesCatalog : ITypesCatalog
  {
    private readonly Dictionary<RdId, Type> myRdIdToTypeMapping = new Dictionary<RdId, Type>();

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