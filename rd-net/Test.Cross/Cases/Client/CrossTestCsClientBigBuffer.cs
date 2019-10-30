using demo;
using JetBrains.Rd.Base;
using Test.RdCross.Base;

namespace Test.RdCross.Cases
{
  // ReSharper disable once UnusedType.Global
  public class CrossTestCsClientBigBuffer : CrossTestCsClientBase
  {
    protected override void Start(string[] args)
    {
      Queue(() =>
      {
        var demoModel = new DemoModel(ModelLifetime, Protocol);

        var entity = demoModel.Property_with_default;

        entity.Set(new string('5', 100_000));
        entity.Set(new string('0', 100_000));
      });
    }
  }
}