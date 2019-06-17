using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class CircularDependencyExt2Error : RdReflectionBindableBase
  {
    public CircularDependencyExtError ParentRef { get; }
  }
}