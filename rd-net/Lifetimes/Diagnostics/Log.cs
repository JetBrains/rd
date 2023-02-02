using System;
using System.IO;
using System.Runtime.CompilerServices;
using System.Text;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics.Internal;

using JetBrains.Lifetimes;
using JetBrains.Threading;
using JetBrains.Util;
using JetBrains.Util.Util;

namespace JetBrains.Diagnostics
{
  /// <summary>
  /// Logger configuration entry point. 
  /// If you want see logs from this library to bound in your solution
  /// you have to bound <see cref="ILog"/> to some logger implementation library (backend) used in your code base (say log4net)
  /// by implementing <see cref="ILog"/> and <see cref="ILogFactory"/> and setting them as default by <see cref="set_DefaultFactory"/>. 
  /// </summary>
  public class Log
  {

    #region Set factory settings via Statics helpers

    private static readonly StaticsForType<ILogFactory> ourStatics = Statics.For<ILogFactory>();
    private static volatile ILogFactory ourCurrentFactory;

    
    /// <summary>
    /// Default (lowest priority) <see cref="ILogFactory"/>. If nothing chosen <see cref="ConsoleVerboseFactory"/> is used.
    /// This setting is effectively overriden (in a stack-like way) by <see cref="UsingLogFactory"/>
    /// </summary>
    public static ILogFactory DefaultFactory
    {
      get => ourStatics.PeekFirst() ?? ConsoleVerboseFactory;

      set => ourStatics.ReplaceFirst(value);
    }

    static Log()
    {
      ourStatics.AddFirst(ConsoleVerboseFactory);
      ourCurrentFactory = ConsoleVerboseFactory; //not necessary, just to suppress warning
      
      ourStatics.ForEachValue(() =>
      {
        ourCurrentFactory = ourStatics.PeekLast() ?? ConsoleVerboseFactory;
      });
      
    }

    private class LogFactoryCookie : IDisposable
    {
      private readonly ILogFactory myFactory;

      internal LogFactoryCookie(ILogFactory factory)
      {
        myFactory = factory;
        ourStatics.AddLast(factory);
      }

      void IDisposable.Dispose()
      {
        ourStatics.RemoveLastReferenceEqual(myFactory, true);
      }
    }

    /// <summary>
    /// Use this method if you want to set your global log factory (push it to the top of the stack).  
    /// </summary>
    /// <param name="factory">Factory to use as global  util returned object is not disposed</param>
    /// <returns>IDisposable, that should be disposed to return old logger factory. </returns>
    /// <exception cref="ArgumentNullException"></exception>
    public static IDisposable UsingLogFactory(ILogFactory factory)
    {
      if (factory == null) throw new ArgumentNullException(nameof(factory));

      return new LogFactoryCookie(factory);
    }

    #endregion



    #region API

    public static readonly ILog Root = GetLog("");

    /// <summary>
    /// Creates log for <see cref="category"/>. Dots ('.') are separators between subcategories so all loggers form a hierarchy tree.
    /// </summary>
    /// <param name="category"></param>
    /// <returns></returns>
    public static ILog GetLog(string category)
    {
      return new SwitchingLog(category);
    }

    /// <summary>
    /// Creates logger for FQN of <paramref name="type"/>
    /// </summary>
    /// <returns></returns>
    public static ILog GetLog(Type type)
    {
      return GetLog(type.ToString(withNamespaces:true, withGenericArguments:false));
    }

    /// <summary>
    /// Creates logger for FQN of type <see cref="T"/>
    /// </summary>
    /// <typeparam name="T"></typeparam>
    /// <returns></returns>
    public static ILog GetLog<T>()
    {
      return GetLog(typeof(T));
    }

    #endregion


    #region Switching log
    /// <summary>
    /// This class is used automatically as wrapper when you log something. On each log event it checks whether underlying <see cref="myFactory"/>
    /// implementation is switched. So you can substitute new log factories by <see cref="Log.UsingLogFactory"/> and every existing <see cref="ILog"/>
    /// will be reconfigured on the fly. 
    /// </summary>
    private class SwitchingLog : ILog
    {

      private readonly string myCategory;

      private 
        // todo volatile : think about it when we target ARM  
        ILogFactory? myFactory;

      private ILog? myLog;

      public SwitchingLog(string category)
      {
        myCategory = category;
      }

