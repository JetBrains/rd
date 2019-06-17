using JetBrains.Annotations;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public class PropertiesNotNullOk
  {
    [NotNull] public string First { get; private set; }
    public string Second { get; private set; } // NotNull by default
    public MyEnum Enum { get; private set; }
  }
}