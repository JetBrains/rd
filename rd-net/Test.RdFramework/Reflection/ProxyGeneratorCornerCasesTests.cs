using System;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;
using NUnit.Framework;

namespace Test.RdFramework.Reflection
{
  [TestFixture]
  public class AsyncCornerCases : ProxyGeneratorAsyncCallsTest
  {
    protected override bool IsAsync => true;

    [Test]
    public async Task TestUnregisteredType()
    {
      await TestTemplate<UnknownSerializer, IUnknownSerializer>(s =>
      {
        var task = s.M(new Derived());
        Assert.Throws<AggregateException>(() => task.Wait(1000));
      });
    }

    public class Base  { }
    public class Derived : Base  { }

    [RdRpc]
    public interface IUnknownSerializer
    {
      Task M(Base val);
    }
    [RdExt]
    public class UnknownSerializer : RdExtReflectionBindableBase, IUnknownSerializer
    {
      public Task M(Base val) => Task.Factory.StartNew(() => { });
    }
  }

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
    public interface IInvalid10 {  } // no RdRpcAttribute
    [RdRpc] public interface IInvalid11 { void M(Action x); }


    private bool TestType<T>() where T : class
    {
      try
      {
        CFacade.ProxyGenerator.CreateType<T>();
        CFacade.ScalarSerializers.GetOrCreate(typeof(T));
      }
      catch (Exception e)
      {
        return true;
      }

      return false;
    }

    [Test] public void TestInvalid1() { Assert.IsTrue(TestType<IInvalid1>()); }
    [Test] public void TestInvalid2() { Assert.IsTrue(TestType<IInvalid2>()); }
    [Test] public void TestInvalid3() { Assert.IsTrue(TestType<IInvalid3>()); }
    [Test] public void TestInvalid4() { Assert.IsTrue(TestType<IInvalid4<int>>()); }
    [Test] public void TestInvalid5() { Assert.IsTrue(TestType<IInvalid5>()); }
    [Test] public void TestInvalid7() { Assert.IsTrue(TestType<IInvalid7>()); }
    [Test] public void TestInvalid8() { Assert.IsTrue(TestType<IInvalid8>()); }
    [Test] public void TestInvalid9() { Assert.IsTrue(TestType<IInvalid9>()); }
    [Test] public void TestInvalid10() { Assert.IsTrue(TestType<IInvalid10>()); }
    [Test] public void TestInvalid11() { Assert.IsTrue(TestType<IInvalid11>()); }


    [Test]
    public void TestIncorrectInitialization()
    {
      try
      {
        WithExtsProxy<WrongInitializedTypeTest, IWrongInitialializedTypeTest>((c, s) =>
        {
          c.ViewableProperty.Value = "test";
          Assert.AreEqual(c.ViewableProperty.Value, s.ViewableProperty.Value);
        });
      }
      catch (Exception e)
      {
        return;
      }
      Assert.Fail();
    }

    [Test]
    public void TestUnexpectedInterfaceType()
    {
      Assert.Throws<Exception>(() =>
      {
        WithExtsProxy<UnexpectedInterfaceType, IUnexpectedInterfaceType>((c, s) =>
        {
          c.ViewableProperty.Value = "test";
        });
      });
    }

    [RdRpc]
    public interface IUnexpectedInterfaceType
    {
      ViewableProperty<string> ViewableProperty { get; }
    }

    [RdExt]
    public class UnexpectedInterfaceType : RdExtReflectionBindableBase, IUnexpectedInterfaceType
    {
      public ViewableProperty<string> ViewableProperty { get; }
    }

    [RdRpc]
    public interface IWrongInitialializedTypeTest
    {
      IViewableProperty<string> ViewableProperty { get; }
    }

    [RdExt]
    public class WrongInitializedTypeTest : RdExtReflectionBindableBase, IWrongInitialializedTypeTest
    {
      public IViewableProperty<string> ViewableProperty { get; }

      public WrongInitializedTypeTest()
      {
        ViewableProperty = new ViewableProperty<string>();
      }
    }
  }
}