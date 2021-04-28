using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public class CircularDependencyInModelError : RdExtReflectionBindableBase
  {
    public IViewableProperty<CircularDependencyModelError> ParentRef { get; }
  }

  [RdModel]
  public class CircularDependencyModel2Error : RdReflectionBindableBase
  {
    public IViewableProperty<CircularDependencyModelError> Value;
  }

  [RdModel]
  public class CircularDependencyModelError : RdReflectionBindableBase
  {
    public IViewableProperty<CircularDependencyModel2Error> Value;
  }
}