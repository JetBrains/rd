using System;
using System.IO;
using JetBrains.Rd;
using JetBrains.Rd.Impl;
using Test.RdCross.Util;

namespace Test.RdCross
{
    public abstract class CrossTestClientBase : CrossTestBase
    {
        protected int Port;
        
        protected CrossTestClientBase()
        {
            using (FileSystemWatcher watcher = new FileSystemWatcher(FileSystem.PortFile))
            {
                watcher.WaitForChanged(WatcherChangeTypes.Created, 5_000);
            }

            var stream = new StreamReader(File.OpenRead(FileSystem.PortFile));
            Int32.TryParse(stream.ReadLine(), out Port);
            
            Console.Error.WriteLine("Port is " + Port);
        }
    }
}