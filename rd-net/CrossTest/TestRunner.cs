using System;
using System.Linq;

namespace Test.RdCross
{
    public class TestRunner
    {
        public static void Main(string[] args)
        {
            if (args.Length < 1)
            {
                throw new ArgumentException($"Wrong number of arguments:${args.Length}, expected = 1." +
                                            $"Main([\"CrossTestCsClientAllEntities\"]) for example.)");
            }
            var type = Type.GetType($"Test.RdCross.{args[0]}") ??
                       throw new ArgumentException($"Wrong class name={args[0]})");
            if (Activator.CreateInstance(type) is CrossTestCsBase testCsBase)
            {
                testCsBase.Run(args.Skip(1).ToArray());
            }
            else
            {
                throw new ArgumentException($"{type} is not an inheritor of CrossTestCsBase");
            }
        }
    }
}