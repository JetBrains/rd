using demo;
using JetBrains.Rd.Base;
using Test.RdCross.Util;

namespace Test.RdCross
{
    // ReSharper disable once UnusedMember.Global
    public class CrossTestCsClientBigBuffer : CrossTestCsClientBase
    {
        public override void Start(string[] args)
        {
            Before(args);

            Queue(() =>
            {
                var demoModel = new DemoModel(ModelLifetime, Protocol);

                var entity = demoModel.Property_with_default;

                int count = 0;

                entity.Advise(ModelLifetime, it =>
                {
                    if (!entity.IsLocalChange() && entity.Value != DemoModel.const_for_default)
                    {
                        Printer.PrintIfRemoteChange(entity, "property_with_default", it);

                        if (++count == 2)
                        {
                            Finished = true;
                        }
                    }
                });

                entity.Set(new string('5', 100_000));
                entity.Set(new string('0', 100_000));
            });

            After();
        }
    }
}