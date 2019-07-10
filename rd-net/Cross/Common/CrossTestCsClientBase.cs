using System;
using System.IO;
using System.Threading;
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
    }
}