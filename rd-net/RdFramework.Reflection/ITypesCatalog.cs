using System;

namespace JetBrains.Rd.Reflection
{
  public interface ITypesCatalog
  {
    Type? GetById(RdId id);
    RdId GetByType(Type type);
    void AddType(Type type);
  }
}