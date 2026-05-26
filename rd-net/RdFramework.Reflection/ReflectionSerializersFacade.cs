using System;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;

namespace JetBrains.Rd.Reflection
{
  public class ReflectionSerializersFacade
  {
    public ReflectionRdActivator Activator { get; }

    public ITypesCatalog TypesCatalog { get; }

    public IScalarSerializers ScalarSerializers { get; }

    public ReflectionSerializers Serializers { get; }

    public IProxyGenerator ProxyGenerator { get; }

    public ReflectionSerializersFacade() : this(null)
    {
    }

    public ReflectionSerializersFacade(ITypesCatalog? typesCatalog = null, IScalarSerializers? scalarSerializers = null,
      ReflectionSerializers? reflectionSerializers = null, IProxyGenerator? proxyGenerator = null,
      ReflectionRdActivator? activator = null, bool allowSave = false, Predicate<Type>? blackListChecker = null)
    {
      TypesCatalog = typesCatalog ?? new SimpleTypesCatalog();
      ScalarSerializers = scalarSerializers ?? new ScalarSerializer(TypesCatalog, blackListChecker);
      Serializers = reflectionSerializers ?? new ReflectionSerializers(TypesCatalog, ScalarSerializers);

      ProxyGenerator = proxyGenerator ?? new ProxyGeneratorCache(new ProxyGenerator(allowSave));
      Activator = activator ?? new ReflectionRdActivator(Serializers, ProxyGenerator, TypesCatalog);
    }

    public TInterface ActivateProxy<TInterface>(Lifetime lifetime, IProtocol protocol) where TInterface : class
    {
      var type = ProxyGenerator.CreateType<TInterface>();
      var proxyInstance = Activator.ActivateBind(type, lifetime, protocol) as TInterface;
      Assertion.AssertNotNull(proxyInstance, "Unable to cast proxy to desired interface ({0})", typeof(TInterface));
      return proxyInstance;
    }

    public T InitBind<T>(T instance, Lifetime lifetime, IProtocol protocol)
      where T : IRdBindable
    {
      Activator.ReflectionInit(instance);
      Bind(instance, lifetime, protocol);
      return instance;
    }

    private static void Bind(IRdBindable instance, Lifetime lifetime, IProtocol protocol)
    {
      var typename = ReflectionRdActivator.GetTypeName(instance.GetType());
      instance.Identify(protocol.Identities, protocol.Identities.Mix(RdId.Root, typename));
      instance.BindTopLevel(lifetime, protocol, typename);
    }
  }
}