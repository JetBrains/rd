using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Lifetimes;
using JetBrains.Threading;
using JetBrains.Util.Internal;
using NUnit.Framework;

namespace Test.Lifetimes.Lifetimes
{
  
  
  public class LifetimeTest : LifetimesTestBase
  {
    
    // ReSharper disable once InconsistentNaming
    LifetimeDefinition def;
    // ReSharper disable once InconsistentNaming
    private Lifetime lt => def.Lifetime;

    [SetUp]
    public void BeforeTest()
    {
      def = new LifetimeDefinition();
      def.Id = TestContext.CurrentContext.Test.Name;
    }

    class FailureException : Exception
    {}
    private void Fail()
    {
      throw new FailureException();
    }
    private T Fail<T>()
    {
      throw new FailureException();
    }
    
    [Test]
    public void TestEmptyLifetime()
    {
      def.Terminate();
      def.Terminate();
    }

    [Test]
    public void TestActionsSequence()
    {
      var log = new List<int>();
      
      def.Lifetime.OnTermination(() => log.Add(1));
      def.Lifetime.OnTermination(() => log.Add(2));
      def.Lifetime.OnTermination(() => log.Add(3));

      def.Terminate();
      
      Assert.AreEqual(new []{3,2,1}, log.ToArray());
    }


    [Test]
    public void TestNestedLifetime()
    {
      var log = new List<int>();
      
      def.Lifetime.OnTermination(() => log.Add(1));
      new LifetimeDefinition(def.Lifetime).Lifetime.OnTermination(() => log.Add(2));
      def.Lifetime.OnTermination(() => log.Add(3));
      
      def.Terminate();
      
      Assert.AreEqual(new []{3,2,1}, log.ToArray()); 
    }

    [Test]
    public void TestUsing()
    {
      Lifetime lf;
      Lifetime.Using(l =>
      {
        lf = l;
        Assert.True(lf.IsAlive);
        Assert.False(lf.IsEternal);
      });
    }

#if !NET35
    [Test]
    public void TestTerminationWithAsyncTimeout()
    {
      var lin = new Linearization();
      lin.Enable();
      
      var log = new List<int>();

      var task = Task.Run(() =>
      {
        var first = lt.TryExecute(() =>
        {
          log.Add(0);
          Assert.AreEqual(LifetimeStatus.Alive, lt.Status);
          Assert.True(lt.IsAlive);
          
          lin.Point(0);
          log.Add(1);

          SpinWaitEx.SpinUntil(() => def.Status == LifetimeStatus.Canceling);
          Assert.False(lt.IsAlive);                    
        });
        
        //shouldn't execute
        var second = lt.TryExecute(() => { log.Add(2); });
        
        Assert.True(first.Succeed);
        Assert.False(second.Succeed);
      });
      
      def.Lifetime.OnTermination(() => {log.Add(-1);});
      
      lin.Point(1);
      def.Terminate();
      lin.Point(2);

      task.Wait();
      Assert.AreEqual(new []{0, 1, -1}, log.ToArray());

    } 
#endif
     
    [Test]
    public void TestEternal()
    {
      Assert.True(Lifetime.Eternal.IsEternal);
      
      //doesn't fail
      Lifetime.Eternal.OnTermination(() => { });
    }

    [Test]
    public void TestEquals()
    {
      Lifetime eternal = default;
      Assert.AreEqual(Lifetime.Eternal, eternal);
      Assert.AreEqual(Lifetime.Eternal, Lifetime.Eternal);
      Assert.AreEqual(eternal, eternal);
      
      Assert.True(Lifetime.Eternal == eternal);

      Assert.AreNotEqual(Lifetime.Eternal, Lifetime.Terminated);
      Assert.False(Lifetime.Eternal == Lifetime.Terminated);
      Assert.False(eternal == Lifetime.Terminated);
    }

    [Test]
    public void TestTerminated()
    {
      Assert.True(Lifetime.Terminated.Status == LifetimeStatus.Terminated);
    }


    [Test]
    public void TestRecursiveTermination()
    {
      Assert.Throws<InvalidOperationException>(() => lt.TryExecute(() => { def.Terminate(); }).Unwrap());
      Assert.AreEqual(LifetimeStatus.Alive, lt.Status);

      def.AllowTerminationUnderExecution = true;
      lt.TryExecute(() => { def.Terminate(); });
      Assert.AreEqual(LifetimeStatus.Terminated, lt.Status);
    }


    [Test]
    public void TestTryExecuteAction()
    {
      Assert.True(lt.TryExecute(() => { }).Succeed);
      Assert.True(lt.TryExecute(() => { }, true).Succeed);
      
      Assert.Throws<FailureException>(() => lt.TryExecute(Fail));
      Assert.True(lt.TryExecute(Fail, true).Exception is FailureException);
      Assert.True(lt.TryExecute(Fail, true).FailedNotCanceled);
      
      def.Terminate();
      Assert.True(lt.TryExecute(Fail).Canceled);
      Assert.True(lt.TryExecute(Fail).Exception is LifetimeCanceledException);
      Assert.True(lt.TryExecute(Fail, true).Canceled);
      Assert.True(lt.TryExecute(Fail, true).Exception is LifetimeCanceledException);
    }
    
    [Test]
    public void TestTryExecuteFunc()
    {
      Assert.True(lt.TryExecute(() => 1).Succeed);
      Assert.True(lt.TryExecute(() => 1, true).Succeed);
      
      Assert.Throws<FailureException>(() => lt.TryExecute(Fail<int>));
      Assert.True(lt.TryExecute(Fail<int>, true).Exception is FailureException);
      Assert.True(lt.TryExecute(Fail<int>, true).FailedNotCanceled);
      
      def.Terminate();
      Assert.True(lt.TryExecute(Fail<int>).Canceled);
      Assert.True(lt.TryExecute(Fail<int>).Exception is LifetimeCanceledException);
      Assert.True(lt.TryExecute(Fail<int>, true).Canceled);
      Assert.True(lt.TryExecute(Fail<int>, true).Exception is LifetimeCanceledException);
    }

