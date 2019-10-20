using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class CircularDependencyInModelError : RdExtReflectionBindableBase
  {
    public IViewableProperty<CircularDependencyModelError> ParentRef { get; }
  }
}