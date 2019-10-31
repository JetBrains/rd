using System;
using System.IO;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using Test.RdCross.Util;

namespace Test.RdCross.Base
{
  public abstract class CrossTestCsClientBase : CrossTestCsBase
  {
    private readonly int myPort;

    protected CrossTestCsClientBase()
    {
      Console.WriteLine($"Waiting for port being written in file={FileSystem.PortFile}");

      SpinWait.SpinUntil(() => FileSystem.IsFileReady(FileSystem.PortFileClosed), 5_000);

      if (!FileSystem.IsFileReady(FileSystem.PortFileClosed))
      {
        Console.Error.WriteLine("Stamp file wasn't created during timeout");
        Environment.Exit(1);
      }

      using (var stream = new StreamReader(File.OpenRead(FileSystem.PortFile)))
      {
        int.TryParse(stream.ReadLine(), out myPort);
      }

      Console.WriteLine($"Port is {myPort}");
    }

    protected void Queue(Action action)
    {
      SingleThreadScheduler.RunOnSeparateThread(SocketLifetime, "Worker", scheduler =>
      {
        var client = new SocketWire.Client(ModelLifetime, scheduler, myPort, "DemoClient");
        var serializers = new Serializers();
        Protocol = new Protocol("Server", serializers, new Identities(IdKind.Client), scheduler,
          client, SocketLifetime);
        scheduler.Queue(action);
      });
    }
  }
}