      public string Category => CheckImplementationSwitched().Category;

      bool ILog.IsEnabled(LoggingLevel level)
      {
        return CheckImplementationSwitched().IsEnabled(level);
      }

      void ILog.Log(LoggingLevel level, string? message, Exception? exception)
      {
        CheckImplementationSwitched().Log(level, message, exception);
      }

      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      private ILog CheckImplementationSwitched()
      {
        if (myFactory == ourCurrentFactory) return myLog!;

        myLog = ourCurrentFactory.GetLog(myCategory).NotNull("Logger mustn't be null!");
        myFactory = ourCurrentFactory;
        return myLog;
      }
    }
    #endregion


    #region Static consts & helpers

    // ReSharper disable once RedundantArgumentDefaultValue
    public static readonly ILogFactory ConsoleVerboseFactory = new TextWriterLogFactory(Console.Out, LoggingLevel.VERBOSE);

    /// <summary>
    /// Default format for <see cref="DateTime"/> of logging event.
    /// WARNING!!! don't change it, we have code that do fast formatting according this pattern
    /// </summary>
    public const string DefaultDateFormat = "HH:mm:ss.fff";

    /// <summary>
    /// Default formatting of logging message
    /// </summary>
    /// <param name="date"></param>
    /// <param name="loggingLevel"></param>
    /// <param name="category"></param>
    /// <param name="thread"></param>
    /// <param name="message"></param>
    /// <param name="exception"></param>
    /// <returns></returns>
    public static string DefaultFormat(
      DateTime? date,
      LoggingLevel loggingLevel,
      string? category,
      Thread? thread,
      string? message,
      Exception? exception = null
    )
    {
      var builder = new StringBuilder();

      if (date != null)
      {
        builder.Append(date.Value.ToString(DefaultDateFormat));
        builder.Append(" |");
      }

      //Level is always present in result string. Pipes around shouldn't be separates by spaces.
      builder.Append(loggingLevel.ToString().ToUpperInvariant()[0]);
      builder.Append("| ");

      if (category != null)
      {
        builder.Append(category.PadRight(30));
        builder.Append(" | ");
      }

      if (thread != null)
      {
        builder.Append(thread.ToThreadString().PadRight(30));
        builder.Append(" | ");
      }

      if (message != null)
      {
        builder.Append(message);
        if (exception != null) builder.Append(" :: ");
      }

      if (exception != null)
      {
        builder.Append(exception);
      }

      builder.Append(Environment.NewLine);

      return builder.ToString();
    }


    /// <summary>
    /// Creates <see cref="ILogFactory"/> that opens file writer. All log messages will be in <see cref="DefaultFormat"/>  
    /// </summary>
    /// <param name="lifetime">lifetime of file writer; after lifetime termination file writer will be closed</param>
    /// <param name="path">path to the file</param>
    /// <param name="append">append or rewrite file</param>
    /// <param name="enabledLevel">Filter out all messages with <see cref="LoggingLevel"/> more than this. I.e. enabledLevel == INFO will filter out VERBOSE and TRACE.</param>
    /// <returns>created factory</returns>
    /// <exception cref="Exception"></exception>
    /// <exception cref="IOException"></exception>
    public static TextWriterLogFactory CreateFileLogFactory(Lifetime lifetime, string path, bool append = false, LoggingLevel enabledLevel = LoggingLevel.VERBOSE)
    {      
      Assertion.Require(lifetime.IsAlive, "lifetime.IsTerminated");
      
      var directory = Path.GetDirectoryName(path);
      if (directory == null)
      {
        throw new Exception($"Couldn't extract directory from path: [{path}]");
      }
      try
      {
        Directory.CreateDirectory(directory);
      }
      catch (Exception ex)
      {
        throw new IOException($"Couldn't create directory: [{directory}]", ex);
      }
      try
      {
        var fileStream = new FileStream(path, append ? FileMode.Append : FileMode.Create, FileAccess.Write,
          FileShare.ReadWrite);

        var writer = new StreamWriter(fileStream, Encoding.UTF8);
        lifetime.OnTermination(writer);

        return new TextWriterLogFactory(TextWriter.Synchronized(writer), enabledLevel);
      }
      catch (Exception ex)
      {
        throw new IOException($"Couldn't open or create log file: [{path}]", ex);
      }
    }

    #endregion
  }
}