    [Test]
    public void TestLongTryExecute()
    {
      const string expectedWarningText = "can't wait for `ExecuteIfAlive` completed on other thread";
      const string expectedExceptionText = "ExecuteIfAlive after termination of";
      bool warningReceived = false, exceptionReceived = false;

      Lifetime.Using(lifetime =>
      {
        void LoggerHandler(LeveledMessage message)
        {
          if (message.Level == LoggingLevel.WARN && message.FormattedMessage.Contains(expectedWarningText)) 
            warningReceived = true;
        }

        lifetime.Bracket(
          () => TestLogger.ExceptionLogger.Handlers += LoggerHandler,
          () => TestLogger.ExceptionLogger.Handlers -= LoggerHandler
          );

        var lifetimeDefinition = lifetime.CreateNested();
        var lifetimeTerminatedEvent = new ManualResetEvent(false);
        var backgroundThreadIsInTryExecuteEvent = new ManualResetEvent(false);
        var thread = new Thread(() => lifetimeDefinition.Lifetime.TryExecute(() =>
        {
          backgroundThreadIsInTryExecuteEvent.Set();
          lifetimeTerminatedEvent.WaitOne();
        }));
        thread.Start();
        backgroundThreadIsInTryExecuteEvent.WaitOne();
        lifetimeDefinition.Terminate();
        lifetimeTerminatedEvent.Set();
        thread.Join();
        try
        {
          TestLogger.ExceptionLogger.ThrowLoggedExceptions();
        }
        catch (Exception e)
        {
          if (!e.Message.Contains(expectedExceptionText))
            throw;

          exceptionReceived = true;
        }
      });

      Assert.IsTrue(warningReceived, "Warning `{0}` must have been logged", expectedWarningText);
      Assert.IsTrue(exceptionReceived, "Exception `{0}` must have been logged", expectedExceptionText);
    }

#if !NET35
    [Test]
    public void TestLongTryExecuteAsync()
    {
      const string expectedWarningText = "can't wait for `ExecuteIfAlive` completed on other thread";
      const string expectedExceptionText = "ExecuteIfAlive after termination of";
      bool warningReceived = false, exceptionReceived = false;

      Lifetime.Using(lifetime =>
      {
        void LoggerHandler(LeveledMessage message)
        {
          if (message.Level == LoggingLevel.WARN && message.FormattedMessage.Contains(expectedWarningText)) 
            warningReceived = true;
        }

        lifetime.Bracket(
          () => TestLogger.ExceptionLogger.Handlers += LoggerHandler,
          () => TestLogger.ExceptionLogger.Handlers -= LoggerHandler
        );

        var lifetimeDefinition = lifetime.CreateNested();
        var lifetimeTerminatedEvent = new ManualResetEvent(false);
        var backgroundThreadIsInTryExecuteEvent = new ManualResetEvent(false);
        var thread = new Thread(() => lifetimeDefinition.Lifetime.TryExecute(() =>
        {
          backgroundThreadIsInTryExecuteEvent.Set();
          lifetimeTerminatedEvent.WaitOne();
        }));
        thread.Start();
        backgroundThreadIsInTryExecuteEvent.WaitOne();
        
        var terminationTask = lifetimeDefinition.TerminateAsync();
        SpinWait.SpinUntil(() => terminationTask.IsCompleted);
        
        lifetimeTerminatedEvent.Set();
        thread.Join();
        try
        {
          TestLogger.ExceptionLogger.ThrowLoggedExceptions();
        }
        catch (Exception e)
        {
          if (!e.Message.Contains(expectedExceptionText))
            throw;

          exceptionReceived = true;
        }
      });

      Assert.IsTrue(warningReceived, "Warning `{0}` must have been logged", expectedWarningText);
      Assert.IsTrue(exceptionReceived, "Exception `{0}` must have been logged", expectedExceptionText);
    }
#endif

    
    [Test]
    public void TestBracketGood()
    {

      var log = 0;
      void Inner(Action action)
      {
        log = 0;
        def = new LifetimeDefinition();
        action();
        Assert.AreEqual(1, log);      
        def.Terminate();      
        Assert.AreEqual(11, log);
      }
      
      //Action, Action
      Inner( () => lt.Bracket(() => log += 1, () => log += 10));
      
      //Func<T>, Action
      Inner(() => Assert.AreEqual(1, lt.Bracket(() =>
        {
          log += 1;
          return 1;
        },

        () => { log += 10; }
      )));
      
      //Func<T>, Action<T>
      Inner(() => Assert.AreEqual(10, lt.Bracket(() =>
        {
          log += 1;
          return 10;
        },

        x => { log += x; }
      )));
    }
    
    
    [Test]
    public void TestBracketCanceled()
    {
      var log = 0;
      def.Terminate();
      void Inner(Action action)
      {
        log = 0;
        Assert.Throws<LifetimeCanceledException>(() => action());
        Assert.AreEqual(0, log);      
        def.Terminate(); //once more      
        Assert.AreEqual(0, log);
      }
      
      //Action, Action
      Inner( () => lt.Bracket(() => log += 1, () => log += 10));
      
      //Func<T>, Action
      Inner(() => Assert.AreEqual(1, lt.Bracket(() =>
        {
          log += 1;
          return 1;
        },

        () => { log += 10; }
      )));
      
      //Func<T>, Action<T>
      Inner(() => Assert.AreEqual(10, lt.Bracket(() =>
        {
          log += 1;
          return 10;
        },

        x => { log += x; }
      )));
    }
    
    
    [Test]
    public void TestBracketBadOpening()
    {
      var log = 0;
      void Inner(Action action)
      {
        def = new LifetimeDefinition();
        log = 0;
        Assert.Throws<FailureException>(() => action());
        Assert.AreEqual(1, log);      
        def.Terminate(); //once more      
        Assert.AreEqual(1, log);
      }
      
      //Action, Action
      Inner(() => lt.Bracket(() =>
      {
        log += 1;
        Fail();
      }, () => log += 10));
      
      //Func<T>, Action
      Inner(() => Assert.AreEqual(1, lt.Bracket(() =>
        {
          log += 1;
          return Fail<int>();
        },

        () => { log += 10; }
      )));
      
      //Func<T>, Action<T>
      Inner(() => Assert.AreEqual(10, lt.Bracket(() =>
        {
          log += 1;
          return Fail<int>();
        },

        x => { log += x; }
      )));
    }
    
    
    [Test]
    public void TestBracketTerminationInOpening()
    {
      var log = 0;
      void Inner(Action action)
      {
        def = new LifetimeDefinition();
        def.AllowTerminationUnderExecution = true;
        log = 0;
        action();
        Assert.AreEqual(11, log);      
        def.Terminate(); //once more      
        Assert.AreEqual(11, log);
      }
      
      //Action, Action
      Inner(() => lt.Bracket(() =>
      {
        log += 1;
        def.Terminate();
      }, () => log += 10));
      
      //Func<T>, Action
      Inner(() => Assert.AreEqual(1, lt.Bracket(() =>
        {
          log += 1;
          def.Terminate();
          return 1;
        },

        () => { log += 10; }
      )));
      
      //Func<T>, Action<T>
      Inner(() => Assert.AreEqual(10, lt.Bracket(() =>
        {
          log += 1;
          def.Terminate();
          return 10;
        },

        x => { log += x; }
      )));
    }
    
    
    
