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

namespace Test.Reflection.App
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

  static class Program
  {
    private static int ourPort = 5000;

    public static event Action<char> OnChar;

    private static readonly IPEndPoint ourIpEndPoint = new IPEndPoint(IPAddress.Loopback, ourPort);

    public static void StartClient() => Main(new [] {"client"});
    public static void StartServer() => Main(new [] {"server"});
    
    //try to start both client and server
    static void Main(string[] args)
    {
      using (var lifetimeDefinition = new LifetimeDefinition())
        MainLifetime(args, lifetimeDefinition);
    }

    private static void MainLifetime(string[] args, LifetimeDefinition lifetimeDefinition)
    {
      var lifetime = lifetimeDefinition.Lifetime;

      var reflectionSerializers = new ReflectionSerializersFacade();

      var scheduler = SingleThreadScheduler.RunOnSeparateThread(lifetime, "Scheduler");
      Protocol protocol;
      SocketWire.Base wire;


      var isServer = args.Length == 0 ? Util.Fork(args) : args[0] == "server";
      if (isServer)
      {
        Console.Title = "Server";
        wire = new SocketWire.Server(lifetime, scheduler, ourIpEndPoint);
        protocol = new Protocol("Server", reflectionSerializers.Serializers, new SequentialIdentities(IdKind.Server), scheduler, wire, lifetime);
      }
      else
      {
        Console.Title = "Client";
        wire = new SocketWire.Client(lifetime, scheduler, ourIpEndPoint);
        protocol = new Protocol("Client", reflectionSerializers.Serializers, new SequentialIdentities(IdKind.Client), scheduler, wire, lifetime);
      }

      scheduler.Queue(() => RunApplication(isServer, reflectionSerializers, lifetime, protocol));

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
          scheduler.Queue(() => OnChar?.Invoke(Console.ReadKey(true).KeyChar));

        Thread.Sleep(100);
      }
    }

    private static void RunApplication(bool isServer, ReflectionSerializersFacade facade, Lifetime lifetime, Protocol protocol)
    {
      IRootExt root;
      if (isServer)
      {
        root = facade.ActivateProxy<IRootExt>(lifetime, protocol);
      }
      else
      {
        root = facade.InitBind(new RootExt(), lifetime, protocol);
      }
      (root as RdExtReflectionBindableBase).Connected.Advise(lifetime, v => Console.WriteLine("RootExt connected: " + v));

      root.OnChar.Advise(lifetime, Console.Write);
      OnChar += c => root.OnChar.Fire(c);
    }
  }
}
