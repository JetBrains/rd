using System.Collections.Generic;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Impl;

public class RdEntitiesRegistrar
{
  private readonly Dictionary<RdId, IRdDynamic> myMap = new();

  internal void Register(Lifetime lifetime, RdId rdId, IRdDynamic dynamic)
  {
    Assertion.Assert(!rdId.IsNil);

    myMap.BlockingAddUnique(lifetime, myMap, rdId, dynamic);
  }

  public bool TryGetEntity(RdId rdId, out IRdDynamic entity)
  {
    lock (myMap)
    {
      return myMap.TryGetValue(rdId, out entity);
    }
  }
}