using JetBrains.Collections.Viewable;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public sealed class RootModel : RdExtReflectionBindableBase
  {
    // Can be nested but will not be activated and bind
    public NestedModel Nested { get; }

    public IViewableProperty<EmptyOK> EmptyOK { get; }
    public IViewableProperty<ModelSample> FieldsNotNullOk { get; }
    public IViewableProperty<FieldsNullableOk> FieldsNullableOk { get; }
    public IViewableProperty<PropertiesNotNullOk> PropertiesNotNullOk { get; }

    [RdExt]
    public sealed class NestedModel : RdExtReflectionBindableBase
    {
      public RdProperty<string> SomeProperty { get; }
      public IViewableProperty<string> IPropertyAlsoFine { get; }

      public IViewableProperty<PropertiesNotNullOk> PropModel { get; }
      public RdSet<Animal> PolymorphicPossible { get; }
    }
  }
}