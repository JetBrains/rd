using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Lifetimes;

namespace Test.RdCross.Util
{
  public static class Logging
  {
    public static void LogWithTime(string message)
    {
      Console.WriteLine($"At {DateTime.Now:G} {message}");
    }

    public static T TrackAction<T>(string message, Func<T> action)
    {
      using (new LoggingCookie(message))
      {
        return action();
      }
    }

    public static void TrackAction(string message, Action action)
    {
      using (new LoggingCookie(message))
      {
        action();
      }
    }
  }

  class LoggingCookie : IDisposable
  {
    private readonly string myAction;

    public LoggingCookie(string action)
    {
      myAction = action;
      Logging.LogWithTime($"{myAction} started");
    }

    public void Dispose()
    {
      Logging.LogWithTime($"{myAction} finished");
    }
  }

  public class CrossTestsLog : LogBase
  {
    public TextWriter Writer { get; }

    private static readonly string[] ourIncludedCategories = {"protocol.SEND", "protocol.RECV"};
    
    protected override string Format(LoggingLevel level, string message, Exception exception)
    {
      return JetBrains.Diagnostics.Log.DefaultFormat(null, level, Category, null, message, exception);
    }

    public CrossTestsLog([NotNull] TextWriter writer, [NotNull] string category) : base(category, LoggingLevel.TRACE)
    {
      Writer = writer ?? throw new ArgumentNullException(nameof(writer));
      Handlers += WriteMessage;
    }

    private void WriteMessage(LeveledMessage msg)
    {
      if (ourIncludedCategories.Contains(Category))
      {
        lock (Writer) //Can't use TextWriter.Synchronized in NetCore 1.1
        {
          Writer.Write(msg.FormattedMessage);
          Writer.Flush();
        }
      }
    }
  }

  public class CrossTestsLogFactory : LogFactoryBase
  {
    public LoggingLevel EnabledLevel { get; }
    public TextWriter Writer { get; }

    public CrossTestsLogFactory([NotNull] TextWriter writer)
    {
      EnabledLevel = LoggingLevel.TRACE;
      Writer = writer ?? throw new ArgumentNullException(nameof(writer));
    }

    protected override LogBase GetLogBase(string category)
    {
      return new CrossTestsLog(Writer, category);
    }
  }
}