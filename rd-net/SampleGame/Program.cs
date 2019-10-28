using System;
using System.Net;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;
using SampleGame.Common;

namespace SampleGame
{
  [RdRpc]
  public interface IRootExt
  {
    Task Greet(string name);
    ISignal<char> OnChar { get; }
  }

  [RdExt]
  public class RootExt : RdExtReflectionBindableBase, IRootExt
  {
    public ISignal<char> OnChar { get; }

    public Task Greet(string name)
    {
      Console.WriteLine($"Hello, {name}");
      return Task.CompletedTask;
    }
  }

  class Program
  {
    private static int ourPort = 5000;

    public static event Action<char> OnChar;

    private static readonly IPEndPoint ourIpEndPoint = new IPEndPoint(IPAddress.Loopback, ourPort);

    static void Main(string[] args)
    {
      using (var lifetimeDefinition = new LifetimeDefinition())
        MainLifetime(args, lifetimeDefinition);
    }

    private static void MainLifetime(string[] args, LifetimeDefinition lifetimeDefinition)
    {
      var lifetime = lifetimeDefinition.Lifetime;

      var typeCatalog = new SimpleTypesCatalog();
      var reflectionSerializers = new ReflectionSerializersFactory(typeCatalog);
      var polymorphicTypesCatalog = new TypesRegistrar(typeCatalog, reflectionSerializers);
      var serializers = new Serializers(polymorphicTypesCatalog);
      var activator = new ReflectionRdActivator(reflectionSerializers, new ProxyGenerator(false), typeCatalog);

      var scheduler = SingleThreadScheduler.RunOnSeparateThread(lifetime, "Scheduler");
      Protocol protocol;
      SocketWire.Base wire;
      var isServer = Util.Fork(args);
      if (isServer)
      {
        Console.Title = "Server";
        wire = new SocketWire.Server(lifetime, scheduler, ourIpEndPoint);
        protocol = new Protocol("Server", serializers, new Identities(IdKind.Server), scheduler, wire, lifetime);
      }
      else
      {
        Console.Title = "Client";
        wire = new SocketWire.Client(lifetime, scheduler, ourIpEndPoint);
        protocol = new Protocol("Client", serializers, new Identities(IdKind.Client), scheduler, wire, lifetime);
      }

      scheduler.Queue(() => RunApplication(isServer, activator, lifetime, protocol));

      wire.Connected.Change.Advise(lifetime, value =>
      {
        if (value)
        {
          Console.Title += ": connected";
        }
        else
        {
          lifetimeDefinition.Terminate();
        }
      });

      while (lifetime.IsAlive)
      {
        if (Console.KeyAvailable && OnChar != null)
          scheduler.Queue(() => OnChar?.Invoke(Console.ReadKey(false).KeyChar));

        Thread.Sleep(100);
      }
    }

    private static void RunApplication(bool isServer, ReflectionRdActivator activator, Lifetime lifetime, Protocol protocol)
    {
      IRootExt root;
      if (isServer)
      {
        var type = activator.Generator.CreateType<IRootExt>();
        root = (IRootExt) activator.ActivateBind(type, lifetime, protocol);
      }
      else
      {
        root = activator.ActivateBind<RootExt>(lifetime, protocol);
      }

      root.OnChar.Advise(lifetime, Console.Write);
      OnChar += c => root.Greet(c.ToString());
    }
  }
}
