using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class CircularDependencyExtError : RdExtReflectionBindableBase
  {
    public CircularDependencyExt2Error ChildRef { get; }
  }
}