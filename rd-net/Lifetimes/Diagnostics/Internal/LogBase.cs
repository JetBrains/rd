using System;

namespace JetBrains.Diagnostics.Internal
{
  public struct LeveledMessage
  {
    public LeveledMessage(LoggingLevel level, string formattedMessage)
    {
      Level = level;
      FormattedMessage = formattedMessage;
    }

    public LoggingLevel Level { get; }
    public string FormattedMessage { get; }
  }


  public abstract class LogBase : ILog
  {
    public event Action<LeveledMessage>? Handlers;
    public string Category { get; }
    public LoggingLevel EnabledLevel { get; set; }

    protected LogBase(string category, LoggingLevel enabledLevel = LoggingLevel.INFO)
    {
      Category = category ?? throw new ArgumentNullException(nameof(category));
      EnabledLevel = enabledLevel;
    }

    public bool IsEnabled(LoggingLevel level)
    {
      return EnabledLevel >= level;
    }

    protected abstract string Format(LoggingLevel level, string? message, Exception? exception);

    public virtual void Log(LoggingLevel level, string? message, Exception? exception = null)
    {
      if (!IsEnabled(level))
        return;

      LogLog.Catch(() =>
        {
          var formatted = Format(level, message, exception);
          Handlers?.Invoke(new LeveledMessage(level, formatted));
        }
      );
    }
  }

  public abstract class LogFactoryBase : ILogFactory
  {
    public event Action<LeveledMessage>? Handlers;

    public ILog GetLog(string category)
    {
      var log = GetLogBase(category);
      log.Handlers += Handlers;
      return log;
    }

    protected abstract LogBase GetLogBase(string category);
  }
}