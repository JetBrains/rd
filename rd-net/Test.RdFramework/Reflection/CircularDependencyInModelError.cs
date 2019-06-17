using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class CircularDependencyInModelError : RdReflectionBindableBase
  {
    public IViewableProperty<CircularDependencyModelError> ParentRef { get; }
  }
}