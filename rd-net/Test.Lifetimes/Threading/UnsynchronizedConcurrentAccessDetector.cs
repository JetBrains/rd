using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Lifetimes;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.Lifetimes.Threading;

[TestFixture]
public class UnsynchronizedConcurrentAccessDetectorTest : LifetimesTestBase
{
  [Test]
  public void SimpleReentrantTest()
  {
    var detector = new UnsynchronizedConcurrentAccessDetector();
    Task.Factory.StartNew(async () =>
    {
      DoTest();
      for (int i = 0; i < 10; i++) 
        await Task.Factory.StartNew(DoTest);
      
    }).Unwrap().Wait(TimeSpan.FromSeconds(10));

    void DoTest()
    {
      for (int i = 0; i < 10; i++)
      {
        using (detector.CreateCookie())
        {
          for (int j = 0; j < i; j++)
          {
            using (detector.CreateCookie()) { }
          }
        }
      }
    }
  }

  [Test]
  public void TwoThreadsAccessTest()
  {
    var detector = new UnsynchronizedConcurrentAccessDetector();
    using (detector.CreateCookie())
    {
      var task = Task.Factory.StartNew(() =>
      {
        MyWorkerThreaMethod();
      });

      var success = SpinWaitEx.SpinUntil(Lifetime.Eternal, TimeSpan.FromSeconds(10), () => task.IsCompleted);
      Assert.IsTrue(success);
    }

    try
    {
      ThrowLoggedExceptions();
      Assert.Fail("Must not be reached");
    }
    catch (Exception e)
    {
      Assert.IsTrue(e.ToString().Contains(nameof(MyWorkerThreaMethod)));
      Assert.IsTrue(e.ToString().Contains(nameof(TwoThreadsAccessTest)));
    }
    
    void MyWorkerThreaMethod()
    {
      using (detector.CreateCookie())
      {
      }
    }
  }
}