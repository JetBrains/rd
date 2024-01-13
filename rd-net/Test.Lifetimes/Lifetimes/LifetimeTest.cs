using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Lifetimes;
using NUnit.Framework;
#if !NET35
using System.Diagnostics;
using JetBrains.Threading;
using Microsoft.Diagnostics.Runtime;
#endif

// ReSharper disable MethodSupportsCancellation

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
      var warningReceived = false;
      Exception? receivedException = null;

#if !NET35
      const string stackTraceHeader = "CurrentProcessThreadDumps:";
      var executionWasNotCancelledByTimeoutReceived = false;
#endif

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

        var def2 = lifetime.CreateNested();
#if !NET35
        LifetimeDefinition.AdditionalDiagnostics = new LifetimeDefinition.AdditionalDiagnosticsInfo(false, async (lf) => 
        {
          var stacks = GetCurrentProcessThreadDumps();
          Assert.AreEqual(lifetimeDefinition.Lifetime, lf);
          executionWasNotCancelledByTimeoutReceived = true;
          return $"{stackTraceHeader}\n{stacks}";
        });
#endif
        
        def2.Terminate();
#if !NET35
        Assert.IsFalse(executionWasNotCancelledByTimeoutReceived);
#endif
        
        var lifetimeTerminatedEvent = new ManualResetEvent(false);
        var backgroundThreadIsInTryExecuteEvent = new ManualResetEvent(false);
        var thread = new Thread(() => lifetimeDefinition.Lifetime.TryExecute(() =>
        {
          backgroundThreadIsInTryExecuteEvent.Set();
          WaitForLifetimeTerminatedEvent(lifetimeTerminatedEvent);
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

          receivedException = e;
        }
      });

      Assert.IsTrue(warningReceived, "Warning `{0}` must have been logged", expectedWarningText);
      Assert.IsNotNull(receivedException, "Exception `{0}` must have been logged", expectedExceptionText);
      
#if !NET35
      Assert.IsTrue(executionWasNotCancelledByTimeoutReceived);
      Assert.IsTrue(receivedException.Message.Contains(stackTraceHeader), $"Exception `{expectedExceptionText}` doesn't contain {stackTraceHeader}");
      Assert.IsTrue(receivedException.Message.Contains(nameof(WaitForLifetimeTerminatedEvent)), $"Exception `{expectedExceptionText}` doesn't contain {nameof(WaitForLifetimeTerminatedEvent)} method");

      static string GetCurrentProcessThreadDumps()
      {
        if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
        {
          // clrmd crashes the process if os is not Windows, so just return the name of the method
          return nameof(WaitForLifetimeTerminatedEvent);
        }

        using var dataTarget = DataTarget.AttachToProcess(Process.GetCurrentProcess().Id, suspend: false);
        var clrVersion = dataTarget.ClrVersions.SingleOrDefault() ?? throw new Exception("Failed to get single clr from current process");

        using var runtime = clrVersion.CreateRuntime();
        var output = new StringBuilder();
        foreach (var clrThread in runtime.Threads)
        {
          if (!clrThread.IsAlive)
            continue;
          output.AppendLine($"Thread #{clrThread.ManagedThreadId}:");

          foreach (var frame in clrThread.EnumerateStackTrace())
            output.AppendLine($"\tat {frame}");
        }

        return output.ToString();
      }
