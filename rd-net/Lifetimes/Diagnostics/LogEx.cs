using System;
using System.Runtime.CompilerServices;
using JetBrains.Annotations;
using JetBrains.Diagnostics.StringInterpolation;

namespace JetBrains.Diagnostics
{
  public static class LogEx
  {
    public static ILog GetSublogger(this ILog log, string subcategory)
    {
      return Log.GetLog(log.Category + "." + subcategory);
    }

    #region IsEnabled

    public static bool IsTraceEnabled(this ILog @this)
    {
      return @this.IsEnabled(LoggingLevel.TRACE);
    }

    public static bool IsVersboseEnabled(this ILog @this)
    {
      return @this.IsEnabled(LoggingLevel.VERBOSE);
    }

    #endregion
    #region LogFormat

    [StringFormatMethod("message")]
    public static void LogFormat<T1>(this ILog @this, LoggingLevel level, string message, T1 t1)
    {
      if (@this.IsEnabled(level))
      {
        @this.Log(level, message.FormatEx(t1));
      }
    }

    [StringFormatMethod("message")]
    public static void LogFormat<T1, T2>(this ILog @this, LoggingLevel level, string message, T1 t1, T2 t2)
    {
      if (@this.IsEnabled(level))
      {
        @this.Log(level, message.FormatEx(t1, t2));
      }
    }

    [StringFormatMethod("message")]
    public static void LogFormat<T1, T2, T3>(this ILog @this, LoggingLevel level, string message, T1 t1, T2 t2, T3 t3)
    {
      if (@this.IsEnabled(level))
      {
        @this.Log(level, message.FormatEx(t1, t2, t3));
      }
    }

    [StringFormatMethod("message")]
    public static void LogFormat<T1, T2, T3, T4>(this ILog @this, LoggingLevel level, string message, T1 t1, T2 t2, T3 t3, T4 t4)
    {
      if (@this.IsEnabled(level))
      {
        @this.Log(level, message.FormatEx(t1, t2, t3, t4));
      }
    }

    //Universal method for many parameter
    [StringFormatMethod("message")]
    public static void LogFormat(this ILog @this, LoggingLevel level, string message, params object?[] args)
    {
      if (@this.IsEnabled(level))
      {
        @this.Log(level, message.FormatEx(args));
      }
    }

    #endregion
    #region Trace

    public static void Trace(this ILog @this, string message)
    {
      @this.Log(LoggingLevel.TRACE, message);
    }

    /// <summary>
    /// Log the message if <see cref="LoggingLevel.TRACE"/> is enabled, otherwise the message will not be logged, moreover, no calculations (including method calls) will be performed.
    /// <br />
    /// <br />
    /// For example, the code below
    /// <code>
    /// logger.Trace($"{DoSmthSlow()}");
    /// </code>
    /// 
    /// will be compiled into
    /// 
    /// <code>
    /// var handler = new JetLogTraceInterpolatedStringHandler(logger, out var isEnabled);
    /// if (isEnabled)
    ///   handler.Append(DoSmthSlow());
    /// logger.Trace(ref handler);
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <param name="messageHandler"></param>
    public static void Trace(this ILog logger, [InterpolatedStringHandlerArgument("logger")] ref JetLogTraceInterpolatedStringHandler messageHandler)
    {
      if (messageHandler.IsEnabled)
      {
        logger.Log(LoggingLevel.TRACE, messageHandler.ToStringAndClear());
      }      
    }    

