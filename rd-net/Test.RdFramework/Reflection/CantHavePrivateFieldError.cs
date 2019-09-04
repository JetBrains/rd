using JetBrains.Rd.Reflection;

#pragma warning disable 0169

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public class CantHavePrivateFieldError
  {
    private NotRdModelData Val;
  }
}