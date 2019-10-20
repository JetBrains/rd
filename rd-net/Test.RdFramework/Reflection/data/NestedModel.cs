using JetBrains.Collections.Viewable;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public sealed class NestedModel : RdExtReflectionBindableBase
  {
    public RdProperty<string> SomeProperty { get; }
    public IViewableProperty<string> IPropertyAlsoFine { get; }

    public IViewableProperty<PropertiesNotNullOk> PropModel { get; }
    public RdSet<Animal> PolymorphicPossible { get; }
  }
}