using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class ModelCantHaveNullableError : RdExtReflectionBindableBase {
    public string SomeString;
  }
}