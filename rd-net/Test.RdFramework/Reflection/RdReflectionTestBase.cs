using System;
using System.IO;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdReflectionTestBase : RdFrameworkTestBase
  {
    protected ReflectionRdActivator ReflectionRdActivator;
    protected ReflectionSerializersFactory ReflectionSerializersFactory;
    protected SimpleTypesCatalog TestRdTypesCatalog;

    public override void SetUp()
    {
      ReflectionSerializersFactory = new ReflectionSerializersFactory();
      TestRdTypesCatalog = new SimpleTypesCatalog(ReflectionSerializersFactory);
      ReflectionRdActivator = new ReflectionRdActivator(ReflectionSerializersFactory, new ProxyGenerator(true), TestRdTypesCatalog);

      base.SetUp();
      ServerWire.AutoTransmitMode = true;
      ClientWire.AutoTransmitMode = true;
    }

    protected override Serializers CreateSerializers(bool isServer)
    {
      return new Serializers(TestRdTypesCatalog);
    }

    [NotNull]
    protected T CreateServerProxy<T>() where T : class
    {
      var proxyType = ReflectionRdActivator.Generator.CreateType<T>();
      var proxy = ReflectionRdActivator.ActivateBind(proxyType, TestLifetime, ServerProtocol) as T;
      Assertion.AssertNotNull(proxy, "proxy != null");
      return proxy;
    }

    protected void SaveGeneratedAssembly()
    {
#if NET35 || NETCOREAPP
      // throw new NotSupportedException();
#else
      var generator = ReflectionRdActivator.Generator;
      var modulePath = generator.DynamicModule.FullyQualifiedName;
      var proxyName = Path.GetFileName(modulePath);

      generator.DynamicAssembly.Save(generator.DynamicAssembly.FullName);

      var tempPath = Path.Combine(Path.GetTempPath(), proxyName);
      if (File.Exists(tempPath))
        File.Delete(tempPath);
      File.Move(modulePath, tempPath);
      Console.WriteLine("Proxy module saved to: " + tempPath);

#endif
    }
  }
}