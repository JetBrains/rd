using System;
using System.IO;
using Test.RdCross.Util;

namespace Test.RdCross
{
    public abstract class CrossTestCsClientBase : CrossTestCsBase
    {
        protected readonly int Port;
        
        protected CrossTestCsClientBase()
        {
            using (FileSystemWatcher watcher = new FileSystemWatcher(FileSystem.RdTmpDir))
            {
                watcher.WaitForChanged(WatcherChangeTypes.Created, 5_000);
            }

            var stream = new StreamReader(File.OpenRead(FileSystem.PortFile));
            int.TryParse(stream.ReadLine(), out Port);
            
            Console.Error.WriteLine("Port is " + Port);
        }
    }
}