    [Test]
    public void TestTryBracketGood()
    {
      
      var log = 0;
      void Inner(Action action)
      {
        log = 0;
        def = new LifetimeDefinition();
        action();
        Assert.AreEqual(1, log);      
        def.Terminate();      
        Assert.AreEqual(11, log);
      }

      
      //Action + Action
      Inner(() => Assert.True(Result.Unit == lt.TryBracket(() =>
        {
          log += 1;
        },

        () => { log += 10; }
      )));
      Inner(() => Assert.True(Result.Unit == lt.TryBracket(() =>
                                {
                                  log += 1;
                                },

                                () => { log += 10; }
                                , true)));
            
      
      //Func<T> + Action
      Inner(() => Assert.True(Result.Success(1) == lt.TryBracket(() =>
                                {
                                  log += 1;
                                  return 1;
                                },

                                () => { log += 10; }
                              )));
      Inner(() => Assert.True(Result.Success(1) == lt.TryBracket(() =>
                                {
                                  log += 1;
                                  return 1;
                                },

                                () => { log += 10; }
                                , true)));
      
      
      //Func<T> + Action<T>
      Inner(() => Assert.True(Result.Success(10) == lt.TryBracket(() =>
                                {
                                  log += 1;
                                  return 10;
                                },

                                x => { log += x; }
                              )));
      Inner(() => Assert.True(Result.Success(10) == lt.TryBracket(() =>
                                {
                                  log += 1;
                                  return 10;
                                },

                                x => { log += x; }
                                , true)));
      
    }
    
    
    [Test]
    public void TestTryBracketCanceled()
    {
      
      var log = 0;
      void Inner(Action action)
      {
        log = 0;
        def.Terminate();
        action();
        Assert.AreEqual(0, log);      
        def.Terminate();      
        Assert.AreEqual(0, log);
      }

      //Action + Action
      Inner(() =>
      {
        var res = lt.TryBracket(() => { log += 1; },

          () => { log += 10; }
        );
        Assert.True(res.Canceled);
        Assert.True(res.Exception is LifetimeCanceledException lce && lce.Lifetime == lt);
      });
      
      Inner(() =>
      {
        var res = lt.TryBracket(() => { log += 1; },

          () => { log += 10; },
          true
        );
        Assert.True(res.Canceled);
        Assert.True(res.Exception is LifetimeCanceledException lce && lce.Lifetime == lt);
      });
      
      
      //Func<T> + Action
      Inner(() =>
      {
        var res = lt.TryBracket(() => { log += 1;
            return 1;
          },

          () => { log += 10; }
        );
        Assert.True(res.Canceled);
        Assert.True(res.Exception is LifetimeCanceledException lce && lce.Lifetime == lt);
      });
      
      Inner(() =>
      {
        var res = lt.TryBracket(() => { log += 1; return 1;},

          () => { log += 10; },
          true
        );
        Assert.True(res.Canceled);
        Assert.True(res.Exception is LifetimeCanceledException lce && lce.Lifetime == lt);
      });
      
      //Func<T> + Action<T>
      Inner(() =>
      {
        var res = lt.TryBracket(() => { log += 1;
            return 1;
          },

          x => { log += x; }
        );
        Assert.True(res.Canceled);
        Assert.True(res.Exception is LifetimeCanceledException lce && lce.Lifetime == lt);
      });
      
      Inner(() =>
      {
        var res = lt.TryBracket(() => { log += 1; return 1;},

          x => { log += x; },
          true
        );
        Assert.True(res.Canceled);
        Assert.True(res.Exception is LifetimeCanceledException lce && lce.Lifetime == lt);
      });

    }
    
    
    [Test]
    public void TestTryBracketBadOpeningWrap()
    {
      var log = 0;
      void Inner(Action action)
      {
        log = 0;
        def = new LifetimeDefinition();
        action();
        Assert.AreEqual(1, log);      
        def.Terminate();      
        Assert.AreEqual(1, log);
      }

      //Action + Action
      Inner(() =>
      {
        Assert.Throws<FailureException>( () => lt.TryBracket(() => { log += 1; Fail(); },
          () => { log += 10;  }
        ));
      });
      
      Inner(() =>
      {
        var res = lt.TryBracket(() => { log += 1; Fail(); },
          () => { log += 10; },
          true
        );
        Assert.True(res.FailedNotCanceled);
        Assert.True(res.Exception is FailureException);
      });
      
      
      //Func<T> + Action
      Inner(() =>
      {
        Assert.Throws<FailureException>( () => lt.TryBracket(() => { log += 1; return Fail<int>(); },
          () => { log += 10;  }
        ));
      });
      
      Inner(() =>
      {
        var res = lt.TryBracket(() => { log += 1; return Fail<int>(); },
          () => { log += 10; },
          true
        );
        Assert.True(res.FailedNotCanceled);
        Assert.True(res.Exception is FailureException);
      });

      
      //Func<T> + Action<T>
      Inner(() =>
      {
        Assert.Throws<FailureException>(() => lt.TryBracket(() => { log += 1;
            return Fail<int>();
          },

          x => { log += x; }
        ));
      });
      
      Inner(() =>
      {
        var res = lt.TryBracket(() => { log += 1; return Fail<int>(); },

          x => { log += x; },
          true
        );
        Assert.True(res.FailedNotCanceled);
        Assert.True(res.Exception is FailureException);
      });
    }
    
    
    
