using System;
using System.IO;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Threading;
using Test.RdCross.Util;

namespace Test.RdCross.Base
{
  // ReSharper disable once InconsistentNaming
  public abstract class CrossTest_Cs_Base
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

        protected CrossTest_Cs_Base()
        {
            SocketLifetime = SocketLifetimeDef.Lifetime;
            ModelLifetime = ModelLifetimeDef.Lifetime;
        }

        // ReSharper disable once SuggestBaseTypeForParameter
        private void Before(string[] args)
        {
            if (args.Length != 1)
            {
                throw new ArgumentException($"Wrong number of arguments for {TestName}:{args.Length}" +
                                            $"{args}");
            }

            var outputFileName = args[0];
            Console.WriteLine($"outputFileName={outputFileName}");
            var fileStream = new FileStream(outputFileName, FileMode.Create, FileAccess.Write, FileShare.ReadWrite);
            myOutputFile = new StreamWriter(fileStream);
            Console.WriteLine($"Test:{TestName} started, file={outputFileName}");
        }

        private void After()
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
            using (Log.UsingLogFactory(new CombinatorLogFactory(new LogFactoryBase[]
            {
              new TextWriterLogFactory(Console.Out, LoggingLevel.TRACE),
              new CrossTestsLogFactory(myStringWriter),
              new TestLogger.TestLogFactory()
            }))) 
            {
              try
              {
                Before(args);
                Start();
                After();
              }
              catch (Exception e)
              {
                Console.WriteLine(e);
                throw;
              }
              finally
              {
                if (myOutputFile != null)
                {
                  using (myOutputFile)
                  {
                    myOutputFile.Write(myStringWriter.ToString());
                  }
                }
                TestLogger.Logger.ThrowLoggedExceptions();
              }
            }
        }

        protected abstract void Start();
    }
}