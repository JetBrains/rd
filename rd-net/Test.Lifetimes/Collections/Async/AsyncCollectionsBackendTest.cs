#if !NET35
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Async;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.Lifetimes.Collections.Async;

public class AsyncCollectionsBackendTest : LifetimesTestBase
{
  [Test]
  public void SimpleUpdateValueTest()
  {
    var pumpScheduler = new PumpScheduler(TestLifetime, false);
    var backend = new TestIntBackend(0, pumpScheduler);

    {
      backend.CopyImpl = i =>
      {
        Assert.Fail("must not be reached");
        return i;
      };
      backend.UpdateImpl = (element) =>
      {
        Assert.AreEqual(1, element);
        return element;
      };
      
      var task = backend.SetValueAsync(1);
    
      Assert.IsTrue(task.IsCompleted);
      using var cookie = backend.GetSnapshotCookie();
      Assert.AreEqual(1, cookie.State);
      Assert.IsFalse(pumpScheduler.PumpOnce());
    }

    {
      backend.UpdateImpl = (element) =>
      {
        Assert.AreEqual(2, element);
        return element;
      };
      
      var task = backend.SetValueAsync(2);
      
      Assert.IsTrue(task.IsCompleted);
      using var cookie = backend.GetSnapshotCookie();
      Assert.AreEqual(2, cookie.State);
      Assert.IsFalse(pumpScheduler.PumpOnce());
    }
    
    {
      using var cookie = backend.GetSnapshotCookie();
      Assert.AreEqual(2, cookie.State);
      var called = false;
      backend.CopyImpl = i =>
      {
        called = true;
        return i;
      };
      backend.UpdateImpl = (element) =>
      {
        Assert.AreEqual(3, element);
        return element;
      };
      
      var task = backend.SetValueAsync(3);
      
      Assert.IsTrue(task.IsCompleted);
      Assert.IsTrue(called);
      
      using var cookie2 = backend.GetSnapshotCookie();
      Assert.AreEqual(3, cookie2.State);
      Assert.IsFalse(pumpScheduler.PumpOnce());
    }

    {
      backend.CopyImpl = i =>
      {
        Assert.Fail("must not be reached");
        return i;
      };
      backend.UpdateImpl = (element) =>
      {
        Assert.AreEqual(4, element);
        return element;
      };
      
      var task = backend.SetValueAsync(4);
      Assert.IsTrue(task.IsCompleted);
    }
  }

  [Test]
  public void UpdateOrderTest()
  {
    var pumpScheduler = new PumpScheduler(TestLifetime, false);
    var backend = new TestIntBackend(0, pumpScheduler);
    
    pumpScheduler.Execute(() =>
    {
      var nestedTask = backend.SetValueAsync(1);
      Assert.IsTrue(nestedTask.IsCompleted);
      using var cookie = backend.GetSnapshotCookie();
      Assert.AreEqual(1, cookie.State);
    });

    {
      var task = backend.SetValueAsync(2);
      using (var cookie = backend.GetSnapshotCookie())
      {
        Assert.AreEqual(2, cookie.State);
        Assert.IsTrue(task.IsCompleted); 
      }
    
      pumpScheduler.Execute(() =>
      {
        var nestedTask = backend.SetValueAsync(3);
        Assert.IsTrue(nestedTask.IsCompleted);
        using var cookie = backend.GetSnapshotCookie();
        Assert.AreEqual(3, cookie.State);
      });
    }

    while (pumpScheduler.PumpOnce())
    {
    }
    
    TestLifetime.UsingNested(lifetime =>
    {
      int? lastNotifiedValue = null;
      pumpScheduler.Execute(() =>
      {
        var adviseTask = backend.AdviseAsync(lifetime, i => lastNotifiedValue = i);
        Assert.IsTrue(adviseTask.IsCompleted);
        Assert.AreEqual(3, lastNotifiedValue);
      });
      
      var updateTask = backend.SetValueAsync(2);
      using (var cookie = backend.GetSnapshotCookie())
      {
        Assert.AreEqual(2, cookie.State);
        Assert.IsFalse(updateTask.IsCompleted);
        Assert.AreEqual(3, lastNotifiedValue); 
      }
      
      pumpScheduler.Execute(() =>
      {
        var nestedTask = backend.SetValueAsync(4);
        Assert.IsFalse(nestedTask.IsCompleted);
        using var cookie = backend.GetSnapshotCookie();
        Assert.AreEqual(4, cookie.State);
        Assert.AreEqual(3, lastNotifiedValue);
      });

      Assert.IsTrue(pumpScheduler.PumpOnce());
      Assert.AreEqual(2, lastNotifiedValue);
      
      Assert.IsTrue(pumpScheduler.PumpOnce());
      Assert.AreEqual(4, lastNotifiedValue);
    });
  }

