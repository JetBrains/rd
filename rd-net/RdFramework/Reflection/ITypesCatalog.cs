using System;
using JetBrains.Annotations;

namespace JetBrains.Rd.Reflection
{
  public interface ITypesCatalog
  {
    [CanBeNull] Type GetById(RdId id);
    void AddType([NotNull] Type type);
  }
}