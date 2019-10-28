using System;
using System.IO;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdReflectionTestBase : RdFrameworkTestBase
  {
    protected ReflectionRdActivator ReflectionRdActivator => Facade.Activator;
    protected ReflectionSerializersFactory ReflectionSerializersFactory => Facade.SerializersFactory;
    protected ITypesCatalog TestRdTypesCatalog => new SimpleTypesCatalog();
    protected ReflectionSerializersFacade Facade;

    public override void SetUp()
    {
      Facade = new ReflectionSerializersFacade(TestRdTypesCatalog,  proxyGenerator: new ProxyGenerator(TestRdTypesCatalog, true));

      base.SetUp();
      ServerWire.AutoTransmitMode = true;
      ClientWire.AutoTransmitMode = true;
    }

    protected override Serializers CreateSerializers(bool isServer)
    {
      return new Serializers(Facade.Registrar);
    }

    protected void WithExts<T>(Action<T,T> run) where T : RdBindableBase
    {
      var c = ReflectionRdActivator.ActivateBind<T>(TestLifetime, ClientProtocol);
      var s = ReflectionRdActivator.ActivateBind<T>(TestLifetime, ServerProtocol);
      run(c, s);
    }

    protected void SaveGeneratedAssembly()
    {
#if NET35 || NETCOREAPP
      // throw new NotSupportedException();
#else
      var generator = ReflectionRdActivator.Generator as ProxyGenerator;
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