    [Test]
    public void TestTryBracketTerminationInOpening()
    {
      
      var log = 0;
      
      
      void InnerSuccess<T>(Func<bool, Result<T>> action)
      {
        log = 0;
        def = new LifetimeDefinition {AllowTerminationUnderExecution = true};
        
        Assert.True(action(false).Succeed);
        Assert.AreEqual(11, log);
        
        log = 0;
        def = new LifetimeDefinition{AllowTerminationUnderExecution = true};
        Assert.True(action(true).Succeed);
        Assert.AreEqual(11, log);
        
        Assert.AreEqual(11, log);
      }
      
      void InnerFail<T>(Func<bool, Result<T>> action)
      {
        log = 0;
        def = new LifetimeDefinition{AllowTerminationUnderExecution = true};
        
        Assert.Throws<FailureException>(() => action(false));
        Assert.AreEqual(11, log);
        
        log = 0;
        def = new LifetimeDefinition{AllowTerminationUnderExecution = true};
        Assert.True(action(true).Exception is FailureException);
        Assert.AreEqual(11, log);
      }
      
      //Action + Action 
      InnerSuccess(wrap => lt.TryBracket(() =>
        {
          log += 1;
          def.Terminate();
        },
        () => { log += 10; },
        wrap
      ));
      
      InnerFail(wrap => lt.TryBracket(() =>
        {
          log += 1;
          def.Terminate();
        },
        () => { log += 10; Fail(); },
        wrap
      ));
      
      
      //Func<T> + Action
      InnerSuccess(wrap => lt.TryBracket(() =>
        {
          log += 1;
          def.Terminate();
          return 1;
        },
        () => { log += 10; },
        wrap
      ));
      

      // Func<T> + Action<T>
      InnerFail(wrap => lt.TryBracket(() =>
        {
          log += 1;
          def.Terminate();
          return 10;
        },
        x => { log += x; Fail();},
        wrap
      )); 
    }


    [Test]
    public void TestAddTerminationActionToTerminatedLifetime()
    {
      int executed = 0;
      def.Terminate();

      //actions
      Assert.False(lt.TryOnTermination(() => { executed++; }));
      Assert.AreEqual(0, executed);  //no change

      Assert.Throws<InvalidOperationException>(() => lt.OnTermination(() => { executed++; }));
      Assert.AreEqual(1, executed);
      
      //dispose
      Assert.False(lt.TryOnTermination(() => { executed++; }));
      Assert.AreEqual(1, executed); //no change

      Assert.Throws<InvalidOperationException>(() => lt.OnTermination(() => { executed++; }));
      Assert.AreEqual(2, executed);

    }

#if !NET35
    [Test]
    public void TestTaskAttachment()
    {
      int executed = 0;
      lt.ExecuteAsync(async () =>
      {
        await Task.Yield();
        executed += 1;
      });
      lt.OnTermination(() => executed *= 2);

      def.Terminate(); //will wait for task
      Assert.AreEqual(2, executed);
    }


    [Test]
    public void TestTaskWithTerminatedLifetime() {
      var task = Lifetime.Terminated.TryExecuteAsync(async () =>
      {
        await Task.Yield();
        return 0;
      });
      
      Assert.True(task.IsCanceled);
    }
    
    [Test]
    public void TestFailedTask() {
      var task = lt.ExecuteAsync(async () =>
      {
        await Task.Yield();
        throw new Exception();
      });

      Assert.Throws<AggregateException>(task.Wait);
    }
#endif

    [Test]
    public void TestAllInnerLifetimesTerminatedExceptLast()
    {
      var o = lt.CreateNested();
      for (int i = 0; i < 100; i++)
      {
        var n = lt.CreateNested();
        o.Terminate();
        o = n;
      }

      var resCount = def.GetDynamicField("myResCount");
      Assert.AreEqual(2, resCount); //one is dead
      
      var resourcesCapacity = def.GetDynamicField("myResources").GetDynamicProperty("Length");
      Assert.AreEqual(2, resourcesCapacity); //one is dead
    }

    
//    [Test]
//    public void TestScopeLifetime()
//    {
//      Lifetime lf;
//      using (var scoped = new ScopedLifetime())
//      {
//        lf = scoped;
//        lf.AssertIsAlive();
//      }
//       Assert.False(lf.IsAlive);
//    }
//    
//    [Test]
//    public void TestScopedLifetimeWithAliveParent()
//    {
//      Lifetime lf;
//      using (var scoped = new ScopedLifetime(lt))
//      {
//        lf = scoped;
//        lf.AssertIsAlive();        
//      }
//      Assert.True(lt.IsAlive);
//      Assert.False(lf.IsAlive);
//    }
//    
//    [Test]
//    public void TestScopedLifetimeWithTerminatingParent()
//    {
//      Lifetime lf;
//      using (var scoped = new ScopedLifetime(lt))
//      {
//        lf = scoped;
//        lf.AssertIsAlive();
//        def.Terminate();
//        Assert.False(lf.IsAlive);
//      }
//      Assert.False(lt.IsAlive);
//      Assert.False(lf.IsAlive);
//    }
//    
//    [Test]
//    public void TestScopedLifetimeWithTerminatedParent()
//    {
//      Lifetime lf;
//      using (var scoped = new ScopedLifetime(lt))
//      {
//        lf = scoped.Instance;
//        lf.AssertIsAlive();
//        def.Terminate();
//        Assert.False(lf.IsAlive);
//      }
//      Assert.False(lt.IsAlive);
//      Assert.False(lf.IsAlive);
//    }

#if !NET35
    [Test]
    public void TestCancellationToken1()
    {
      def.Terminate();
      var task = Task.Run(() => {}, lt);
      Log.Root.CatchAndDrop(task.Wait);
      Assert.AreEqual(TaskStatus.Canceled, task.Status);
    }
    

    [Test]
    public void TestCancellationToken2()
    {
      var evt = new ManualResetEvent(false);
      var task = Task.Run(() => { evt.Set();}, lt);
      
      evt.WaitOne();
      def.Terminate();
      
      Log.Root.CatchAndDrop(task.Wait);
      
      Assert.AreEqual(TaskStatus.RanToCompletion, task.Status);
    }
    
    [Test]
    public void TestCancellationToken3()
    {
      var task = Task.Run(() =>
      {
        def.Terminate();
        lt.OnTermination(() => { });
      }, lt);

      Log.Root.CatchAndDrop(task.Wait);
      Assert.AreEqual(TaskStatus.Faulted, task.Status);
    }
    
