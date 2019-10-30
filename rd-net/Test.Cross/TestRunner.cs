using System;
using System.Linq;

namespace Test.RdCross
{
    public static class TestRunner
    {
        public static void Main(string[] args)
        {
            if (args.Length != 2 )
            {
                throw new ArgumentException($"Wrong number of arguments:{args.Length}, expected = 2." +
                                            "Main([" +
                                            "\"CrossTestCsClientAllEntities\", \"C:\\Work\rd\build\\src\\main\\resources\\tmp\\CrossTestKtCsAllEntities\\CrossTestCsClientAllEntities.tmp\"" +
                                            "]) for example.)");
            }
            var type = Type.GetType($"Test.RdCross.Cases.{args[0]}") ??
                       throw new ArgumentException($"Wrong class name={args[0]}");
            if (Activator.CreateInstance(type) is CrossTestCsBase testCsBase)
            {
                Console.WriteLine($"Instance of {type} created");
                testCsBase.Run(args.Skip(1).ToArray());
            }
            else
            {
                throw new ArgumentException($"{type} is not an inheritor of CrossTestCsBase");
            }
        }
    }
}