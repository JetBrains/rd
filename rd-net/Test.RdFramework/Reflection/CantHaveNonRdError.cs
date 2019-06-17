using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public class CantHaveNonRdError
  {
    public NotRdModelData Model;
  }
}