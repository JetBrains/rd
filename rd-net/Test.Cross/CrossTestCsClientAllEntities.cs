using System;
using System.Diagnostics;
using demo;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using Test.RdCross.Util;

namespace Test.RdCross
{
    // ReSharper disable once UnusedMember.Global
    public class CrossTestCsClientAllEntities : CrossTestCsClientBase
    {
      static void FireAll(DemoModel model, ExtModel extModel)
        {
            model.Boolean_property.Set(false);

            model.Boolean_array.Set(new[] {false, true, false});

            var scalar = new MyScalar(
                false,
                50,
                32000,
                1000000000,
                -2000000000000000000,
                3.14f,
                -123456789.012345678,
                byte.MaxValue - 1,
                ushort.MaxValue - 1,
                uint.MaxValue - 1,
                ulong.MaxValue - 1,
                MyEnum.net,
                Flags.anyFlag | Flags.netFlag
            );

            // ReSharper disable once UnusedVariable
            // ReSharper disable once InconsistentNaming
            var (_bool, _byte, _short, _int, _long, _float, _double, _unsigned_byte, unsigned_short, _unsigned_int, _unsigned_long, _my_enum, _flags) = scalar;
            var (first, second) = new ComplicatedPair(new Derived("First"), new Derived("Second"));
            
            model.Scalar.Set(scalar);

            model.Set.Add(50);

            model.MapLongToString.Add(50, "C#");

            const string valA = "C#";
            const string valB = "protocol";

            model.Interned_string.Set(valA);
            model.Interned_string.Set(valA);
            model.Interned_string.Set(valB);
            model.Interned_string.Set(valB);
            model.Interned_string.Set(valA);

            var derived = new Derived("C# instance");
            model.Polymorphic.Set(derived);
            model.Enum.Value = MyEnum.net;
            extModel.Checker.Fire();
        }

        protected override void Start(string[] args)
        {
            Before(args);

            Logging.TrackAction("Checking constant", CheckConstants);
            
            Queue(() =>
            {
                var model = Logging.TrackAction("Creating DemoModel", () => new DemoModel(ModelLifetime, Protocol));
                var extModel = Logging.TrackAction("Creating ExtModel", () => model.GetExtModel());
                
                Logging.TrackAction("Firing", () => 
                  FireAll(model, extModel)
                );
            });

            After();
        }

        private static void CheckConstants()
        {
            Assertion.Assert(DemoModel.const_toplevel, "DemoModel.const_toplevel");
            Assertion.Assert(ConstUtil.const_enum == MyEnum.@default, "ConstUtil.const_enum == MyEnum.@default");
            Assertion.Assert(ConstUtil.const_string == "const_string_value",
                "ConstUtil.const_string == 'const_string_value'");
            Assertion.Assert(Base.const_base == 'B', "Base.const_base == 'B'");
        }
    }
}