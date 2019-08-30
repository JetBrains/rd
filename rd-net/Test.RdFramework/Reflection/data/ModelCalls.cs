using JetBrains.Core;
using JetBrains.Rd.Reflection;
using JetBrains.Rd.Tasks;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public sealed class ModelCalls : RdReflectionBindableBase
  {
    public IRdCall<string, Unit> Rpc1 { get; }
    public IRdCall<MyEnum, MyEnum> Rpc2 { get; }


    [RdExt]
    public class ModelInvalidCalls : RdReflectionBindableBase
    {
      // should not be possible!
      public IRdCall<Animal, Animal> Rpc3 { get; }
    }
  }
}