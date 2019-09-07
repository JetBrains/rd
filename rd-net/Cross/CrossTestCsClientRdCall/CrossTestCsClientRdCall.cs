using demo;
using JetBrains.Rd.Tasks;
using Test.RdCross.Util;

namespace Test.RdCross
{
    internal class CrossTestCsClientTask : CrossTestCsClientBase
    {
        public static void Main(string[] args)
        {
            new CrossTestCsClientTask().Run(args);
        }

        public override void Start(string[] args)
        {
            Before(args);
            
            Queue(() =>
            {
                var demoModel = new DemoModel(ModelLifetime, Protocol);
                
                demoModel.Call.Set(c => c.ToString());
                
                demoModel.Callback.Start("Csharp").Result.Advise(ModelLifetime, 
                    result =>
                    {
                        Printer.PrintAnyway(nameof(demoModel.Callback), result);

                        Finished = true;
                    });
            });
            
            After();
        }
    }
}