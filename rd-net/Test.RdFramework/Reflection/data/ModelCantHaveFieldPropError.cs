using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class ModelCantHaveFieldPropError : RdExtReflectionBindableBase {
    public int SomeString { get; private set; }
  }
}