  [Test]
  public void AdviseOrderTest()
  {
    var pumpScheduler = new PumpScheduler(TestLifetime, false);
    var backend = new TestIntBackend(0, pumpScheduler);
    var change1 = 0;
    pumpScheduler.Execute(() =>
    {
      var task = backend.AdviseAsync(TestLifetime, i => change1 = i);
      Assert.IsTrue(task.IsCompleted);
    });
    
    var updateTask = backend.SetValueAsync(1);
    Assert.IsFalse(updateTask.IsCompleted);
    Assert.AreEqual(0, change1);
    
    pumpScheduler.Execute(() =>
    {
      var change2 = 0;
      var task = backend.AdviseAsync(TestLifetime, i => change2 = i);
      
      Assert.IsFalse(task.IsCompleted);
      Assert.AreEqual(0, change2);

      Assert.IsTrue(pumpScheduler.PumpOnce());
      
      Assert.AreEqual(1, change1);
      Assert.AreEqual(0, change2);
      Assert.IsTrue(updateTask.IsCompleted);
      
      Assert.IsTrue(pumpScheduler.PumpOnce());
      
      Assert.AreEqual(1, change1);
      Assert.AreEqual(1, change2);
      Assert.IsTrue(task.IsCompleted);
      
      Assert.IsFalse(pumpScheduler.PumpOnce());
    });
  }

  [Test]
  public void AdviseTest()
  {
    var pumpScheduler = new PumpScheduler(TestLifetime, false);
    var backend = new TestIntBackend(0, pumpScheduler);

    var change = 123;
    TestLifetime.UsingNested(lifetime =>
    {
      pumpScheduler.Execute(() =>
      {
        var adviseTask = backend.AdviseAsync(lifetime, i => change = i);
        Assert.IsTrue(adviseTask.IsCompleted);

        Assert.AreEqual(123, change);
        var updateTask = backend.SetValueAsync(1);
        Assert.IsTrue(updateTask.IsCompleted);
        Assert.AreEqual(1, change);
      });

      {
        var task = backend.SetValueAsync(2);
        Assert.IsFalse(task.IsCompleted);
        
        Assert.AreEqual(1, change);
        Assert.IsTrue(pumpScheduler.PumpOnce());
        
        Assert.AreEqual(2, change);
      }
    });
    
    pumpScheduler.Execute(() =>
    {
      var task = backend.SetValueAsync(1);
      Assert.IsTrue(task.IsCompleted);
      Assert.AreEqual(2, change);
    });
  }

  [Test]
  public void AdviseCancellationTest()
  {
    var pumpScheduler = new PumpScheduler(TestLifetime, false);
    var backend = new TestIntBackend(0, pumpScheduler);    
    
    pumpScheduler.Execute(() =>
    {
      var task = backend.SetValueAsync(1);
      Assert.IsTrue(task.IsCompleted);
    });

    var definition = TestLifetime.CreateNested();
    var change = 123;
    var adviseTask = backend.AdviseAsync(definition.Lifetime, i => change = i);
    
    Assert.IsFalse(adviseTask.IsCompleted);
    definition.Terminate();
    
    Assert.IsTrue(adviseTask.IsCanceled);
    Assert.IsTrue(pumpScheduler.PumpOnce());
    Assert.IsFalse(pumpScheduler.PumpOnce());
    Assert.AreEqual(123, change);

    adviseTask = backend.AdviseAsync(Lifetime.Terminated, i => { });
    Assert.IsTrue(adviseTask.IsCanceled);
  }

  [Test]
  public void AdviseHasNoValueTest()
  {
    var pumpScheduler = new PumpScheduler(TestLifetime, false);
    var backend = new TestIntBackend(0, pumpScheduler);    
    
    var adviseTask = backend.AdviseAsync(TestLifetime, i => {});
    Assert.IsTrue(adviseTask.IsCompleted);
  }

  [Test]
  public void OneWriterManyViewersTest()
  {
    var pumpScheduler = new PumpScheduler(TestLifetime, false);
    var backend = new TestListBackend(pumpScheduler);

    backend.AdviseAsync(TestLifetime, (kind, index, value) =>
    {
      Assert.AreEqual(AddUpdateRemove.Add, kind);
      Assert.AreEqual(index, value);
    });

    var definition = TestLifetime.CreateNested();
    var viewTasks = Enumerable.Range(0, 1).Select(_ => Task.Run(async () =>
    {
      while (true)
      {
        var nested = TestLifetime.CreateNested();
        var ints = new List<int>();
        var lastValue = -1;

        await backend.AdviseAsync(nested.Lifetime, (kind, index, value) =>
        {
          Assert.AreEqual(AddUpdateRemove.Add, kind);
          Assert.IsTrue(pumpScheduler.IsActive);
          Assert.AreEqual(index, value);
          
          if (lastValue != -1)
            Assert.AreEqual(lastValue, value - 1);

          lastValue = value;
          ints.Add(value);
          for (int i = 0; i <= value; i++) 
            Assert.IsTrue(ints[i] == i);
        });
        
        nested.Terminate();
        if (definition.Lifetime.IsNotAlive)
          return ints;
      }
    })).ToArray();

    const int n = 100;
    var expected = Enumerable.Range(0, n).ToList();
    var appendTask = Task.Run(async () =>
    {
      var value = 0;
      foreach (var i in expected)
      {
        Assert.AreEqual(i, value++);
        await backend.AppendAsync(i);
      }

    });
    appendTask.ContinueWith(_ => definition.Terminate());
    
    var task = Task.WhenAll(viewTasks);
    while (!task.IsCompleted) 
      pumpScheduler.PumpOnce();
    
    foreach (var ints in viewTasks.Select(x => x.Result)) 
      Assert.IsTrue(ints.SequenceEqual(expected));

    while (!appendTask.IsCompleted)
      pumpScheduler.PumpOnce();

    appendTask.Wait();

    while (pumpScheduler.PumpOnce())
    {
    }
    
    Assert.AreEqual(TaskStatus.RanToCompletion, task.Status);
  }

