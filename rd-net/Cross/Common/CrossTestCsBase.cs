using System;
using System.IO;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Util;
using JetBrains.Threading;

namespace Test.RdCross
{
    public abstract class CrossTestCsBase
    {
        private string TestName => GetType().Name;

        protected readonly PrettyPrinter Printer = new PrettyPrinter();
        private StreamWriter OutputFile;
        
        protected volatile bool Finished;
        
        protected IProtocol Protocol { get; set; }
        protected LifetimeDefinition ModelLifetimeDef { get; } = Lifetime.Eternal.CreateNested();
        protected LifetimeDefinition SocketLifetimeDef { get; } = Lifetime.Eternal.CreateNested();

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
            OutputFile = new StreamWriter(outputFileName);
            Console.WriteLine($"Test:{TestName} started, file={outputFileName}");
        }

        protected void After()
        {
            SpinWaitEx.SpinUntil(ModelLifetime, 10_000, () => Finished);
            SpinWaitEx.SpinUntil(ModelLifetime, 1_000, () => false);

            SocketLifetimeDef.Terminate();
            ModelLifetimeDef.Terminate();

            using (OutputFile)
            {
                OutputFile.Write(Printer.ToString());
            }
        }


        public void Run(string[] args)
        {
            using(Log.UsingLogFactory(new TextWriterLogFactory(Console.Out, LoggingLevel.TRACE)))
            {
                Start(args);
            }
        }
        
        public abstract void Start(string[] args);

    }
}