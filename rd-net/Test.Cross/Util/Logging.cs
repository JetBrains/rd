using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Rd.Base;

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

    private readonly Regex myExcludedRegex =
      new Regex(string.Join("|", Enum.GetValues(typeof(RdExtBase.ExtState)).Cast<RdExtBase.ExtState>()));

    private readonly Regex myRdIdRegex = new Regex(@"\(\d+\)|(taskId=\d+)|(send request '\d+')|(task '\d+')");

    private string ProcessMessage(string message)
    {
      return !myExcludedRegex.IsMatch(message) ? myRdIdRegex.Replace(message, "") : null;
    }

    private void WriteMessage(LeveledMessage msg)
    {
      if (ourIncludedCategories.Contains(Category))
      {
        var processedMessage = ProcessMessage(msg.FormattedMessage);
        if (processedMessage != null)
        {
          lock (Writer) //Can't use TextWriter.Synchronized in NetCore 1.1
          {
            Writer.Write(processedMessage);
            Writer.Flush();
          }
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

  /// <summary>
  /// Combine provided loggers. Execute <see cref="Log"/> each of <see cref="myLogs"/> on every event.
  /// </summary>
  public class CombinatorLog : LogBase
  {
    private readonly List<LogBase> myLogs;

    public CombinatorLog(string category, List<LogBase> logs) : base(category, logs.Max(log => log.EnabledLevel))
    {
      myLogs = logs;
    }

    protected override string Format(LoggingLevel level, string message, Exception exception)
    {
      throw new NotSupportedException();
    }

    public override void Log(LoggingLevel level, string message, Exception exception = null)
    {
      foreach (var log in myLogs)
        log.Log(level, message, exception);
    }
  }

  public class CombinatorLogFactory : LogFactoryBase
  {
    private readonly List<ILogFactory> myFactories;

    public CombinatorLogFactory(IEnumerable<ILogFactory> factories)
    {
      myFactories = factories.ToList();
    }

    protected override LogBase GetLogBase(string category) =>
      new CombinatorLog(category, myFactories.Select(factory => factory.GetLog(category)).Cast<LogBase>().ToList());
  }
}