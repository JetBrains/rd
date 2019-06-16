using JetBrains.Core;
using JetBrains.Rd.Reflection;
using JetBrains.Rd.Tasks;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public sealed class ModelCalls : RdReflectionBindableBase
  {
    private IRdCall<string, Unit> Rpc1 { get; }
    private IRdCall<MyEnum, MyEnum> Rpc2 { get; }
    private IRdCall<Animal, Animal> Rpc3 { get; }
  }
}