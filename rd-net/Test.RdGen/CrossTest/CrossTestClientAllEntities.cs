using System;
using System.IO;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Annotations;
using demo;

namespace JetBrains.Rd.Util
{
    public static class CrossTest
    {
        private static IScheduler ourScheduler;
        private static bool finished;

        static Protocol Client(Lifetime lifetime, int port)
        {
            ourScheduler = new RdSimpleDispatcher(lifetime, null, "RdSimpleDispatcher");
            var client = new SocketWire.Client(lifetime, ourScheduler, port,
                "DemoServer");
            return new Protocol("Server", new Serializers(), new Identities(IdKind.Server), ourScheduler,
                client, lifetime);
        }

        public static void Main(string[] args)
        {
            var lifetimeDef = Lifetime.Eternal.CreateNested();
            var socketLifetimeDef = Lifetime.Eternal.CreateNested();

            var lifetime = lifetimeDef.Lifetime;
            var socketLifetime = socketLifetimeDef.Lifetime;

            var fileStream = File.Create(Path.Combine(Path.GetTempPath(), "rd/port.txt"));
            Directory.CreateDirectory(Directory.GetParent(fileStream.Name).Name);

            int port;
            using (StreamReader file = new StreamReader(fileStream))
            {
                port = Convert.ToInt32(file.ReadLine());
            }

            var protocol = Client(socketLifetime, port);

            var printer = new PrettyPrinter();
            SingleThreadScheduler.RunInCurrentStackframe(socketLifetime, "action", scheduler =>
            {
                scheduler.Queue(() =>
                {
                    DemoModel model = new DemoModel(socketLifetime, protocol);
                    ExtModel extModel = model.GetExtModel();

                    AdviseAll(lifetime, model, extModel, printer);
                    FireAll(model, extModel);
                });
            });
            SpinWait.SpinUntil(() => finished, 10_000);

            Thread.Sleep(TimeSpan.FromSeconds(10));

            lifetimeDef.Terminate();

            Console.WriteLine(printer.ToString());
        }

        private static bool IsLocalChange<T>(this ISource<T> entity)
        {
            return ((RdReactiveBase) entity).IsLocalChange;
        }

        static void PrintIfRemoteChange<T>([NotNull] this PrettyPrinter printer,
            [NotNull] ISource<T> entity,
            [NotNull] string entityName,
            [NotNull] params object[] values)
        {
            if (!(entity.IsLocalChange()))
            {
                "***".PrintEx(printer);
                (entityName + ":").PrintEx(printer);
                foreach (var value in values)
                {
                    value.PrintEx(printer);
                }
            }
        }

        static void AdviseAll(Lifetime lifetime, DemoModel model, ExtModel extModel, PrettyPrinter printer)
        {
            model.Boolean_property.Advise(lifetime,
                it => { printer.PrintIfRemoteChange(model.Boolean_property, "Boolean_property", it); });

            model.Boolean_array.Advise(lifetime,
                it => { printer.PrintIfRemoteChange(model.Boolean_array, "Boolean_array", it); });

            model.Scalar.Advise(lifetime, it => { printer.PrintIfRemoteChange(model.Scalar, "Scalar", it); });

            model.Ubyte.Advise(lifetime, it => { printer.PrintIfRemoteChange(model.Ubyte, "Ubyte", it); });

            model.Ubyte_array.Advise(lifetime,
                it => { printer.PrintIfRemoteChange(model.Ubyte_array, "Ubyte_array", it); });

            model.List.Advise(lifetime, e => { printer.PrintIfRemoteChange(model.List, "List", e); });

            model.Set.Advise(lifetime, e => { printer.PrintIfRemoteChange(model.Set, "Set", e); });

            model.MapLongToString.Advise(lifetime,
                e => { printer.PrintIfRemoteChange(model.MapLongToString, "MapLongToString", e); });

            /*model.Callback.Set(s =>
            {
                printer.Print("RdTask:");
                printer.Print(s);

                return 50;
            });*/

            model.Interned_string.Advise(lifetime,
                it => { printer.PrintIfRemoteChange(model.Interned_string, "Interned_string", it); });

            model.Polymorphic.Advise(lifetime,
                it => { printer.PrintIfRemoteChange(model.Polymorphic, "Polymorphic", it); });

            model.Enum.Advise(lifetime, it =>
            {
                printer.PrintIfRemoteChange(model.Enum, "Enum", it);
                if (!model.Enum.IsLocalChange())
                {
                    finished = true;
                }
            });

            extModel.Checker.Advise(lifetime, () =>
            {
                printer.Print("ExtModel:Checker:");
                "Unit".PrintEx(printer);
            });
        }

        static void FireAll(DemoModel model, ExtModel extModel)
        {
            model.Boolean_property.Set(false);

            model.Boolean_array.Set(new[] {false, true, false});

            var scalar = new MyScalar(false,
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
                MyEnum.net
            );
            model.Scalar.Set(scalar);

            model.Set.Add(50);

            model.MapLongToString.Add(50, "C#");

            var valA = "C#";
            var valB = "protocol";

            // var res = model.get_call().sync(L'c');

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
    }
}