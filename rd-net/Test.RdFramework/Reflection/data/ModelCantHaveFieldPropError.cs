using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class ModelCantHaveFieldPropError : RdReflectionBindableBase {
    public int SomeString { get; private set; }
  }
}