using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;

namespace JetBrains.Rd.Impl
{
  public class RdSimpleDispatcher : IScheduler
  {
    private readonly Lifetime myLifetime;
    private readonly ILog myLogger;
    private readonly string myId;

    private readonly Queue<Action> myTasks = new Queue<Action>();
    private readonly AutoResetEvent myEvent = new AutoResetEvent(false);

    public TimeSpan? MessageTimeout = null;

    public RdSimpleDispatcher(Lifetime lifetime, ILog logger, string id = null)
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
        Action nextTask = null;
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

          var timeout = MessageTimeout != null ? (int)MessageTimeout.Value.TotalMilliseconds : -1;
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

    private string FormatLogMessage(string message)
    {
      if (myId == null) return message;
      return $"{myId}: {message}";
    }
  }
}