    [Test]
    public void TestCancellationToken4()
    {
      var task = Task.Run(() =>
      {
        def.Terminate();
        def.ThrowIfNotAlive();
      }, lt);
      
      Log.Root.CatchAndDrop(task.Wait);
      Assert.AreEqual(TaskStatus.Faulted, task.Status);
    }

    [Test, Ignore("Fails on build server")]
    public void TestTooLongExecuting()
    {
      
      var oldTimeout = LifetimeDefinition.WaitForExecutingInTerminationTimeoutMs;
      LifetimeDefinition.WaitForExecutingInTerminationTimeoutMs = 100;

      try
      {

        AutoResetEvent sync = new AutoResetEvent(false);
        var task = lt.StartAttached(TaskScheduler.Default, () => sync.WaitOne());
        def.Terminate();
        var ex = Assert.Catch(ThrowLoggedExceptions, $"First exception from {nameof(LifetimeDefinition)}.Terminate"); //first from terminate
        Assert.True(ex.Message.Contains("ExecuteIfAlive"));

        sync.Set();


        task.Wait();
        ex = Assert.Catch(ThrowLoggedExceptions, $"Second exception from {nameof(LifetimeDefinition.ExecuteIfAliveCookie)}.Dispose"); //second from terminate
        Assert.True(ex.Message.Contains("ExecuteIfAlive"));

      }
      finally
      {
        LifetimeDefinition.WaitForExecutingInTerminationTimeoutMs = oldTimeout;
      }

    }
#endif
    
    
    
    [Test]
    public void T000_Items()
    {
      int count = 0;
      Lifetime.Using(lifetime =>
      {
        lifetime.OnTermination(() => count++);
        lifetime.AddDispose(Disposable.CreateAction(() => count++));
        lifetime.OnTermination(() => count++);
        lifetime.OnTermination(Disposable.CreateAction(() => count++));
      });

      Assert.AreEqual(4, count, "Mismatch.");
    }

    [Test]
    public void T010_SimpleOrder()
    {
      var entries = new List<int>();
      int x= 0 ;
      Lifetime.Using(lifetime =>
      {
        int a = x++;
        lifetime.OnTermination(() => entries.Add(a));
        int b = x++;
        lifetime.AddDispose(Disposable.CreateAction(() => entries.Add(b)));
        int c = x++;
        lifetime.OnTermination(Disposable.CreateAction(() => entries.Add(c)));
        int d = x++;
        lifetime.AddDispose(Disposable.CreateAction(() => entries.Add(d)));
      });

      CollectionAssert.AreEqual(Enumerable.Range(0, entries.Count).Reverse().ToArray(), entries, "Order FAIL.");
    }

    [Test]
    public void T020_DefineNestedOrder()
    {
      var entries = new List<int>();
      int x= 0 ;

      Func<Action> FMakeAdder = () => { var a = x++; return () => entries.Add(a); };  // Fixes the X value at the moment of FMakeAdder call.

      bool flag = false;

      Lifetime.Using(lifetime =>
      {
        lifetime.OnTermination(FMakeAdder());
        lifetime.AddDispose(Disposable.CreateAction(FMakeAdder()));
        Lifetime.Define(lifetime, atomicAction:(lifeNested) => { lifeNested.OnTermination(FMakeAdder()); lifeNested.OnTermination(FMakeAdder()); lifeNested.OnTermination(FMakeAdder());});
        lifetime.AddDispose(Disposable.CreateAction(FMakeAdder()));
        Lifetime.Define(lifetime, atomicAction:(lifeNested) => { lifeNested.OnTermination(FMakeAdder()); lifeNested.OnTermination(FMakeAdder()); lifeNested.OnTermination(FMakeAdder());});
        lifetime.AddDispose(Disposable.CreateAction(FMakeAdder()));
        Lifetime.Define(lifetime, atomicAction:(lifeNested) => lifeNested.OnTermination(() => flag = true)).Terminate();
        Assert.IsTrue(flag, "Nested closing FAIL.");
        flag = false;
        lifetime.AddDispose(Disposable.CreateAction(FMakeAdder()));
      });

      Assert.IsFalse(flag, "Nested closed twice.");

      CollectionAssert.AreEqual(System.Linq.Enumerable.Range(0, entries.Count).Reverse().ToArray(), entries, "Order FAIL.");
      
    }

#if !NET35
    [Test]
    public void CancellationTokenTest()
    {
      var def = Lifetime.Define();      
      
      var sw = new SpinWait();
      var task = Task.Run(() =>
      {
        while (true)
        {
          def.Lifetime.ThrowIfNotAlive();
          sw.SpinOnce();
        }
      }, def.Lifetime);
      
      Thread.Sleep(100);
      def.Terminate();

      try
      {
        task.Wait();
      }
      catch (AggregateException e)
      {
        Assert.True(task.IsCanceled);
        Assert.True(e.IsOperationCanceled());
        return;
      }          

      Assert.Fail("Unreachable");
    }
    
    
    [Test]
    public void CancellationTokenTestAlreadyCancelled()
    {
      var def = Lifetime.Define();
      def.Terminate();
      
      var task = Task.Run(() =>
      {
        Assertion.Fail("Unreachable");
      }, def.Lifetime);

      Assert.Throws<AggregateException>(() => task.Wait());
      
      Assert.True(task.IsCanceled);
    }
    
    [Test]
    public void TestCancellationEternalLifetime()
    {
      var lt = Lifetime.Eternal;
      
      var task = Task.Run(() =>
      {
        lt.ThrowIfNotAlive();
        Thread.Yield();        
      }, lt);

      task.Wait();
      
      Assert.True(task.Status == TaskStatus.RanToCompletion);
    }

    [Test]
    public void TestCreateTaskCompletionSource()
    {
      Assert.True(Lifetime.Terminated.CreateTaskCompletionSource<Unit>().Task.IsCanceled);

      
      
      var t = lt.CreateTaskCompletionSource<Unit>().Task;
      Assert.False(t.IsCompleted);
      
      def.Terminate();
      Assert.True(t.IsCanceled);
    }
    
    
    
