using System;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class ProxyGeneratorCornerCasesTests : RdReflectionTestBase
  {
    [RdRpc] public interface IInvalid1 { void M(ref int refx); }
    [RdRpc] public interface IInvalid2 { void M(out int refx); }
    [RdRpc] public interface IInvalid3 { void M<T>(out int refx); }
    [RdRpc] public interface IInvalid4<T> { void M(out int refx); }
    [RdRpc] public interface IInvalid5 { ref int M(); }
    //[RdRpc] public interface IInvalid6 { int M(in int x); }
    [RdRpc] public interface IInvalid7 { event Action<string> Event; }
    [RdRpc] public interface IInvalid8 { string X { set; } }
    [RdRpc] public interface IInvalid9 { string X { get; set; } }

    private ProxyGenerator myProxyGenerator;

    [SetUp]
    public void TestSetup(){
      myProxyGenerator = new ProxyGenerator();
    }

    [Test] public void TestInvalid1() { Assert.Throws<ArgumentException>(() => { myProxyGenerator.CreateType<IInvalid1>(); }); }
    [Test] public void TestInvalid2() { Assert.Throws<ArgumentException>(() => { myProxyGenerator.CreateType<IInvalid2>(); }); }
    [Test] public void TestInvalid3() { Assert.Throws<ArgumentException>(() => { myProxyGenerator.CreateType<IInvalid3>(); }); }
    [Test] public void TestInvalid4() { Assert.Throws<ArgumentException>(() => { myProxyGenerator.CreateType<IInvalid4<int>>(); }); }
    [Test] public void TestInvalid5() { Assert.Throws<ArgumentException>(() => { myProxyGenerator.CreateType<IInvalid5>(); }); }
    //[Test] public void TestInvalid6() { Assert.Throws<ArgumentException>(() => { myProxyGenerator.CreateType<IInvalid6>(); }); }
    [Test] public void TestInvalid7() { Assert.Throws<NotSupportedException>(() => { myProxyGenerator.CreateType<IInvalid7>(); }); }
    [Test] public void TestInvalid8() { Assert.Throws<Exception>(() => { myProxyGenerator.CreateType<IInvalid8>(); }); }
    [Test] public void TestInvalid9() { Assert.Throws<Exception>(() => { myProxyGenerator.CreateType<IInvalid9>(); }); }
  }
}