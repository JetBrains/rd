using System;
using System.IO;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Util;
using JetBrains.Threading;
using Test.RdCross.Util;

namespace Test.RdCross
{
    public abstract class CrossTestCsBase
    {
        private string TestName => GetType().Name;

        protected readonly PrettyPrinter Printer = new PrettyPrinter();
        private StreamWriter myOutputFile;
        
        protected volatile bool Finished;
        
        protected IProtocol Protocol { get; set; }
        private LifetimeDefinition ModelLifetimeDef { get; } = Lifetime.Eternal.CreateNested();
        private LifetimeDefinition SocketLifetimeDef { get; } = Lifetime.Eternal.CreateNested();

        protected Lifetime ModelLifetime { get; }
        protected Lifetime SocketLifetime { get; }


        protected CrossTestCsBase()
        {
            SocketLifetime = SocketLifetimeDef.Lifetime;
            ModelLifetime = ModelLifetimeDef.Lifetime;
        }

        protected void Before(string[] args)
        {
            if (args.Length != 1)
            {
                throw new ArgumentException($"Wrong number of arguments for {TestName}:{args.Length}" +
                                            $"{args}");
            }

            var outputFileName = args[0];
            Console.WriteLine($"outputFileName={outputFileName}");
            myOutputFile = new StreamWriter(outputFileName);
            Console.WriteLine($"Test:{TestName} started, file={outputFileName}");
        }

        protected void After()
        {
            Logging.LogWithTime("Spinning started");
            SpinWaitEx.SpinUntil(ModelLifetime, 2000_000, () => Finished);
            SpinWaitEx.SpinUntil(ModelLifetime, 1_000, () => false);
            Logging.LogWithTime($"Spinning finished, Finished={Finished}");

            SocketLifetimeDef.Terminate();
            ModelLifetimeDef.Terminate();

            using (myOutputFile)
            {
                myOutputFile.Write(Printer.ToString());
            }
        }


        public void Run(string[] args)
        {
            Console.WriteLine($"Current time:{DateTime.Now:G}");
            using(Log.UsingLogFactory(new TextWriterLogFactory(Console.Out, LoggingLevel.TRACE)))
            {
                Start(args);
            }
        }

        protected abstract void Start(string[] args);

    }
}