    [StringFormatMethod("message")]
    public static void Trace<T1>(this ILog @this, string message, T1 t1)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1);
    }

    [StringFormatMethod("message")]
    public static void Trace<T1, T2>(this ILog @this, string message, T1 t1, T2 t2)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1, t2);
    }

    [StringFormatMethod("message")]
    public static void Trace<T1, T2, T3>(this ILog @this, string message, T1 t1, T2 t2, T3 t3)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1, t2, t3);
    }

    [StringFormatMethod("message")]
    public static void Trace<T1, T2, T3, T4>(this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1, t2, t3, t4);
    }

    [StringFormatMethod("message")]
    public static void Trace<T1, T2, T3, T4, T5>(this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1, t2, t3, t4, t5);
    }

    [StringFormatMethod("message")]
    public static void Trace<T1, T2, T3, T4, T5, T6>(this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6)
    {
      @this.LogFormat(LoggingLevel.TRACE, message, t1, t2, t3, t4, t5, t6);
    }

    #endregion
    #region Verbose

    public static void Verbose(this ILog @this, string message)
    {
      @this.Log(LoggingLevel.VERBOSE, message);
    }
    
    /// <summary>
    /// Log the message if <see cref="LoggingLevel.VERBOSE"/> is enabled, otherwise the message will not be logged, moreover, no calculations (including method calls) will be performed.
    /// <br />
    /// <br />
    /// For example, the code below
    /// <code>
    /// logger.Verbose($"{DoSmthSlow()}");
    /// </code>
    /// 
    /// will be compiled into
    /// 
    /// <code>
    /// var handler = new JetLogVerboseInterpolatedStringHandler(logger, out var isEnabled);
    /// if (isEnabled)
    ///   handler.Append(DoSmthSlow());
    /// logger.Verbose(ref handler);
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <param name="messageHandler"></param>
    public static void Verbose(this ILog logger, [InterpolatedStringHandlerArgument("logger")] ref JetLogVerboseInterpolatedStringHandler messageHandler)
    {
      if (messageHandler.IsEnabled)
      {
        logger.Log(LoggingLevel.VERBOSE, messageHandler.ToStringAndClear());
      }      
    }

    /// <summary>
    /// Log the message if <see cref="LoggingLevel.VERBOSE"/> is enabled, otherwise the message will not be logged, moreover, no calculations (including method calls) will be performed.
    /// <br />
    /// <br />
    /// For example, the code below
    /// <code>
    /// logger.Verbose(ex, $"{DoSmthSlow()}");
    /// </code>
    /// 
    /// will be compiled into
    /// 
    /// <code>
    /// var handler = new JetLogVerboseInterpolatedStringHandler(logger, out var isEnabled);
    /// if (isEnabled)
    ///   handler.Append(DoSmthSlow());
    /// logger.Verbose(ex, ref handler);
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <param name="ex"></param>
    /// <param name="messageHandler"></param>
    public static void Verbose(this ILog logger, Exception ex, [InterpolatedStringHandlerArgument("logger")] ref JetLogVerboseInterpolatedStringHandler messageHandler)
    {
      if (messageHandler.IsEnabled)
      {
        logger.Verbose(ex, messageHandler.ToStringAndClear());
      }
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1>(this ILog @this, string message, T1 t1)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1, T2>(this ILog @this, string message, T1 t1, T2 t2)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1, t2);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1, T2, T3>(this ILog @this, string message, T1 t1, T2 t2, T3 t3)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1, t2, t3);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1, T2, T3, T4>(this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1, t2, t3, t4);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1, T2, T3, T4, T5>(this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1, t2, t3, t4, t5);
    }

    [StringFormatMethod("message")]
    public static void Verbose<T1, T2, T3, T4, T5, T6>(this ILog @this, string message, T1 t1, T2 t2, T3 t3, T4 t4, T5 t5, T6 t6)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, t1, t2, t3, t4, t5, t6);
    }

    [StringFormatMethod("message")]
    public static void Verbose(this ILog @this, string message, object[] args)
    {
      @this.LogFormat(LoggingLevel.VERBOSE, message, args);
    }

    public static void Verbose(this ILog @this, Exception ex, string? message = null)
    {
      @this.Log(LoggingLevel.VERBOSE, message, ex);
    }

    #endregion
    #region Info

    public static void Info(this ILog @this, string message)
    {
      @this.Log(LoggingLevel.INFO, message);
    }
    
    /// <summary>
    /// Log the message if <see cref="LoggingLevel.INFO"/> is enabled, otherwise the message will not be logged, moreover, no calculations (including method calls) will be performed.
    /// <br />
    /// <br />
    /// For example, the code below
    /// <code>
    /// logger.Info($"{DoSmthSlow()}");
    /// </code>
    /// 
    /// will be compiled into
    /// 
    /// <code>
    /// var handler = new JetLogInfoInterpolatedStringHandler(logger, out var isEnabled);
    /// if (isEnabled)
    ///   handler.Append(DoSmthSlow());
    /// logger.Info(ref handler);
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <param name="messageHandler"></param>
    public static void Info(this ILog logger, [InterpolatedStringHandlerArgument("logger")] ref JetLogInfoInterpolatedStringHandler messageHandler)
    {
      if (messageHandler.IsEnabled)
      {
        logger.Log(LoggingLevel.INFO, messageHandler.ToStringAndClear());
      }      
    }

    /// <summary>
    /// Log the message if <see cref="LoggingLevel.INFO"/> is enabled, otherwise the message will not be logged, moreover, no calculations (including method calls) will be performed.
    /// <br />
    /// <br />
    /// For example, the code below
    /// <code>
    /// logger.Info(ex, $"{DoSmthSlow()}");
    /// </code>
    /// 
    /// will be compiled into
    /// 
    /// <code>
    /// var handler = new JetLogInfoInterpolatedStringHandler(logger, out var isEnabled);
    /// if (isEnabled)
    ///   handler.Append(DoSmthSlow());
    /// logger.Info(ex, ref handler);
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <param name="ex"></param>
    /// <param name="messageHandler"></param>
    public static void Info(this ILog logger, Exception ex, [InterpolatedStringHandlerArgument("logger")] ref JetLogInfoInterpolatedStringHandler messageHandler)
    {
      if (messageHandler.IsEnabled)
      {
        logger.Info(ex, messageHandler.ToStringAndClear());
      }
    }

    [StringFormatMethod("message")]
    public static void Info(this ILog @this, string message, params object[] args)
    {
      @this.LogFormat(LoggingLevel.INFO, message, args);
    }

    public static void Info(this ILog @this, Exception ex, string? message = null)
    {
      @this.Log(LoggingLevel.INFO, message, ex);
    }


    #endregion
    #region Warn

    public static void Warn(this ILog @this, string message)
    {
      @this.Log(LoggingLevel.WARN, message);
    }
    
    /// <summary>
    /// Log the message if <see cref="LoggingLevel.WARN"/> is enabled, otherwise the message will not be logged, moreover, no calculations (including method calls) will be performed.
    /// <br />
    /// <br />
    /// For example, the code below
    /// <code>
    /// logger.Warn($"{DoSmthSlow()}");
    /// </code>
    /// 
    /// will be compiled into
    /// 
    /// <code>
    /// var handler = new JetLogWarnInterpolatedStringHandler(logger, out var isEnabled);
    /// if (isEnabled)
    ///   handler.Append(DoSmthSlow());
    /// logger.Warn(ref handler);
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <param name="messageHandler"></param>
    public static void Warn(this ILog logger, [InterpolatedStringHandlerArgument("logger")] ref JetLogWarnInterpolatedStringHandler messageHandler)
    {
      if (messageHandler.IsEnabled)
      {
        logger.Log(LoggingLevel.WARN, messageHandler.ToStringAndClear());
      }      
    }

    /// Log the message if <see cref="LoggingLevel.WARN"/> is enabled, otherwise the message will not be logged, moreover, no calculations (including method calls) will be performed.
    /// <br />
    /// <br />
    /// For example, the code below
    /// <code>
    /// logger.Warn(ex, $"{DoSmthSlow()}");
    /// </code>
    /// 
    /// will be compiled into
    /// 
    /// <code>
    /// var handler = new JetLogWarnInterpolatedStringHandler(logger, out var isEnabled);
    /// if (isEnabled)
    ///   handler.Append(DoSmthSlow());
    /// logger.Warn(ex, ref handler);
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <param name="ex"></param>
    /// <param name="messageHandler"></param>
    public static void Warn(this ILog logger, Exception ex, [InterpolatedStringHandlerArgument("logger")] ref JetLogWarnInterpolatedStringHandler messageHandler)
    {
      if (messageHandler.IsEnabled)
      {
        logger.Warn(ex, messageHandler.ToStringAndClear());
      }
    }


    [StringFormatMethod("message")]
    public static void Warn(this ILog @this, string message, params object[] args)
    {
      @this.LogFormat(LoggingLevel.WARN, message, args);
    }

    public static void Warn(this ILog @this, Exception ex, string? message = null)
    {
      @this.Log(LoggingLevel.WARN, message, ex);
    }


    #endregion
    #region Error

    [StringFormatMethod("message")]
    public static void Error(this ILog @this, string message)
    {
      @this.Log(LoggingLevel.ERROR, message);
    }
    
    /// <summary>
    /// Log the message if <see cref="LoggingLevel.ERROR"/> is enabled, otherwise the message will not be logged, moreover, no calculations (including method calls) will be performed.
    /// <br />
    /// <br />
    /// For example, the code below
    /// <code>
    /// logger.Error($"{DoSmthSlow()}");
    /// </code>
    /// 
    /// will be compiled into
    /// 
    /// <code>
    /// var handler = new JetLogErrorInterpolatedStringHandler(logger, out var isEnabled);
    /// if (isEnabled)
    ///   handler.Append(DoSmthSlow());
    /// logger.Error(ref handler);
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <param name="messageHandler"></param>
    public static void Error(this ILog logger, [InterpolatedStringHandlerArgument("logger")] ref JetLogErrorInterpolatedStringHandler messageHandler)
    {
      if (messageHandler.IsEnabled)
      {
        logger.Log(LoggingLevel.ERROR, messageHandler.ToStringAndClear());
      }      
    }

    /// <summary>
    /// Log the message if <see cref="LoggingLevel.ERROR"/> is enabled, otherwise the message will not be logged, moreover, no calculations (including method calls) will be performed.
    /// <br />
    /// <br />
    /// For example, the code below
    /// <code>
    /// logger.Error(ex, $"{DoSmthSlow()}");
    /// </code>
    /// 
    /// will be compiled into
    /// 
    /// <code>
    /// var handler = new JetLogErrorInterpolatedStringHandler(logger, out var isEnabled);
    /// if (isEnabled)
    ///   handler.Append(DoSmthSlow());
    /// logger.Error(ex, ref handler);
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <param name="ex"></param>
    /// <param name="messageHandler"></param>
    [StringFormatMethod("message")]
    public static void Error(this ILog logger, Exception ex, [InterpolatedStringHandlerArgument("logger")] ref JetLogErrorInterpolatedStringHandler messageHandler)
    {
      if (messageHandler.IsEnabled)
      {
        logger.Error(ex, messageHandler.ToStringAndClear());
      }
    }

    [StringFormatMethod("message")]
    public static void Error(this ILog @this, string message, params object?[] args)
    {
      @this.LogFormat(LoggingLevel.ERROR, message, args);
    }

    [StringFormatMethod("message")]
    public static void Error(this ILog @this, string message, Exception e)
    {
      @this.Log(LoggingLevel.ERROR, message, e);
    }

    [StringFormatMethod("message")]
    public static void Error(this ILog @this, Exception ex, string? message = null)
    {
      @this.Log(LoggingLevel.ERROR, message, ex);
    }

    #endregion
    #region Assert

    public static void Assert(this ILog @this, bool condition, [CallerArgumentExpression("condition")] string? message = null)
    {
      if (!condition)
      {
        @this.Error(message ?? "");
      }
    }

    public static void Assert<T>(this ILog @this, bool condition, string message, T t1)
    {
      if (!condition)
      {
        @this.Error(message, t1);
      }
    }

    public static void Assert(this ILog @this, bool condition, string message, params object[] args)
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
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
    public static void Catch(this ILog log, [InstantHandle] Action action)
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
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
    [PublicAPI] public static T? Catch<T>(this ILog log, [InstantHandle] Func<T> action)
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
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
    [PublicAPI] public static void CatchAndDrop(this ILog log, [InstantHandle] Action action)
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
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
    [PublicAPI] public static T? CatchAndDrop<T>(this ILog log, [InstantHandle] Func<T> action)
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
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
    [PublicAPI] public static void CatchWarn(this ILog log, [InstantHandle] Action action)
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
    [System.Runtime.ExceptionServices.HandleProcessCorruptedStateExceptions]
    [PublicAPI] public static T? CatchWarn<T>(this ILog log, [InstantHandle] Func<T> action)
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
    private static string FormatEx(this string s, params object?[] p)
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
    ///   logger.WhenTrace()?.Log($"some messages with {HeavyComputation()}")
    /// </code>
    /// </summary>
    /// <param name="logger"/>
    /// <returns>struct <see cref="LogWithLevel"/>(<paramref name="logger"/>, <see cref="LoggingLevel.TRACE"/>) if <see cref="IsTraceEnabled"/>;
    /// null otherwise</returns>
    [PublicAPI] public static LogWithLevel? WhenTrace(this ILog logger)
    {
      return LogWithLevel.CreateIfEnabled(logger, LoggingLevel.TRACE);
    }

    /// <summary>
    /// One-line shortcut builder to replace common pattern:
    /// <code>
    ///   if (logger.IsVerboseEnabled())
    ///     logger.Verbose($"some messages with {HeavyComputation()}"")
    /// </code>
    /// Usage:
    /// <code>
    ///   logger.WhenVerbose()?.Log($"some messages with {HeavyComputation()}")
    /// </code>
    /// </summary>
    /// <param name="logger"></param>
    /// <returns>struct <see cref="LogWithLevel"/>(<paramref name="logger"/>, <see cref="LoggingLevel.VERBOSE"/>) if <see cref="IsVersboseEnabled"/>;
    /// null otherwise</returns>
    [PublicAPI] public static LogWithLevel? WhenVerbose(this ILog logger)
    {
      return LogWithLevel.CreateIfEnabled(logger, LoggingLevel.VERBOSE);
    }

    [Obsolete("Renamed to WhenTrace")]
    public static LogWithLevel? Trace(this ILog logger) => logger.WhenTrace();

    [Obsolete("Renamed to WhenVerbose")]
    public static LogWithLevel? Verbose(this ILog logger) => logger.WhenVerbose();

    #endregion
  }
}