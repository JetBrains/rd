using demo;
using JetBrains.Collections.Viewable;
using Test.RdCross.Util;

namespace Test.RdCross
{
    public class CrossTestClientBigBuffer : CrossTestClientBase
    {
        public static void Main(string[] args)
        {
            new CrossTestClientAllEntities().Run(args);
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