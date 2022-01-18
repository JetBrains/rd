using System;
using System.Collections;
using System.Collections.Generic;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Util;
using NUnit.Framework;

namespace Test.Lifetimes.Collections;

[TestFixture]
public class DictionaryExTest : LifetimesTestBase
{
  [Test]
  public void BlockingAddUniqueTest()
  {
    var dictionary = new Dictionary<string, string>();
    var locker = new object();

    const string key = "MyKey";
    const string value = "MyValue";

    Lifetime.Using(lifetime =>
    {
      dictionary.BlockingAddUnique(lifetime, locker, key, value);
      Assert.AreEqual(1, dictionary.Count);
      Assert.AreEqual(value, dictionary[key]);
    });
    Assert.AreEqual(0, dictionary.Count);
    
    lock (locker)
    {
      var task = StartTask(() =>
      {
        var definition = TestLifetime.CreateNested();
        StartTask(() =>
        {
          Thread.Sleep(TimeSpan.FromMilliseconds(100));
          Assert.AreEqual(0, dictionary.Count);
          definition.Terminate();
        });
        
        dictionary.BlockingAddUnique(definition.Lifetime, locker, key, value);
        Assert.IsTrue(definition.Lifetime.IsNotAlive);
        Assert.AreEqual(0, dictionary.Count);
      });
      
      Assert.IsTrue(task.Wait(TimeSpan.FromSeconds(10)));
    }
  }

  private static Task StartTask(Action action)
  {
    return Task.Factory.StartNew(action, CancellationToken.None, TaskCreationOptions.None, TaskScheduler.Default);
  }
}