using System;
using JetBrains.Util;

namespace JetBrains.Diagnostics.Internal
{
  public class NullLog : ILog
  {
    public static readonly NullLog Instance = new NullLog();        
    
    private NullLog() {}

    string ILog.Category => "";

    bool ILog.IsEnabled(LoggingLevel level)
    {
      return false;
    }

    void ILog.Log(LoggingLevel level, string message, Exception exception) {}
  }
}