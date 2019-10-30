using System;
using System.Reflection;
using System.Reflection.Emit;

namespace JetBrains.Rd.Reflection
{
  public interface IProxyGenerator
  {
    Type CreateType<TInterface>() where TInterface : class;
    DynamicMethod CreateAdapter(Type selfType, MethodInfo method);
  }
}