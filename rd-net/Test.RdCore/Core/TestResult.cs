using System;
using JetBrains.Annotations;
using JetBrains.Core;
using NUnit.Framework;

namespace Test.RdCore.Core
{
  public class TestResult : RdCoreTestBase
  {
    [Test]
    public void TestSuccess()
    {
      var x = Result.Success(1);

      Assert.True(x.Succeed);
      Assert.False(x.Canceled);
      Assert.False(x.FailedNotCanceled);

      Assert.Null(x.Exception);
      Assert.Null(x.FailMessage);
    }

    [Test]
    public void TestFail()
    {
      var x = Result.Fail();

      Assert.False(x.Succeed);
      Assert.False(x.Canceled);
      Assert.True(x.FailedNotCanceled);

      Assert.NotNull(x.Exception);
      Assert.AreEqual(Result.EmptyFailMessage, x.FailMessage);
    }
    
    
    [Test]
    public void TestCanceled()
    {
      var x = Result.Canceled();

      Assert.False(x.Succeed);
      Assert.True(x.Canceled);
      Assert.False(x.FailedNotCanceled);

      Assert.NotNull(x.Exception);
    }


    public class TestResultInnerClass
    {
      public int Number { get; set; }

      [AssertionMethod]
      public static void Check([NotNull] TestResultInnerClass foo)
      {
        Assert.NotNull(foo);
      } 
    }

    [Test]
    public void NewTest()
    {
      void A() { B(); }

      void B() { C(); }

      void C() { Nothing.Unreachable(); }
      
      Result<TestResultInnerClass> CalcFoo(bool success)
      {
        if (success)
          return Result.Success(new TestResultInnerClass {Number = 1});
        return Result.Fail("Fail");
      }

      var result1 = CalcFoo(true);
      Assert.AreEqual(true, result1.Succeed);
      Assert.AreEqual(1, result1.Value.Number);
      Assert.AreEqual(1, result1.Unwrap().Number);
      TestResultInnerClass.Check(result1.Unwrap());      

      var result2 = CalcFoo(false);
      Assert.AreEqual(false, result2.Succeed);
      Assert.AreEqual("Fail", result2.FailMessage);
      Assert.Throws<ResultException>(() => result2.Unwrap());

      Assert.Throws<InvalidOperationException>(() => Result.Wrap(A).Unwrap());
    }

    [Test]
    public void TestFailValue()
    {
      // ReSharper disable once ParameterOnlyUsedForPreconditionCheck.Local
      void AssertFail(Result<Nothing> res)
      {
        Assert.True(res.FailedNotCanceled);
      }
      
      // ReSharper disable once ParameterOnlyUsedForPreconditionCheck.Local
      void AssertFailForInt(Result<int> res)
      {
        Assert.True(res.FailedNotCanceled);
      }
      
      var debugInfo = Result.Success(42);
      var fail = Result.Fail("Fail", debugInfo);
      var failedDebugInfo = fail.FailValue;
      Assert.AreEqual(debugInfo.Value, failedDebugInfo.Value);
      AssertFail(fail);
      AssertFailForInt((Result<Nothing>)fail);
    }
    
  }
}