using System;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class ProxyGeneratorCornerCasesTests : ProxyGeneratorTestBase
  {
    [RdRpc]
    public interface ICornerCases
    {
    }

    [RdRpc] public interface IInvalid1 { void M(ref int refx); }
    [RdRpc] public interface IInvalid2 { void M(out int refx); }
    [RdRpc] public interface IInvalid3 { void M<T>(out int refx); }
    [RdRpc] public interface IInvalid4<T> { void M(out int refx); }
    [RdRpc] public interface IInvalid5 { ref int M(); }
    [RdRpc] public interface IInvalid6 { int M(in int x); }
    [RdRpc] public interface IInvalid7 { event Action<string> Event; }
    [RdRpc] public interface IInvalid8 { string X { set; } }
    [RdRpc] public interface IInvalid9 { string X { get; set; } }
  }
}