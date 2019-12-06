using System;
using System.Diagnostics;
using System.Globalization;
using System.Linq;
using JetBrains.Core;
using Test.RdCross.Base;

namespace Test.RdCross
{
  public static class TestRunner
  {
    public static void Main(string[] args)
    {
      try
      {
        if (args.Length != 2)
        {
          throw new ArgumentException($"Wrong number of arguments:{args.Length}, expected = 2." +
                                      "Main([" +
                                      "\"CrossTest_AllEntities_CsClient\", \"C:\\Work\rd\build\\src\\main\\resources\\tmp\\CrossTest_AllEntities_KtServer_CsClient\\CrossTest_AllEntities_CsClient.tmp\"" +
                                      "]) for example.)");
        }

        var type = (Type.GetType($"Test.RdCross.Cases.Server.{args[0]}") ??
                    Type.GetType($"Test.RdCross.Cases.Client.{args[0]}")) ??
                   throw new ArgumentException($"Wrong class name={args[0]}");
        if (Activator.CreateInstance(type) is CrossTest_Cs_Base testCsBase)
        {
          CultureInfo.DefaultThreadCurrentCulture = CultureInfo.InvariantCulture;
          Console.WriteLine($"Instance of {type} created");
          testCsBase.Run(args.Skip(1).ToArray());
        }
        else
        {
          throw new ArgumentException($"{type} is not an inheritor of CrossTest_Cs_Base");
        }
      }
      catch (Exception e)
      {
        Console.Error.WriteLine(e);
        Console.Error.Flush();
        Environment.Exit(1);
      }
    }
  }
}