  [Test]
  public void ManyItemsTest()
  {
    var pumpScheduler = new PumpScheduler(TestLifetime, false);
    var backend = new TestListBackend(pumpScheduler);

    const int n = 10_000_000;
    
    var stopwatch = Stopwatch.StartNew();

    {
      var ints = new List<int>();
      for (var i = 0; i < n; i++)
        ints.Add(i);
    
      Console.WriteLine(stopwatch.ElapsedMilliseconds + "ms"); 
    }
    
    stopwatch.Restart();

    {
      pumpScheduler.Execute(() =>
      {
        for (var i = 0; i < n; i++)
        {
          var task = backend.AppendAsync(i);
          // Assert.IsTrue(task.IsCompleted);
        }
      });
      Console.WriteLine(stopwatch.ElapsedMilliseconds + "ms");
    }

    {
      stopwatch.Restart();
      var list = new ViewableList<int>();
      for (var i = 0; i < n; i++)
        list.Add(i);
      Console.WriteLine(stopwatch.ElapsedMilliseconds + "ms");
    }
  }
}

internal class TestIntBackend : AsyncCollectionsBackend<int, int, int>
{
  public Func<int, int> UpdateImpl = element => element;
  public Func<int, int> CopyImpl = (state) => state;

  public TestIntBackend(int defaultState, [NotNull] IScheduler scheduler) : base(defaultState, scheduler)
  {
  }

  public Task AdviseAsync(Lifetime lifetime, Action<int> action)
  {
    return AdviseAsync(lifetime, (_, _, value) => action(value));
  }

  public Task SetValueAsync(int value) => UpdateValueAsync(AddUpdateRemove.Update, 0, value);

  protected override int DoUpdate(int state, AddUpdateRemove kind, int _, int element)
  {
    myState =  UpdateImpl(element);
    return 0;
  }

  protected override int Copy(int state) => CopyImpl(state);

  protected override void DoFireState(int state, Action<AddUpdateRemove, int, int> listener) => Execute(AddUpdateRemove.Update, 0, state, listener);
}

internal sealed class TestListBackend : AsyncCollectionsBackend<List<int>, int, int> 
{
  public TestListBackend([NotNull] IScheduler scheduler) : base(new List<int>(), scheduler)
  {
  }

  public Task AppendAsync(int value) => UpdateValueAsync(AddUpdateRemove.Add, -1, value);


  protected override int DoUpdate(List<int> state, AddUpdateRemove kind, int key, int element)
  {
    if (kind == AddUpdateRemove.Add)
    {
      state.Add(element);
      var index = state.Count - 1;
      if (index != element)
        Assert.Fail($"{string.Join(", ", state)}");
      
      return index;
    }

    throw new NotSupportedException();
  }

  protected override List<int> Copy(List<int> state) => new(state);

  protected override void DoFireState(List<int> state, Action<AddUpdateRemove, int, int> listener)
  {
    for (var index = 0; index < state.Count; index++)
    {
      var item = state[index];
      Execute(AddUpdateRemove.Add, index, item, listener);
    }
  }
}

// todo concurrent GetReadonlyState test (huge structs + ref types)
// todo stress test

public class PumpScheduler : IScheduler
{
  private readonly Lifetime myLifetime;
  private readonly ConcurrentQueue<Action> myQueue = new();
  private readonly ThreadLocal<int> myCount = new();

  public bool IsActive => myCount.Value > 0;
  public bool OutOfOrderExecution { get; }

  public PumpScheduler(Lifetime lifetime, bool outOfOrderExecution)
  {
    myLifetime = lifetime;
    OutOfOrderExecution = outOfOrderExecution;
    lifetime.OnTermination(() => Assertion.Assert(myQueue.Count == 0));
  }

  public void Queue(Action action)
  {
    using var cookie = myLifetime.UsingExecuteIfAlive();
    if (!cookie.Succeed)
      throw new InvalidOperationException($"Lifetime: {myLifetime} is not alive");

    myQueue.Enqueue(action);
  }

  public void Execute(Action action)
  {
    myCount.Value++;
    try
    {
      action();
    }
    finally
    {
      myCount.Value--;
    }
  }

  public void Pump(Lifetime lifetime)
  {
    SpinWaitEx.SpinUntil(lifetime, () =>
    {
      PumpOnce();
      return true;
    });
  }

  public bool PumpOnce()
  {
    if (myQueue.TryDequeue(out var action))
    {
      Execute(action);
      return true;
    }

    return false;
  }
}

#endif