using System;
using JetBrains.Annotations;
using JetBrains.Util;

namespace JetBrains.Diagnostics
{
  /// <summary>
  /// Logger's frontend. This class is used as entry point for logging in this library. It could
  /// be bound to any logger's backend (say log4net) used in your solution by
  /// <list type="number">
  /// <item>Implementing this class</item>
  /// <item>Implementing <see cref="ILogFactory"/></item>
  /// <item>Set as default by <see cref="Log"/>.<see cref="Diagnostics.Log.DefaultFactory"/></item>
  /// </list>
  /// 
  /// <seealso cref="ILogFactory"/> 
  /// <seealso cref="Log"/> 
  /// </summary>
  public interface ILog
  {    
    [NotNull] string Category { get; }
    bool IsEnabled(LoggingLevel level);
    void Log(LoggingLevel level, [CanBeNull] string message, [CanBeNull] Exception exception = null);
  }
}