using System;

namespace JetBrains.Rd.Reflection
{
  public interface ITypesCatalog
  {
    Type? GetById(RdId id);
    void AddType(Type type);
  }
}