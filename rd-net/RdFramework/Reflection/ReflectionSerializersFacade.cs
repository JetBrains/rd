using JetBrains.Annotations;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;

namespace JetBrains.Rd.Reflection
{
  public class ReflectionSerializersFacade
  {
    [NotNull]
    public ReflectionRdActivator Activator { get; }

    [NotNull]
    public ITypesCatalog TypesCatalog { get; }

    [NotNull]
    public IScalarSerializers ScalarSerializers { get; }

    [NotNull]
    public ReflectionSerializersFactory SerializersFactory { get; }

    [NotNull]
    public IProxyGenerator ProxyGenerator { get; }

    [NotNull]
    public ITypesRegistrar Registrar { get; }

    public ReflectionSerializersFacade() : this(null)
    {
    }

    public ReflectionSerializersFacade([CanBeNull] ITypesCatalog typesCatalog = null, [CanBeNull] IScalarSerializers scalarSerializers = null, [CanBeNull] ReflectionSerializersFactory reflectionSerializers = null, [CanBeNull] IProxyGenerator proxyGenerator = null, [CanBeNull] ReflectionRdActivator activator = null, [CanBeNull] TypesRegistrar registrar = null, bool allowSave = false)
    {
      TypesCatalog = typesCatalog ?? new SimpleTypesCatalog();
      ScalarSerializers = scalarSerializers ?? new ScalarSerializer(TypesCatalog);
      SerializersFactory = reflectionSerializers ?? new ReflectionSerializersFactory(TypesCatalog, ScalarSerializers);

      ProxyGenerator = proxyGenerator ?? new ProxyGeneratorCache(new ProxyGenerator(ScalarSerializers, allowSave));
      Activator = activator ?? new ReflectionRdActivator(SerializersFactory, ProxyGenerator, TypesCatalog);
      Registrar = registrar ?? new TypesRegistrar(TypesCatalog, SerializersFactory);
    }

    public TInterface ActivateProxy<TInterface>(Lifetime lifetime, IProtocol protocol) where TInterface : class
    {
      var type = ProxyGenerator.CreateType<TInterface>();
      var proxyInstance = Activator.ActivateBind(type, lifetime, protocol) as TInterface;
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
      var typename = instance.GetType().Name;
      instance.Identify(protocol.Identities, RdId.Root.Mix(typename));
      instance.Bind(lifetime, protocol, typename);
    }
  }
}