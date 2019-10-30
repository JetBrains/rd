using demo;
using Test.RdCross.Base;
using Test.RdCross.Util;

namespace Test.RdCross.Cases
{
  // ReSharper disable once UnusedType.Global
  public class CrossTestCsClientAllEntities : CrossTestCsClientBase
  {
    protected override void Start(string[] args)
    {
      Logging.TrackAction("Checking constant", CrossTestCsAllEntities.CheckConstants);

      Queue(() =>
      {
        var model = Logging.TrackAction("Creating DemoModel", () => new DemoModel(ModelLifetime, Protocol));
        var extModel = Logging.TrackAction("Creating ExtModel", () => model.GetExtModel());

        Logging.TrackAction("Firing", () =>
          CrossTestCsAllEntities.FireAll(model, extModel)
        );
      });
    }
  }
}