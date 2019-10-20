using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public sealed class RootModel : RdExtReflectionBindableBase
  {
    public NestedModel Nested { get; }

    public IViewableProperty<EmptyOK> EmptyOK { get; }
    public IViewableProperty<FieldsNotNullOk> FieldsNotNullOk { get; }
    public IViewableProperty<FieldsNullableOk> FieldsNullableOk { get; }
    public IViewableProperty<PropertiesNotNullOk> PropertiesNotNullOk { get; }
    public IViewableProperty<PropertiesNullOk> PropertiesNullOk { get; }
  }
}