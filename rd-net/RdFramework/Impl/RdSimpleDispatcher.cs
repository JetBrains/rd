using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Util;

namespace JetBrains.Rd.Impl
{
  public class RdSimpleDispatcher : IRunWhileScheduler
  {
    private readonly Lifetime myLifetime;
    private readonly ILog myLogger;
    private readonly string? myId;

    private readonly Queue<Action> myTasks = new Queue<Action>();
    private readonly AutoResetEvent myEvent = new AutoResetEvent(false);

    public TimeSpan? MessageTimeout = null;

    public RdSimpleDispatcher(Lifetime lifetime, ILog logger, string? id = null)
    {
      myLogger = logger;
      myId = id;
      myLifetime = lifetime;
      myLifetime.OnTermination(() =>
      {
        myLogger.Trace("Terminate dispatcher");
        myEvent.Set();
      });
    }

    public virtual void Run()
    {
      while (myLifetime.IsAlive)
      {
        Action? nextTask = null;
        lock (myTasks)
        {
          if (myTasks.Count > 0)
            nextTask = myTasks.Dequeue();
        }

        if (nextTask != null)
        {
          try
          {
            myLogger.Trace(FormatLogMessage("Process incoming task"));
            nextTask();
          }
          catch (Exception e)
          {
            myLogger.Error(e, FormatLogMessage("Exception during task processing"));
          }
        }
        else
        {
          if (!myLifetime.IsAlive)
          {
            myLogger.Verbose(FormatLogMessage("Lifetime terminated. Exiting."));
            return;
          }

          var timeout = MessageTimeout == null || MessageTimeout == TimeSpan.MaxValue ? -1 : (int)MessageTimeout.Value.TotalMilliseconds;
          if (!myEvent.WaitOne(timeout))
          {
            throw new Exception($"Cannot receive a message in {timeout} ms");
          }
          myLogger.Trace(FormatLogMessage("Awakened"));
        }
      }
    }

    public virtual bool IsActive => true;

    public virtual bool OutOfOrderExecution => false;

    public void Queue(Action action)
    {
      myLogger.Trace(FormatLogMessage("Queuing task"));
      lock (myTasks)
        myTasks.Enqueue(action);
      myEvent.Set();
    }

    public bool RunWhile(Func<bool> condition, TimeSpan timeout)
    {
      var stopwatch = timeout == TimeSpan.MaxValue ? (LocalStopwatch?)null : LocalStopwatch.StartNew();

      while (condition())
      {
        if (stopwatch.HasValue && stopwatch.Value.Elapsed >= timeout)
        {
          return false;
        }

        Action? nextTask = null;
        lock (myTasks)
        {
          if (myTasks.Count > 0)
            nextTask = myTasks.Dequeue();
        }

        if (nextTask != null)
        {
          try
          {
            myLogger.Trace(FormatLogMessage("Process incoming task in RunWhile"));
            nextTask();
          }
          catch (Exception e)
          {
            myLogger.Error(e, FormatLogMessage("Exception during RunWhile task processing"));
          }
        }
        else
        {
          // Wait for a short time for new tasks
          var waitTime = 5; // milliseconds
          myEvent.WaitOne(waitTime);
        }
      }
      return true;
    }
    
    private string FormatLogMessage(string message)
    {
      if (myId == null) return message;
      return $"{myId}: {message}";
    }    
  }
}