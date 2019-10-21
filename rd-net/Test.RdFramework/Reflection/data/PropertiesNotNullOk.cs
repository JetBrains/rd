using JetBrains.Annotations;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdScalar] // not required
  public class PropertiesNotNullOk
  {
    public PropertiesNotNullOk([NotNull] string first, string second, MyEnum @enum)
    {
      First = first;
      Second = second;
      Enum = @enum;
    }

    [NotNull] public string First { get; private set; }
    public string Second { get; private set; } // NotNull by default
    public MyEnum Enum { get; private set; }
  }
}