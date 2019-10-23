using System;
using System.IO;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Threading;
using Test.RdCross.Util;

namespace Test.RdCross
{
    public abstract class CrossTestCsBase
    {
        private string TestName => GetType().Name;

        private StreamWriter myOutputFile;

        private const int SpinningTimeout = 10_000;
        
        protected IProtocol Protocol { get; set; }
        private LifetimeDefinition ModelLifetimeDef { get; } = Lifetime.Eternal.CreateNested();
        private LifetimeDefinition SocketLifetimeDef { get; } = Lifetime.Eternal.CreateNested();

        protected Lifetime ModelLifetime { get; }
        protected Lifetime SocketLifetime { get; }

        private readonly StringWriter myStringWriter = new StringWriter();

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
            SpinWaitEx.SpinUntil(ModelLifetime, SpinningTimeout, () => false);
            Logging.LogWithTime("Spinning finished");

            SocketLifetimeDef.Terminate();
            ModelLifetimeDef.Terminate();
        }


        public void Run(string[] args)
        {
            Console.WriteLine($"Current time:{DateTime.Now:G}");
            using (Log.UsingLogFactory(new TextWriterLogFactory(Console.Out, LoggingLevel.TRACE)))
            {
              using (Log.UsingLogFactory(new CrossTestsLogFactory(myStringWriter)))
              {
                try
                {
                  Start(args);
                }
                catch (Exception e)
                {
                  Console.WriteLine(e);
                  throw;
                }
                finally
                {
                  using (myOutputFile)
                  {
                    myOutputFile?.Write(myStringWriter.ToString());                  
                  }
                }
              }
            }
        }

        protected abstract void Start(string[] args);

    }
}