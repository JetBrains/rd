using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public sealed class CircularDependencyModel2Error : RdReflectionBindableBase
  {
    public IViewableProperty<CircularDependencyModelError> Value;
  }

  [RdModel]
  public sealed class CircularDependencyModelError : RdReflectionBindableBase
  {
    public IViewableProperty<CircularDependencyModel2Error> Value;
  }

  [RdModel]
  public class CircularDependencyNestedModel2Error : RdReflectionBindableBase
  {
    public CircularDependencyNestedModel1Error Value;
  }

  [RdModel]
  public class CircularDependencyNestedModel1Error : RdReflectionBindableBase
  {
    public CircularDependencyNestedModel2Error Value;
  }
}