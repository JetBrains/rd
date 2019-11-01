using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;

namespace JetBrains.Diagnostics.Internal
{
  public class TestLogger : LogBase
  {
    public static readonly TestLogger Logger = new TestLogger("Tests");
    public static readonly ILogFactory Factory = new TestLogFactory();

    private readonly object myMonitor = new object();
    private readonly List<Exception> myExceptions = new List<Exception>();

    private TestLogger([NotNull] string category) : base(category) { }

    protected override string Format(LoggingLevel level, string message, Exception exception)
    {
      if (level.IsSeriousError())
      {
        lock (myMonitor)
        {
          myExceptions.Add(exception ?? new Exception(message));
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
      protected override LogBase GetLogBase(string category) => Logger;
    }
  }
}