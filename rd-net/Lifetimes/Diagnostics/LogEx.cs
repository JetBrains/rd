using System;
using System.Diagnostics;
using System.Runtime.CompilerServices;
using JetBrains.Annotations;

namespace JetBrains.Diagnostics
{
  public static class LogEx
  {

    public static ILog GetSublogger([NotNull] this ILog log, [NotNull] string subcategory)
    {
      return Log.GetLog(log.Category + "." + subcategory);
    }


    
    #region IsEnabled

    public static bool IsTraceEnabled([NotNull] this ILog @this)
    {
      return @this.IsEnabled(LoggingLevel.TRACE);
    }

    public static bool IsVersboseEnabled([NotNull] this ILog @this)
    {
      return @this.IsEnabled(LoggingLevel.VERBOSE);
    }
    

    #endregion
    
    
    
    #region LogFormat
    
    [StringFormatMethod("message")]
    public static void LogFormat<T1>([NotNull] this ILog @this, LoggingLevel level, string message, T1 t1)
    {
      if(@this.IsEnabled(level))
        @this.Log(level, message.FormatEx(t1));
    }

    [StringFormatMethod("message")]
    public static void LogFormat<T1, T2>([NotNull] this ILog @this, LoggingLevel level, string message, T1 t1, T2 t2)
    {
      if(@this.IsEnabled(level))
        @this.Log(level, message.FormatEx(t1, t2));
    }

    [StringFormatMethod("message")]
    public static void LogFormat<T1, T2, T3>([NotNull] this ILog @this, LoggingLevel level, string message, T1 t1, T2 t2, T3 t3)
    {
      if(@this.IsEnabled(level))
        @this.Log(level, message.FormatEx(t1, t2, t3));
    }

    [StringFormatMethod("message")]
    public static void LogFormat<T1, T2, T3, T4>([NotNull] this ILog @this, LoggingLevel level, string message, T1 t1, T2 t2, T3 t3, T4 t4)
    {
      if(@this.IsEnabled(level))
        @this.Log(level, message.FormatEx(t1, t2, t3, t4));
    }

    //Universal method for many parameter
    [StringFormatMethod("message")]
    public static void LogFormat([NotNull] this ILog @this, LoggingLevel level, string message, params object[] args)
    {
      if(@this.IsEnabled(level))
        @this.Log(level, message.FormatEx(args));
    }
    #endregion
    
    
    
    
    #region Trace

    public static void Trace([NotNull] this ILog @this, [NotNull] string message)
    {
      @this.Log(LoggingLevel.TRACE, message);      
    }    

