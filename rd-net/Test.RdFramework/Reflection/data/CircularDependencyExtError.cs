using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class CircularDependencyExtError : RdReflectionBindableBase
  {
    public CircularDependencyExt2Error ChildRef { get; }
  }
}