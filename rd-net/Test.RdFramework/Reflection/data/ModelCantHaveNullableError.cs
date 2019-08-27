using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class ModelCantHaveNullableError : RdReflectionBindableBase {
    public string SomeString;
  }
}