#endif
    }

    [MethodImpl(MethodImplOptions.NoInlining | MethodImplOptions.NoOptimization)]
    private static void WaitForLifetimeTerminatedEvent(ManualResetEvent lifetimeTerminatedEvent)
    {
      lifetimeTerminatedEvent.WaitOne();
    }
    
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
    
    [Test]
    public void TestTerminationTimeout()
    {
      var defA = new LifetimeDefinition(lt) { TerminationTimeoutKind = LifetimeTerminationTimeoutKind.Long };
      var defB = new LifetimeDefinition(defA.Lifetime);
      var defC = new LifetimeDefinition(defA.Lifetime) { TerminationTimeoutKind = LifetimeTerminationTimeoutKind.Short };
      
      Assert.AreEqual(LifetimeTerminationTimeoutKind.Long, defB.TerminationTimeoutKind);
      Assert.AreEqual(LifetimeTerminationTimeoutKind.Long, defA.TerminationTimeoutKind);
      Assert.AreEqual(LifetimeTerminationTimeoutKind.Short, defC.TerminationTimeoutKind);
    }
    
    [TestCase(LifetimeTerminationTimeoutKind.Default)]
    [TestCase(LifetimeTerminationTimeoutKind.Short)]
    [TestCase(LifetimeTerminationTimeoutKind.Long)]
    [TestCase(LifetimeTerminationTimeoutKind.ExtraLong)]
    public void TestSetTestTerminationTimeout(LifetimeTerminationTimeoutKind timeoutKind)
    {
      var oldTimeoutMs = LifetimeDefinition.GetTerminationTimeoutMs(timeoutKind);
      try
      {
        LifetimeDefinition.SetTerminationTimeoutMs(timeoutKind, 2000);

        var subDef = new LifetimeDefinition(lt) { TerminationTimeoutKind = timeoutKind };
        var subLt = subDef.Lifetime;
      
        subLt.ExecuteAsync(() => Task.Delay(750));
        subDef.Terminate();

        Assert.DoesNotThrow(ThrowLoggedExceptions);
      }
      finally
      {
        LifetimeDefinition.SetTerminationTimeoutMs(timeoutKind, oldTimeoutMs);
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
    public void CancellationTokenStressTest()
    {
      var cancel = new CancellationTokenSource(1000).Token;
      
      var def = new LifetimeDefinition();
      Task.WaitAll(
        Task.Run(() =>
        {
          while (!cancel.IsCancellationRequested)
          {
            def.Terminate();
            Thread.Yield();
            def = new LifetimeDefinition();
          }
        }),
        Task.Run(() =>
        {
          while (!cancel.IsCancellationRequested) 
            def.ToCancellationToken();
        }));
    }

    [Test]
    public void CancellationTokenActualCancellationStressTest()
    {
      var cancel = new CancellationTokenSource(1000).Token;
      
      var sum = 0;
      var def = Lifetime.Define();
      Task.WaitAll(
        Task.Run(() =>
        {
          while (!cancel.IsCancellationRequested)
          {
            def.Terminate();
            Thread.Yield();
            def = new LifetimeDefinition();
          }
          def.Terminate();
        }),
        Task.Run(CheckerProc),
        Task.Run(CheckerProc),
        Task.Run(CheckerProc),
        Task.Run(CheckerProc),
        Task.Run(CheckerProc),
        Task.Run(CheckerProc)
      );

      void CheckerProc()
      {
        var cache = def;
        var localSum = 0;
        while (!cancel.IsCancellationRequested)
        {
          if (def != cache)
          {
            cache = def;
            Interlocked.Increment(ref localSum);
            cache.ToCancellationToken().Register(() => Interlocked.Decrement(ref localSum));
          }
        }
        SpinWait.SpinUntil(() => def.Lifetime.Status == LifetimeStatus.Terminated);
        Interlocked.Add(ref sum, localSum);
      }

      Assert.AreEqual(0, sum);
    }
    
    [Test]
    public void CancellationTokenMultiThreadTerminationTest()
    {
      const int n = 6;
      const int magicNumber = (n + 1) * 1000;
      
      for (int i = 0; i < 1000; i++)
      {
        var count = 0;
        var sum = 0;
        var def = TestLifetime.CreateNested();
        var creatorTask = Task.Run(Creator);

        Task.WaitAll(Enumerable.Range(0, n).Select(_ => Task.Run(Terminator)).Concat(new []{creatorTask}).ToArray());

        def.Terminate();

        Assert.AreEqual(0, sum);

        void Creator()
        {
          while (Volatile.Read(ref count) <= magicNumber)
          {
            var newDef = new LifetimeDefinition();
            Interlocked.Increment(ref sum);
            newDef.ToCancellationToken().Register(() => Interlocked.Decrement(ref sum));
            Interlocked.Exchange(ref def, newDef).Terminate();
            Interlocked.Increment(ref count);
          }
        }

        void Terminator()
        {
          while (Volatile.Read(ref count) <= magicNumber)
          {
            Volatile.Read(ref def).Terminate();
            Interlocked.Increment(ref count);
          }
        }
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

    [Test]
    public void IntersectionsAndInheritTimeoutKindTest()
    {
      var lf1 = new LifetimeDefinition();
      var lf2 = new LifetimeDefinition();
      var lf3 = new LifetimeDefinition();

      DoTest1(LifetimeTerminationTimeoutKind.Default);
      DoTest2(LifetimeTerminationTimeoutKind.Default);

      lf1.TerminationTimeoutKind = LifetimeTerminationTimeoutKind.ExtraLong;
      DoTest1(LifetimeTerminationTimeoutKind.Default);
      DoTest2(LifetimeTerminationTimeoutKind.Default);

      lf2.TerminationTimeoutKind = LifetimeTerminationTimeoutKind.Long;
      DoTest1(LifetimeTerminationTimeoutKind.Long);
      DoTest2(LifetimeTerminationTimeoutKind.Default);

      lf3.TerminationTimeoutKind = LifetimeTerminationTimeoutKind.Short;
      DoTest1(LifetimeTerminationTimeoutKind.Long);
      DoTest2(LifetimeTerminationTimeoutKind.Short);

      void DoTest1(LifetimeTerminationTimeoutKind expected)
      {
        var definedLifetime = Lifetime.DefineIntersection(lf1.Lifetime, lf2.Lifetime);
        var outerDefinedLifetime = OuterLifetime.DefineIntersection(lf1.Lifetime, lf2.Lifetime);
        Assert.AreEqual(expected, definedLifetime.TerminationTimeoutKind);
        Assert.AreEqual(expected, outerDefinedLifetime.TerminationTimeoutKind);
      }

      void DoTest2(LifetimeTerminationTimeoutKind expected)
      {
        var definedLifetime = Lifetime.DefineIntersection(lf1.Lifetime, lf2.Lifetime, lf3.Lifetime);
        var outerDefinedLifetime = OuterLifetime.DefineIntersection(lf1.Lifetime, lf2.Lifetime, lf3.Lifetime);
        Assert.AreEqual(expected, definedLifetime.TerminationTimeoutKind);
        Assert.AreEqual(expected, outerDefinedLifetime.TerminationTimeoutKind);
      }
    }

    [Test]
    public void DefineLifetimeInheritTimeoutKindTest()
    {
      var definition = new LifetimeDefinition { TerminationTimeoutKind = LifetimeTerminationTimeoutKind.ExtraLong };
      Assert.AreEqual(LifetimeTerminationTimeoutKind.ExtraLong, Lifetime.Define(definition.Lifetime, "id", (LifetimeDefinition ld) => {}).TerminationTimeoutKind);
      Assert.AreEqual(LifetimeTerminationTimeoutKind.ExtraLong, Lifetime.Define(definition.Lifetime, "id", (Lifetime ld) => {}).TerminationTimeoutKind);
      
      Assert.AreEqual(LifetimeTerminationTimeoutKind.ExtraLong, OuterLifetime.Define(definition.Lifetime, "id", (ld, lf) => {}).TerminationTimeoutKind);
    }

    [Test]
    public void EternalLifetimeKeepalive()
    {
      Assert.DoesNotThrow(() =>
      {
        var o = new object();
        Lifetime.Eternal.KeepAlive(o);
      });
      var ex = Assert.Throws<Exception>(() =>
      {
        TestLogger.ExceptionLogger.ThrowLoggedExceptions();
      });
      Assert.True(ex.Message.Contains("!IsEternal"));
    }
  }
}