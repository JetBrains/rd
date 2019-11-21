using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class CircularDependencyExt2Error : RdExtReflectionBindableBase
  {
    public CircularDependencyExtError ParentRef { get; }
  }
}