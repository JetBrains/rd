
namespace JetBrains.Diagnostics
{
  public enum LoggingLevel
  {
    /// <summary>
    /// Do not use it in logging. Only in config to disable logging.
    /// </summary>
    OFF = 0,
    /// <summary>
    /// For errors that lead to application failure 
    /// </summary>
    FATAL = 1,
    /// <summary>
    /// For errors that must be shown in Exception Browser
    /// </summary>
    ERROR = 2,

    /// <summary>
    /// Suspicious situations but not errors
    /// </summary>
    WARN = 3,
    
    /// <summary>
    /// Regular level for important events
    /// </summary>
    INFO = 4,

    /// <summary>
    /// Additional info for debugging
    /// </summary>
    VERBOSE = 5,

    /// <summary>
    /// Methods &amp; callstacks tracing, more than verbose
    /// </summary>
    TRACE = 6
  }


  public static class LoggingLevelEx
  {
    public static bool IsSeriousError(this LoggingLevel level)
    {
      return level == LoggingLevel.FATAL || level == LoggingLevel.ERROR;
    }

    public static LoggingLevel AtLeast(this LoggingLevel? level, LoggingLevel least)
    {
      if (level == null || level.Value < least) return least;
      return level.Value;
    } 
    
  }

}