    [Test]
    public void TestSynchronizeTaskCompletionSource()
    {
      //lifetime terminated
      var tcs = new TaskCompletionSource<Unit>();
      Lifetime.Terminated.CreateNested().SynchronizeWith(tcs);
      Assert.True(tcs.Task.IsCanceled);
      
      
      //tcs completed
      tcs = new TaskCompletionSource<Unit>();
      tcs.SetResult(Unit.Instance);
      
      Lifetime.Terminated.CreateNested().SynchronizeWith(tcs); //nothing
      Lifetime.Eternal.CreateNested().SynchronizeWith(tcs); //nothing
      
      def.SynchronizeWith(tcs);
      Assert.True(lt.Status == LifetimeStatus.Terminated);
      
      
      //lifetime terminates first
      tcs = new TaskCompletionSource<Unit>();
      var d = new LifetimeDefinition();
      d.SynchronizeWith(tcs);
      Assert.True(d.Lifetime.IsAlive);
      Assert.False(tcs.Task.IsCompleted);
      
      d.Terminate();
      Assert.True(tcs.Task.IsCanceled);
      
      //tcs terminates first
      tcs = new TaskCompletionSource<Unit>();
      d = new LifetimeDefinition();
      d.SynchronizeWith(tcs);
      
      tcs.SetCanceled();
      Assert.True(d.Lifetime.Status == LifetimeStatus.Terminated);
    }

    [Test]
    public void TestTerminatesAfter()
    {
      var lf = TestLifetime.CreateTerminatedAfter(TimeSpan.FromMilliseconds(100));
      Assert.True(lf.IsAlive);
      Thread.Sleep(200);
      Assert.True(lf.IsNotAlive);
      
      lf = TestLifetime.CreateTerminatedAfter(TimeSpan.FromMilliseconds(100));
      Assert.True(lf.IsAlive);
      LifetimeDefinition.Terminate();
      Assert.True(lf.IsNotAlive);
      
      Thread.Sleep(200);
      Assert.True(lf.IsNotAlive);
    }

    [Test]
    public void DoubleAsyncTerminationTest()
    {
      {
        var definition = new LifetimeDefinition();
        var count = 0;
        definition.Lifetime.OnTermination(() => count++);
        definition.Terminate();
        Assert.AreEqual(1, count);
        Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        definition.Terminate();
        Assert.AreEqual(1, count);
      }
      
      {
        var definition = new LifetimeDefinition();
        var count = 0;
        definition.Lifetime.OnTermination(() => count++);
        Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        Assert.AreEqual(1, count);
        definition.Terminate();
        definition.Terminate();
        Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        Assert.AreEqual(1, count);
      }
      
      {
        var definition = new LifetimeDefinition();
        var count = 0;
        definition.Lifetime.OnTermination(() => count++);
        var e1 = new ManualResetEvent(false);
        var e2 = new ManualResetEvent(false);
        var task = TestLifetime.Start(TaskScheduler.Default, () =>
        {
          using (definition.Lifetime.UsingExecuteIfAlive())
          {
            e1.Set();
            e2.WaitOne();
          }
        });

        e1.WaitOne();
        var t = definition.TerminateAsync();
        Assert.IsFalse(t.IsCompleted);
        
        Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        e2.Set();
        
        Assert.IsTrue(task.Wait(TimeSpan.FromSeconds(10)));
        Assert.IsTrue(t.AsTask().Wait(TimeSpan.FromSeconds(10)));
        
        Assert.AreEqual(1, count);
      }
    }

    [Test]
    public void ConcurrentDoubleAsyncTermination()
    {
      for (var i = 0; i < 10000; i++)
      {
        var definition = new LifetimeDefinition();
        var count = 0;
        definition.Lifetime.OnTermination(() => count++);

        const int threadsCount = 10;
        var threads = 0;
        var tasks = Enumerable.Range(0, threadsCount).Select(_ => TestLifetime.Start(TaskScheduler.Default, () =>
        {
          Interlocked.Increment(ref threads);
          SpinWait.SpinUntil(() => Memory.VolatileRead(ref threads) == threadsCount); // sync threads

          Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        })).ToArray();

        Assert.IsTrue(Task.WaitAll(tasks, TimeSpan.FromMinutes(1)));
        Assert.AreEqual(1, count);
      }
    }

    [Test]
    public void SimpleTerminateAsyncTest()
    {
      {
        var definition = new LifetimeDefinition();
        var currentThread = Thread.CurrentThread;
        var called = false;
        definition.Lifetime.OnTermination(() =>
        {
          Assert.AreEqual(currentThread, Thread.CurrentThread);
          called = true;
        });

        var task = definition.TerminateAsync();
        Assert.IsTrue(task.IsCompletedSuccessfully);
        Assert.IsTrue(called);
      }

      {
        var scheduler = SingleThreadScheduler.RunOnSeparateThread(TestLifetime, "TestScheduler");
        
        var definition = new LifetimeDefinition();
        var called = false;
        definition.Lifetime.OnTermination(() =>
        {
          scheduler.AssertThread();
          called = true;
        });

        Task task;
        var e1 = new SemaphoreSlim(0);
        var e2 = new SemaphoreSlim(0);
        using (var cookie = definition.Lifetime.UsingExecuteIfAlive())
        {
          Assert.IsTrue(cookie.Succeed);
          task = TestLifetime.StartAsync(scheduler, async () =>
          {
            scheduler.AssertThread();
            var task = definition.TerminateAsync();
            Assert.IsFalse(task.IsCompletedSuccessfully);
            Assert.IsFalse(called);

            scheduler.Queue(() => e1.Release());
            await e2.WaitAsync();
            
            scheduler.AssertThread();
            Assert.IsTrue(task.IsCompletedSuccessfully);
            Assert.IsTrue(called);
          });
          
          e1.Wait();
        }

        e2.Release();
        Assert.IsTrue(task.Wait(TimeSpan.FromSeconds(10)));
        
        Assert.AreEqual(TaskStatus.RanToCompletion, task.Status);
        Assert.IsTrue(called);
      }
    }

