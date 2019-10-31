using demo;
using JetBrains.Rd.Tasks;
using Test.RdCross.Base;

namespace Test.RdCross.Cases.Client
{
  // ReSharper disable once UnusedType.Global
  // ReSharper disable once InconsistentNaming
  internal class CrossTest_RdCall_CsClient : CrossTest_CsClient_Base
  {
    protected override void Start(string[] args)
    {
      Queue(() =>
      {
        var demoModel = new DemoModel(ModelLifetime, Protocol);

        demoModel.Call.Set((lifetime, c) => RdTask<string>.Successful(c.ToString()));

        demoModel.Callback.Start("Csharp");
      });
    }
  }
}