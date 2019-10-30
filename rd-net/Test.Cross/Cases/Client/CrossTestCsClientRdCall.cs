using demo;
using JetBrains.Rd.Tasks;
using Test.RdCross.Base;

namespace Test.RdCross.Cases
{
  // ReSharper disable once UnusedType.Global
  internal class CrossTestCsClientRdCall : CrossTestCsClientBase
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