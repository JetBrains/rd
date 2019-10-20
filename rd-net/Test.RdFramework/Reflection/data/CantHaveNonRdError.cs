using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public class CantHaveNonRdError : RdReflectionBindableBase
  {
    public NotRdModelData Model;
  }
}