    [Test]
    public void SimpleNestedAsyncTermination()
    {
      var def1 = new LifetimeDefinition();
      var def2 = def1.Lifetime.CreateNested();
      
      var e1 = new ManualResetEvent(false);
      var e2 = new ManualResetEvent(false);

      TestLifetime.Start(TaskScheduler.Default, () =>
      {
        using (var cookie = def2.UsingExecuteIfAlive())
        {
          Assert.IsTrue(cookie.Succeed);

          e1.Set();
          e2.WaitOne();
        }
      });

      e1.WaitOne();
      
      var task = def1.TerminateAsync();
      Assert.IsFalse(task.IsCompleted);
      Assert.AreEqual(LifetimeStatus.Terminating, def1.Status);
      Assert.AreEqual(LifetimeStatus.Canceling, def2.Status);

      e2.Set();
      
      Assert.IsTrue(task.AsTask().Wait(TimeSpan.FromSeconds(10)));
      
      Assert.IsTrue(task.IsCompletedSuccessfully);
      Assert.AreEqual(LifetimeStatus.Terminated, def1.Status);
      Assert.AreEqual(LifetimeStatus.Terminated, def2.Status);
    }

    [Test]
    public void AsyncDisposableTerminationTest()
    {
      var disposable = new TestAsyncDisposable();
      var definition = new LifetimeDefinition();
      definition.Lifetime.OnTermination(disposable);

      var scheduler = new SequentialScheduler("TestScheduler", TestLifetime);
      var task = TestLifetime.StartAsync(scheduler, async () =>
      {
        var t = definition.TerminateAsync();
        Assert.IsFalse(t.IsCompleted);
        Assert.AreEqual(LifetimeStatus.Terminating, definition.Status);

        Assert.IsTrue(disposable.Disposing);
        Assert.IsFalse(disposable.Disposed);

        await Task.Yield();
        
        Assert.IsTrue(disposable.Disposed);
        Assert.IsTrue(t.IsCompletedSuccessfully);
        Assert.AreEqual(LifetimeStatus.Terminated, definition.Status);
      });
      
      Assert.IsTrue(task.Wait(TimeSpan.FromSeconds(10)));
      Assert.AreEqual(TaskStatus.RanToCompletion, task.Status);
      Assert.AreEqual(LifetimeStatus.Terminated, definition.Status);
    }

    [Test]
    public void AllowAsyncTerminationTest()
    {
      var definition = new LifetimeDefinition {AllowTerminationUnderExecution = true};
      var called = false;
      definition.Lifetime.OnTermination(() => called = true);
      using (definition.Lifetime.UsingExecuteIfAlive())
      {
        definition.TerminateAsync();
        Assert.IsTrue(called);
      }
    }

    [Test]
    public void AsyncTerminationUnderExecutionErrorTest()
    {
      var definition = new LifetimeDefinition();
      var called = false;
      var thread = Thread.CurrentThread;
      definition.Lifetime.OnTermination(() =>
      {
        Assert.AreEqual(thread, Thread.CurrentThread);
        called = true;
      });
      using (definition.Lifetime.UsingExecuteIfAlive())
      {
        Assert.Throws<InvalidOperationException>(() => definition.TerminateAsync());
        Assert.IsFalse(called);
      }
      Assert.IsFalse(called);
      
      Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
      Assert.IsTrue(called);
    }

    [Test]
    public void CancellationTokenAsyncTerminationTest()
    {
      {
        var definition = new LifetimeDefinition();
        var token = definition.Lifetime.ToCancellationToken();
        definition.TerminateAsync();
        Assert.IsTrue(token.IsCancellationRequested);
      }
      
      {
        var definition = new LifetimeDefinition();
        definition.TerminateAsync();
        var token = definition.Lifetime.ToCancellationToken();
        Assert.IsTrue(token.IsCancellationRequested);
      }
      
      {
        var definition = new LifetimeDefinition();
        var token = definition.Lifetime.ToCancellationToken();
        var e1 = new ManualResetEvent(false);
        var e2 = new ManualResetEvent(false);
        TestLifetime.Start(TaskScheduler.Default, () =>
        {
          using (definition.Lifetime.UsingExecuteIfAlive())
          {
            e1.Set();
            e2.WaitOne();
          }
        });
        
        var task = definition.TerminateAsync();
        Assert.IsTrue(token.IsCancellationRequested);
        e2.Set();
        Assert.IsTrue(task.AsTask().Wait(TimeSpan.FromSeconds(10)));
      }
      
      {
        var definition = new LifetimeDefinition();
        var e1 = new ManualResetEvent(false);
        var e2 = new ManualResetEvent(false);
        TestLifetime.Start(TaskScheduler.Default, () =>
        {
          using (definition.Lifetime.UsingExecuteIfAlive())
          {
            e1.Set();
            e2.WaitOne();
          }
        });
        
        var task = definition.TerminateAsync();
        var token = definition.Lifetime.ToCancellationToken();
        Assert.IsTrue(token.IsCancellationRequested);
        e2.Set();
        Assert.IsTrue(task.AsTask().Wait(TimeSpan.FromSeconds(10)));
      }
      
      {
        var definition = new LifetimeDefinition();
        var e1 = new ManualResetEvent(false);
        var e2 = new ManualResetEvent(false);
        TestLifetime.Start(TaskScheduler.Default, () =>
        {
          using (definition.Lifetime.UsingExecuteIfAlive())
          {
            e1.Set();
            e2.WaitOne();
          }
        });
        
        var task = definition.TerminateAsync();
        e2.Set();
        Assert.IsTrue(task.AsTask().Wait(TimeSpan.FromSeconds(10)));
        var token = definition.Lifetime.ToCancellationToken();
        Assert.IsTrue(token.IsCancellationRequested);
      }
    }

    [Test]
    public void ConcurrentCancellationTokenAndAsyncTerminationTest()
    {
      for (int i = 0; i < 10000; i++)
      {
        var definition = new LifetimeDefinition();
        var e1 = new ManualResetEvent(false);
        var e2 = new ManualResetEvent(false);

        var executionTask = TestLifetime.Start(TaskScheduler.Default, () =>
        {
          using (definition.Lifetime.UsingExecuteIfAlive())
          {
            e1.Set();
            e2.WaitOne();
          }
        });

        e1.WaitOne();

        const int threadsCount = 10;
        var threads = 0;
        
        var tasks = Enumerable.Range(0, threadsCount).Select(num => TestLifetime.StartAsync(TaskScheduler.Default, async () =>
        {
          Interlocked.Increment(ref threads);
          SpinWait.SpinUntil(() => Memory.VolatileRead(ref threads) == threadsCount);

          if (num % 2 == 0)
            return definition.ToCancellationToken();

          await definition.TerminateAsync();
          return definition.ToCancellationToken();
        })).ToArray();

        e2.Set();
        
        var whenAllTask = Task.WhenAll(tasks);
        Assert.IsTrue(whenAllTask.Wait(TimeSpan.FromMinutes(1)));
        
        var tokens = whenAllTask.Result;
        Assert.IsTrue(tokens.All(x => x.IsCancellationRequested));

        executionTask.Wait(TimeSpan.FromSeconds(10));
      }
    }
    
