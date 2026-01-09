using JetBrains.Core;
using JetBrains.Rd.Reflection;
using JetBrains.Rd.Tasks;

namespace Test.RdFramework.Reflection
{
  [RdExt]
  public sealed class ModelCalls : RdExtReflectionBindableBase
  {
    public IRdCall<string, Unit> Rpc1 { get; }
    public IRdCall<MyEnum, MyEnum> Rpc2 { get; }
  }

  public enum MyEnum
  {
    First,
    Second
  }
}