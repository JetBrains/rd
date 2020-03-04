using System;
using System.Reflection;
using System.Reflection.Emit;
using JetBrains.Annotations;

namespace JetBrains.Rd.Reflection
{
  public interface IProxyGenerator
  {
    Type CreateType(Type interfaceType);
    DynamicMethod CreateAdapter(Type selfType, MethodInfo method);
  }

  public static class ProxyGeneratorEx
  {
    public static Type CreateType<TInterface>([NotNull] this IProxyGenerator proxyGenerator) where TInterface : class
    {
      return proxyGenerator.CreateType(typeof(TInterface));
    }
  }
}