    [Test]
    public void ConcurrentCancellationTokenAndTerminationTest()
    {
      for (int i = 0; i < 10000; i++)
      {
        var definition = new LifetimeDefinition();
        var e1 = new ManualResetEvent(false);
        var e2 = new ManualResetEvent(false);

        var executionTask = TestLifetime.Start(TaskScheduler.Default, () =>
        {
          using (definition.Lifetime.UsingExecuteIfAlive())
          {
            e1.Set();
            e2.WaitOne();
          }
        });

        e1.WaitOne();

        const int threadsCount = 10;
        var threads = 0;
        
        var tasks = Enumerable.Range(0, threadsCount).Select(num => TestLifetime.Start(TaskScheduler.Default, () =>
        {
          Interlocked.Increment(ref threads);
          SpinWait.SpinUntil(() => Memory.VolatileRead(ref threads) == threadsCount);

          if (num % 2 == 0)
            return definition.ToCancellationToken();

          definition.Terminate();
          return definition.ToCancellationToken();
        })).ToArray();

        e2.Set();
        
        var whenAllTask = Task.WhenAll(tasks);
        Assert.IsTrue(whenAllTask.Wait(TimeSpan.FromMinutes(1)));
        
        var tokens = whenAllTask.Result;
        Assert.IsTrue(tokens.All(x => x.IsCancellationRequested));

        executionTask.Wait(TimeSpan.FromSeconds(10));
      }
    }


    [Test]
    public void ConcurrentToCancellationTokenTest()
    {
      for (int i = 0; i < 10000; i++)
      {
        var definition = new LifetimeDefinition();
        
        const int threadsCount = 10;
        var threads = 0;
        
        var tasks = Enumerable.Range(0, threadsCount).Select(num => TestLifetime.Start(TaskScheduler.Default, () =>
        {
          Interlocked.Increment(ref threads);
          SpinWait.SpinUntil(() => Memory.VolatileRead(ref threads) == threadsCount);

          return definition.ToCancellationToken();
        })).ToArray();

        var whenAllTask = Task.WhenAll(tasks);
        Assert.IsTrue(whenAllTask.Wait(TimeSpan.FromMinutes(1)));
        
        var tokens = whenAllTask.Result;
        var singleToken = tokens.Distinct().Single();
        Assert.IsFalse(singleToken.IsCancellationRequested);

        if (i % 2 == 0)
          Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        else
          definition.Terminate();
        
        Assert.IsTrue(singleToken.IsCancellationRequested);
      }
    }

    [Test]
    public void TerminatedToCancellationToken()
    {
      var token = LifetimeDefinition.Terminated.ToCancellationToken();
      Assert.AreEqual(token, new CancellationToken(true));
    }

    [Test]
    public void RecursiveTerminateAsync()
    {
      {
        var definition = new LifetimeDefinition();
        var count = 0;
        definition.Lifetime.OnTermination(() => count++);
        definition.Lifetime.OnTermination(() => definition.Terminate());
        Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        Assert.AreEqual(1, count);
        Assert.AreEqual(LifetimeStatus.Terminated, definition.Status);
      }
      
      {
        var definition = new LifetimeDefinition();
        var count = 0;
        definition.Lifetime.OnTermination(() => count++);
        definition.Lifetime.OnTermination(() =>
        {
          Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        });
        
        Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        Assert.AreEqual(1, count);
        Assert.AreEqual(LifetimeStatus.Terminated, definition.Status);
      }
      
      {
        var definition = new LifetimeDefinition();
        var nested = definition.Lifetime.CreateNested();
        var count = 0;
        definition.Lifetime.OnTermination(() => count++);
        nested.Lifetime.OnTermination(() =>
        {
          Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        });
        
        Assert.IsTrue(definition.TerminateAsync().IsCompletedSuccessfully);
        Assert.AreEqual(1, count);
        Assert.AreEqual(LifetimeStatus.Terminated, definition.Status);
        Assert.AreEqual(LifetimeStatus.Terminated, nested.Status);
      }
    }

    [Test]
    public void NonAsyncDisposableTerminationTest()
    {
      var definition = new LifetimeDefinition();
      var disposable = new TestNonAsyncDisposable();
      definition.Lifetime.OnTermination(disposable);
      Assert.IsFalse(disposable.Disposed);
      
      definition.Terminate();
      Assert.IsTrue(disposable.Disposed);
    }
    
    private class TestNonAsyncDisposable : IDisposable, IAsyncDisposable
    {
      public bool Disposing { get; private set; }
      public bool Disposed { get; private set; }

      public void Dispose()
      {
        Disposing = true;
        Disposed = true;
      }

      public ValueTask DisposeAsync() => throw new NotImplementedException(); // must not be called
    }

    
    private class TestAsyncDisposable : IDisposable, IAsyncDisposable
    {
      public bool Disposing { get; private set; }
      public bool Disposed { get; private set; }
      
      public void Dispose() => throw new NotImplementedException(); // must not be called

      public async ValueTask DisposeAsync()
      {
        Disposing = true;
        await Task.Yield();
        Disposed = true;
      }
    }
    
#endif

    [Test]
    public void SimpleOnTerminationStressTest()
    {
      for (int i = 0; i < 100; i++)
      {
        using var lifetimeDefinition = new LifetimeDefinition();
        var lifetime = lifetimeDefinition.Lifetime;
        int count = 0;
        const int threadsCount = 10;
        const int iterations = 1000;
        Task.Factory.StartNew(() =>
        {
          for (int j = 0; j < threadsCount; j++)
          {
            Task.Factory.StartNew(() =>
            {
              for (int k = 0; k < iterations; k++) 
                lifetime.OnTermination(() => count++);
            }, TaskCreationOptions.AttachedToParent | TaskCreationOptions.LongRunning);
          }
        }).Wait();

        lifetimeDefinition.Terminate();
        Assert.AreEqual(threadsCount * iterations, count);
      }
    }
  }
}