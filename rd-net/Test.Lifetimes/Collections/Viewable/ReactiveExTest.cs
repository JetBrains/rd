using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Viewable;

[TestFixture]
public class ReactiveExTest : LifetimesTestBase
{
#if !NET35
  [Test]
  public void NextValueAsyncTest()
  {
    var lifetimeDef = TestLifetime.CreateNested();
    var signal = new Signal<int>();
    {
      var task = signal.NextValueAsync(lifetimeDef.Lifetime, i => i == 1);
      Assert.IsFalse(task.IsCompleted);
    
      signal.Fire(0);
      Assert.IsFalse(task.IsCompleted);
    
      signal.Fire(1);
      Assert.AreEqual(TaskStatus.RanToCompletion, task.Status);
      Assert.AreEqual(1, task.Result);
    }

    {
      var task = signal.NextValueAsync(lifetimeDef.Lifetime);
      lifetimeDef.Terminate();
      Assert.IsTrue(task.IsCanceled);
    }
  }
#endif

  [Test]
  public void AdviceOnceTest()
  {
    var signal = new Signal<int>();
    var count = 0;

    signal.AdviseOnce(TestLifetime, _ =>
    {
      Assert.AreEqual(0, count++);
      signal.Fire(1);
    });
    
    signal.Fire(0);
  }
}