    [StringFormatMethod("message")]
    public static void Trace<T1>([NotNull] this ILog @this, string message, T1 t1)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1);
    }

    [StringFormatMethod("message")]
    public static void Trace<T1, T2>([NotNull] this ILog @this, string message, T1 t1, T2 t2)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1, t2);
    }

    [StringFormatMethod("message")]
    public static void Trace<T1, T2, T3>([NotNull] this ILog @this, string message, T1 t1, T2 t2, T3 t3)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1, t2, t3);
    }

    [StringFormatMethod("message")]
    public static void Trace<T1, T2, T3, T4>([NotNull] this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1, t2, t3, t4);
    }

    [StringFormatMethod("message")]
    public static void Trace<T1, T2, T3, T4, T5>([NotNull] this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1, t2, t3, t4, t5);
    }

    [StringFormatMethod("message")]
    public static void Trace<T1, T2, T3, T4, T5, T6>([NotNull] this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1, t2, t3, t4, t5, t6);
    }

    #endregion

    
    
    #region Verbose
       
    public static void Verbose([NotNull] this ILog @this, [NotNull] string message)
    {
      @this.Log(LoggingLevel.VERBOSE, message);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1>([NotNull] this ILog @this, string message, T1 t1)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1, T2>([NotNull] this ILog @this, string message, T1 t1, T2 t2)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1, t2);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1, T2, T3>([NotNull] this ILog @this, string message, T1 t1, T2 t2, T3 t3)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1, t2, t3);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1, T2, T3, T4>([NotNull] this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1, t2, t3, t4);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1, T2, T3, T4, T5>([NotNull] this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1, t2, t3, t4, t5);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1, T2, T3, T4, T5, T6>([NotNull] this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1, t2, t3, t4, t5, t6);
    }

    [StringFormatMethod("message")]
    public static void Verbose([NotNull] this ILog @this, string message, object[] args)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, args);
    }

    public static void Verbose([NotNull] this ILog @this, [NotNull] Exception ex, string message = null)
    {
      @this.Log(LoggingLevel.VERBOSE, message, ex);
    }

    #endregion

    #region Info
    public static void Info([NotNull] this ILog @this, [NotNull] string message)
    {
      @this.Log(LoggingLevel.INFO, message);
    }

    [StringFormatMethod("message")]
    public static void Info([NotNull] this ILog @this, [NotNull] string message, params object[] args)
    {
      @this.LogFormat(LoggingLevel.INFO, message, args);
    }

    public static void Info([NotNull] this ILog @this, [NotNull] Exception ex, string message = null)
    {
      @this.Log(LoggingLevel.INFO, message, ex);
    }


    #endregion


    #region Warn    
    public static void Warn([NotNull] this ILog @this, [NotNull] string message)
    {
      @this.Log(LoggingLevel.WARN, message);
    } 
    
    [StringFormatMethod("message")]
    public static void Warn([NotNull] this ILog @this, [NotNull] string message, params object[] args)
    {
      @this.LogFormat(LoggingLevel.WARN, message, args);
    }
    
    public static void Warn([NotNull] this ILog @this, [NotNull] Exception ex, string message = null)
    {
      @this.Log(LoggingLevel.WARN, message, ex);
    }

    
    #endregion
    
    
    #region Error    
    [StringFormatMethod("message")]
    public static void Error([NotNull] this ILog @this, [NotNull] string message)
    {
      @this.Log(LoggingLevel.ERROR, message);
    } 
    
    [StringFormatMethod("message")]
    public static void Error([NotNull] this ILog @this, [NotNull] string message, params object[] args)
    {
      @this.LogFormat(LoggingLevel.ERROR, message, args);
    }
    
    [StringFormatMethod("message")]
    public static void Error([NotNull] this ILog @this, [NotNull] string message, Exception e)
    {
      @this.Log(LoggingLevel.ERROR, message, e);
    }
    
    [StringFormatMethod("message")]
    public static void Error([NotNull] this ILog @this, [NotNull] Exception ex, string message = null)
    {
      @this.Log(LoggingLevel.ERROR, message, ex);
    }
    
    #endregion
    
    
    
    
    #region Assert
    
    public static void Assert([NotNull] this ILog @this, bool condition, string message)
    {
      if (!condition)
      {
        @this.Error(message);
      }
    }
    
    public static void Assert<T1>([NotNull] this ILog @this, bool condition, string message, T1 t1)
    {
      if (!condition)
      {
        @this.Error(message, t1);
      }
    } 
    
    public static void Assert([NotNull] this ILog @this, bool condition, string message, params object[] args)
    {
      if (!condition)
      {
        @this.Error(message, args);
      }
    } 
    
    #endregion

    

    #region Catch
       
    /// <summary>
    /// Run <paramref name="action"/> and in case of exception log it with <see cref="LoggingLevel"/> == ERROR. Do not throw exception (if any). 
    /// </summary>
    /// <param name="log"></param>
    /// <param name="action"></param>
#if !NET35
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
#endif  
    public static void Catch(this ILog log, Action action)
    {
      try
      {
        action();
      }
      catch(Exception e)
      {
        log.Error(e);
      }            
    }
    
    /// <summary>
    /// Run <paramref name="action"/> and in case of exception log it with <see cref="LoggingLevel"/> == ERROR. Do not throw exception (if any). 
    /// </summary>
    /// <param name="log"></param>
    /// <param name="action"></param>
    /// <returns>result of action() or <c>default(T)></c> if exception arises</returns>
