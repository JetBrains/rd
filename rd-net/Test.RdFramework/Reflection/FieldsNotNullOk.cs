using JetBrains.Annotations;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public sealed class FieldsNotNullOk
  {
    [NotNull] public string FieldOne;
    public string FieldTwo; // NotNull by default
  }
}