using System;
using JetBrains.Annotations;
using JetBrains.Util;

namespace JetBrains.Diagnostics
{
  /// <summary>
  /// Logger's frontend. This class is used as entry point for logging in this library and could
  /// be bound to any logger's backend (say log4net) used in your solution. <see cref="Log"/> 
  /// </summary>
  public interface ILog
  {    
    [NotNull] string Category { get; }
    bool IsEnabled(LoggingLevel level);
    void Log(LoggingLevel level, [CanBeNull] string message, [CanBeNull] Exception exception = null);
  }
}