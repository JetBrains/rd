using System;
using System.IO;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using Test.RdCross.Util;

namespace Test.RdCross.Base
{
  // ReSharper disable once InconsistentNaming
  public abstract class CrossTest_CsClient_Base : CrossTest_Cs_Base
  {
    private readonly int myPort;

    protected CrossTest_CsClient_Base()
    {
      static bool StampFileExists() => File.Exists(FileSystem.PortFileStamp);

      Console.WriteLine($"Waiting for port being written in file={FileSystem.PortFile}");

      SpinWait.SpinUntil(StampFileExists, 5_000);

      if (!StampFileExists())
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