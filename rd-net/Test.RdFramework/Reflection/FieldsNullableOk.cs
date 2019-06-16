using JetBrains.Annotations;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public sealed class FieldsNullableOk
  {
    [CanBeNull] public string FieldOne;
    public MyEnum? MaybeEnum;
    public int? MaybeInt;
  }
}