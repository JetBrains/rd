using System;
using JetBrains.Annotations;
using JetBrains.Util;

namespace JetBrains.Diagnostics
{
  public interface ILog
  {    
    [NotNull] string Category { get; }
    bool IsEnabled(LoggingLevel level);
    void Log(LoggingLevel level, [CanBeNull] string message, [CanBeNull] Exception exception = null);
  }
}