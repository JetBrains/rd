using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public class CircularDependencyModel2Error : RdReflectionBindableBase
  {
    public IViewableProperty<CircularDependencyModelError> Value;
  }
}