#if !NET35
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
#endif  
    [PublicAPI] public static T Catch<T>(this ILog log, Func<T> action)
    {
      try
      {
        return action();
      }
      catch(Exception e)
      {
        log.Error(e);
        return default;
      }            
    }    

    /// <summary>
    /// Run <paramref name="action"/> and in case of exception discard it. Do not throw exception (if any). 
    /// </summary>
    /// <param name="log"></param>
    /// <param name="action"></param>
#if !NET35
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
#endif  
    [PublicAPI] public static void CatchAndDrop(this ILog log, Action action)
    {
      try
      {
        action();
      }
      catch(Exception e)
      {
        DropException(e);
      }            
    }
    
    /// <summary>
    /// Run <paramref name="action"/> and in case of exception discard it. Do not throw exception (if any). 
    /// </summary>
    /// <param name="log"></param>
    /// <param name="action"></param>
    /// <returns>result of action() or <c>default(T)></c> if exception arises</returns>
#if !NET35
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
#endif  
    [PublicAPI] public static T CatchAndDrop<T>(this ILog log, Func<T> action)
    {
      try
      {
        return action();
      }
      catch(Exception e)
      {
        DropException(e);
        return default;
      }            
    }
    
    /// <summary>
    /// Run <paramref name="action"/> and in case of exception log it with <see cref="LoggingLevel"/> == WARN. Do not throw exception (if any). 
    /// </summary>
    /// <param name="log"></param>
    /// <param name="action"></param>
#if !NET35
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
#endif
    [PublicAPI] public static void CatchWarn(this ILog log, Action action)
    {
      try
      {
        action();
      }
      catch(Exception e)
      {
        log.Warn(e);
      }            
    }
    
    /// <summary>
    /// Run <paramref name="action"/> and in case of exception log it with <see cref="LoggingLevel"/> == WARN. Do not throw exception (if any). 
    /// </summary>
    /// <param name="log"></param>
    /// <param name="action"></param>
    /// <returns>result of action() or <c>default(T)></c> if exception arises</returns>
#if !NET35
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
#endif
    [PublicAPI] public static T CatchWarn<T>(this ILog log, Func<T> action)
    {
      try
      {
        return action();
      }
      catch(Exception e)
      {
        log.Warn(e);
        return default;
      }            
    }
    

    private static void DropException(Exception e)
    {
      //do nothing
    }
    
    #endregion
    
    
    
    #region Helpers
    
      
    [MethodImpl(MethodImplOptions.NoInlining)]
    [StringFormatMethod("s")]
    private static string FormatEx(this string s, params object[] p)
    {
      return string.Format(s, p);
    }

    #endregion


    #region LogWithLevel
    
    /// <summary>
    /// One-line shortcut builder to replace common pattern:
    /// <code>
    ///   if (logger.IsTraceEnabled())
    ///     logger.Trace($"some messages with {HeavyComputation()}"")
    /// </code>
    /// Usage:
    /// <code>
    ///   logger.Trace()?.Log($"some messages with {HeavyComputation()}")
    /// </code>
    /// </summary>
    /// <param name="logger"/>
    /// <returns>struct <see cref="LogWithLevel"/>(<paramref name="logger"/>, <see cref="LoggingLevel.TRACE"/>) if <see cref="IsTraceEnabled"/>;
    /// null otherwise</returns>
    [PublicAPI] public static LogWithLevel? Trace(this ILog logger) => LogWithLevel.CreateIfEnabled(logger, LoggingLevel.TRACE);
    
    /// <summary>
    /// One-line shortcut builder to replace common pattern:
    /// <code>
    ///   if (logger.IsVerboseEnabled())
    ///     logger.Verbose($"some messages with {HeavyComputation()}"")
    /// </code>
    /// Usage:
    /// <code>
    ///   logger.Verbose()?.Log($"some messages with {HeavyComputation()}")
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <returns>struct <see cref="LogWithLevel"/>(<paramref name="logger"/>, <see cref="LoggingLevel.VERBOSE"/>) if <see cref="IsVersboseEnabled"/>;
    /// null otherwise</returns>
    [PublicAPI] public static LogWithLevel? Verbose(this ILog logger) => LogWithLevel.CreateIfEnabled(logger, LoggingLevel.VERBOSE);
    
    #endregion
  }
}