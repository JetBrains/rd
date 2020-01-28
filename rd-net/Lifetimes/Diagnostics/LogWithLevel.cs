using System;
using JetBrains.Annotations;

namespace JetBrains.Diagnostics
{
  /// <summary>
  /// Special struct to replace common patten by one-liner:
  /// Old code:
  /// <code>
  ///   if (logger.IsTraceEnabled())
  ///     logger.Trace($"value: {HeavyComputation()}"")
  /// </code>
  /// New code:
  /// <code>
  ///   logger.Trace()?.Log($"value: {HeavyComputation()}")
  /// </code>
  /// </summary>
  /// <seealso cref="LogEx.Trace(JetBrains.Diagnostics.ILog,string)"/>
  /// <seealso cref="LogEx.Verbose(JetBrains.Diagnostics.ILog,string)"/>
  public struct LogWithLevel
  {
    [PublicAPI] public ILog Logger { get; }
    [PublicAPI] public LoggingLevel Level {get; }

    [PublicAPI] public LogWithLevel([NotNull] ILog logger, LoggingLevel level)
    {
      Logger = logger ?? throw new ArgumentNullException(nameof(logger));
      Level = level;
    }

    /// <summary>
    /// Log <paramref name="message"/> (via <see cref="ILog.Log"/>) with level <see cref="Level"/>
    /// </summary>
    /// <param name="message"/>
    [PublicAPI] public void Log(string message) => Logger.Log(Level, message);

    /// <summary>
    /// Create <see cref="LogWithLevel"/> only if provided level for given logger is enabled.
    /// </summary>
    /// <param name="logger"/>
    /// <param name="level"/>
    /// <returns><see cref="LogWithLevel"/>(<paramref name="logger"/>, <paramref name="level"/>) if <see cref="ILog.IsEnabled"/> for given level;
    /// null otherwise</returns>
    [PublicAPI] public static LogWithLevel? CreateIfEnabled([NotNull] ILog logger, LoggingLevel level) => 
      logger.IsEnabled(level) ? new LogWithLevel(logger, level) : (LogWithLevel?) null;
  }
}

