using JetBrains.Lifetimes;

namespace JetBrains.Rd.Reflection
{
  public static class RdReflectionBindableBaseExtensions
  {
    public static T InitBind<T>(this T instance, ReflectionRdActivator activator, Lifetime lifetime, IProtocol protocol)
      where T : RdReflectionBindableBase
    {
      activator.ReflectionInit(instance);
      Bind(instance, lifetime, protocol);
      return instance;
    }

    internal static T Bind<T>(this T instance, Lifetime lifetime, IProtocol protocol)
      where T : RdReflectionBindableBase
    {
      var typename = instance.GetType().Name;
      instance.Identify(protocol.Identities, RdId.Root.Mix(typename));
      instance.Bind(lifetime, protocol, typename);
      return instance;
    }
  }

  public static class RdExtReflectionBindableBaseExtensions
  {
    public static T InitBind<T>(this T instance, ReflectionRdActivator activator, Lifetime lifetime, IProtocol protocol)
      where T : RdExtReflectionBindableBase
    {
      activator.ReflectionInit(instance);
      Bind(instance, lifetime, protocol);
      return instance;
    }

    internal static T Bind<T>(this T instance, Lifetime lifetime, IProtocol protocol)
      where T : RdExtReflectionBindableBase
    {
      var typename = instance.GetType().Name;
      instance.Identify(protocol.Identities, RdId.Root.Mix(typename));
      instance.Bind(lifetime, protocol, typename);
      return instance;
    }
  }
}