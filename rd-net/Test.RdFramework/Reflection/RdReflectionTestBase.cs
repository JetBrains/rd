using System;
using System.IO;
using System.Threading.Tasks;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using JetBrains.Rd.Tasks;
using NUnit.Framework;
using Test.Lifetimes;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class RdReflectionTestBase : RdFrameworkTestBase
  {
    protected ReflectionSerializersFacade CFacade;
    protected ReflectionSerializersFacade SFacade;
    private bool myRespectRpcTimeouts;

    public override void SetUp()
    {
      // increase timeouts to balance unpredictable agent performance
      myRespectRpcTimeouts = RpcTimeouts.RespectRpcTimeouts;
      RpcTimeouts.RespectRpcTimeouts = false;
      CFacade = new ReflectionSerializersFacade(allowSave: true);
      SFacade = new ReflectionSerializersFacade(allowSave: true);

      base.SetUp();
      ServerWire.AutoTransmitMode = true;
      ClientWire.AutoTransmitMode = true;
    }

    public override void TearDown()
    {
      RpcTimeouts.RespectRpcTimeouts = myRespectRpcTimeouts;
      base.TearDown();
    }

    protected void AddType(Type type)
    {
      CFacade.TypesCatalog.AddType(type);
      SFacade.TypesCatalog.AddType(type);
    }

    protected override Serializers CreateSerializers(bool isServer)
    {
      return new Serializers(TestLifetime, TaskScheduler.Default, isServer ? SFacade.Registrar : CFacade.Registrar);
    }

    protected void WithExts<T>(Action<T,T> run) where T : RdBindableBase
    {
      var c = CFacade.Activator.ActivateBind<T>(TestLifetime, ClientProtocol);
      var s = SFacade.Activator.ActivateBind<T>(TestLifetime, ServerProtocol);
      run(c, s);
    }

    protected void WithBothFacades(Action<ReflectionSerializersFacade> act)
    {
      act(CFacade);
      act(SFacade);
    }

    protected void WithExtsProxy<T1, T2>(Action<T1, T2> run) where T1 : RdBindableBase where T2 : class 
    {
      var c = CFacade.Activator.ActivateBind<T1>(TestLifetime, ClientProtocol);
      var s = SFacade.ActivateProxy<T2>(TestLifetime, ServerProtocol);
      run(c, s);
    }

    protected void SaveGeneratedAssembly()
    {
#if NET35 || NETCOREAPP
      // throw new NotSupportedException();
#else
      var generatorCache = SFacade.ProxyGenerator as ProxyGeneratorCache;
      var generator = generatorCache.GetDynamicField("myGenerator") as ProxyGenerator;

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