using System;
using System.IO;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using Test.RdCross.Util;

namespace Test.RdCross
{
    public abstract class CrossTestCsClientBase : CrossTestCsBase
    {
      private readonly int Port;

        protected CrossTestCsClientBase()
        {
            Console.WriteLine($"Waiting for port being written in file={FileSystem.PortFile}");
            
            SpinWait.SpinUntil(() => FileSystem.IsFileReady(FileSystem.PortFileClosed), 5000);

            using (var stream = new StreamReader(File.OpenRead(FileSystem.PortFile)))
            {
                int.TryParse(stream.ReadLine(), out Port);
            }

            Console.WriteLine($"Port is {Port}");
        }

        protected void Queue(Action action)
        {
            SingleThreadScheduler.RunOnSeparateThread(SocketLifetime, "Worker", scheduler =>
            {
                var client = new SocketWire.Client(ModelLifetime, scheduler, Port, "DemoClient");
                var serializers = new Serializers();
                Protocol = new Protocol("Server", serializers, new Identities(IdKind.Client), scheduler,
                    client, SocketLifetime);
                scheduler.Queue(action);
            });
        }
    }
}