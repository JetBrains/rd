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
        protected readonly int Port;

        protected CrossTestCsClientBase()
        {
            SpinWait.SpinUntil(() => FileSystem.IsFileReady(FileSystem.PortFileClosed), 5000);

            using (var stream = new StreamReader(File.OpenRead(FileSystem.PortFile)))
            {
                int.TryParse(stream.ReadLine(), out Port);
            }

            Console.Error.WriteLine($"Port is {Port}");
        }

        protected void Queue(Action action)
        {
            SingleThreadScheduler.RunOnSeparateThread(SocketLifetime, "Worker", scheduler =>
            {
                var client = new SocketWire.Client(ModelLifetime, scheduler, Port, "DemoServer");
                var serializers = new Serializers();
                Protocol = new Protocol("Server", serializers, new Identities(IdKind.Server), scheduler,
                    client, SocketLifetime);
                scheduler.Queue(action);
            });
        }
    }
}