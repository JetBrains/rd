using demo;
using Test.RdCross.Util;

namespace Test.RdCross
{
    public class CrossTestCsClientBigBuffer : CrossTestCsClientBase
    {
        public static void Main(string[] args)
        {
            new CrossTestCsClientBigBuffer().Run(args);
        }

        public override void Run(string[] args)
        {
            Before(args);

            var demoModel = new DemoModel(ModelLifetime, Protocol);

            var entity = demoModel.Property_with_default;

            entity.Advise(ModelLifetime, it =>
            {
                if (!entity.IsLocalChange() && entity.Value != DemoModel.const_for_default)
                {
                    Printer.PrintIfRemoteChange(entity, "property_with_default", it);

                    Finished = true;
                }
            });

            After();
        }
    }
}