using System;
using System.Diagnostics.Eventing.Reader;
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
  public class ProxyGeneratorTestBase : RdFrameworkTestBase
  {
    protected ReflectionRdActivator ReflectionRdActivator;
    protected ReflectionSerializers ReflectionSerializers;
    protected TestRdTypesCatalog TestRdTypesCatalog;

    public override void SetUp()
    {
      ReflectionSerializers = new ReflectionSerializers();
      ReflectionRdActivator = new ReflectionRdActivator(ReflectionSerializers, new ProxyGenerator(true), null);
      TestRdTypesCatalog = new TestRdTypesCatalog(ReflectionSerializers);

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
      throw new NotSupportedException();
#else
      var generator = ReflectionRdActivator.Generator;
      var modulePath = generator.ModuleBuilder.FullyQualifiedName;
      var proxyName = Path.GetFileName(modulePath);

      generator.AssemblyBuilder.Save(generator.AssemblyBuilder.FullName);

      var tempPath = Path.Combine(Path.GetTempPath(), proxyName);
      if (File.Exists(tempPath))
        File.Delete(tempPath);
      File.Move(modulePath, tempPath);
      Console.WriteLine("Proxy module saved to: " + tempPath);

#endif
    }
  }
}