using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public class CircularDependencyModelError : RdReflectionBindableBase
  {
    public IViewableProperty<CircularDependencyModel2Error> Value;
  }
}