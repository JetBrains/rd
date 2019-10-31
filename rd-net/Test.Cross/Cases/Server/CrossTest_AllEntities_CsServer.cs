using demo;
using Test.RdCross.Base;
using Test.RdCross.Util;

namespace Test.RdCross.Cases.Server
{
  // ReSharper disable once UnusedType.Global
  // ReSharper disable once InconsistentNaming
  public class CrossTest_AllEntities_CsServer : CrossTest_CsServer_Base
  {
    protected override void Start(string[] args)
    {
      Logging.TrackAction("Checking constant", CrossTest_AllEntities.CheckConstants);

      Queue(() =>
      {
        var model = Logging.TrackAction("Creating DemoModel", () => new DemoModel(ModelLifetime, Protocol));
        var extModel = Logging.TrackAction("Creating ExtModel", () => model.GetExtModel());

        Logging.TrackAction("Firing", () =>
          CrossTest_AllEntities.FireAll(model, extModel)
        );
      });
    }
  }
}