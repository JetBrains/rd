using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using NUnit.Framework;

namespace Test.Lifetimes
{
  public class TestLogger : LogBase
  {
    public static readonly TestLogger ExceptionLogger = new TestLogger("Tests");
    public static readonly ILogFactory Factory = new TestLogFactory();

    private readonly object myMonitor = new object();
    private readonly List<Exception> myExceptions = new List<Exception>();

    private TestLogger([NotNull] string category) : base(category, LoggingLevel.VERBOSE)
    {
      Handlers += WriteMessage;
    }

    private void WriteMessage(LeveledMessage message)
    {
      TestContext.Progress.WriteLine(message.FormattedMessage);
    }

    protected override string Format(LoggingLevel level, string message, Exception exception)
    {
      if (level.IsSeriousError())
      {
        try
        {
          throw exception == null ? new Exception(message) : new Exception(message, exception);
        }
        catch (Exception e)
        {
          lock (myMonitor) 
            myExceptions.Add(e);
        }
      }

      return JetBrains.Diagnostics.Log.DefaultFormat(
        DateTime.Now,
        level,
        Category,
        Thread.CurrentThread,
        message,
        exception);
    }

    private void RecycleLogLog()
    {
      //not very thread safe
      foreach (var rec in LogLog.StoredRecords)
      {
        if (rec.Severity == LoggingLevel.ERROR || rec.Severity == LoggingLevel.FATAL)
        {
          myExceptions.Add(new Exception(rec.Format(false)));
        }
      }
      
      LogLog.StoredRecords.Clear();
    }

    [CanBeNull]
    private Exception RecycleLoggedExceptions()
    {
      lock (myMonitor)
      {
        RecycleLogLog();
        
        if (myExceptions.Count == 0) return null;


        var exception = myExceptions.Count == 1 ? myExceptions[0] : new AggregateException(myExceptions.ToArray());
        myExceptions.Clear();

        return exception;
      }
    }

    public void ThrowLoggedExceptions()
    {
      var result = RecycleLoggedExceptions();
      if (result != null) throw result;
    }

    internal class TestLogFactory : LogFactoryBase
    {
      protected override LogBase GetLogBase(string category)
      {
        var testLogger = new TestLogger(category);
        testLogger.Handlers += message => ExceptionLogger.Log(message.Level, message.FormattedMessage);
        return testLogger;
      }
    }
  }
}