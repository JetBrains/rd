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
  public class Log
  {

    #region Set factory settings via Statics helpers

    private static readonly StaticsForType<ILogFactory> ourStatics = Statics.For<ILogFactory>();
    [NotNull]
    private static volatile ILogFactory ourCurrentFactory;

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

    public static IDisposable UsingLogFactory([NotNull]ILogFactory factory)
    {
      if (factory == null) throw new ArgumentNullException(nameof(factory));

      return new LogFactoryCookie(factory);
    }

    #endregion



    #region API

    public static readonly ILog Root = GetLog("");

    public static ILog GetLog([NotNull]string category)
    {
      return new SwitchingLog(category);
    }

    public static ILog GetLog(Type type)
    {
      return GetLog(type.ToString(withNamespaces:true, withGenericArguments:false));
    }

    public static ILog GetLog<T>()
    {
      return GetLog(typeof(T));
    }

    #endregion


    #region Switching log
    private class SwitchingLog : ILog
    {

      private readonly string myCategory;

      private 
        // todo volatile : think about it when we target ARM  
        ILogFactory myFactory;

      private ILog myLog;

      public SwitchingLog(string category)
      {
        myCategory = category;
      }

      public string Category
      {
        get
        {
          CheckImplementationSwitched();
          return myLog.Category;
        }
      }

      bool ILog.IsEnabled(LoggingLevel level)
      {
        CheckImplementationSwitched();
        return myLog.IsEnabled(level);
      }

      void ILog.Log(LoggingLevel level, string message, Exception exception)
      {
        CheckImplementationSwitched();
        myLog.Log(level, message, exception);
      }

      [MethodImpl(MethodImplAdvancedOptions.AggressiveInlining)]
      private void CheckImplementationSwitched()
      {
        if (myFactory == ourCurrentFactory) return;

        myLog = ourCurrentFactory.GetLog(myCategory).NotNull("Logger mustn't be null!");
        myFactory = ourCurrentFactory;
      }
    }
    #endregion


    #region Static consts & helpers

    // ReSharper disable once RedundantArgumentDefaultValue
    public static readonly ILogFactory ConsoleVerboseFactory = new TextWriterLogFactory(Console.Out, LoggingLevel.VERBOSE);

    public const string DefaultDateFormat = "HH:mm:ss.fff"; //don't change it, we have code that do fast formatting according this pattern

    public static string DefaultFormat(
      [CanBeNull] DateTime? date,
      LoggingLevel loggingLevel,
      [CanBeNull] string category,
      [CanBeNull] Thread thread,
      [CanBeNull] string message,
      [CanBeNull] Exception exception = null
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


    public static TextWriterLogFactory CreateFileLogFactory([NotNull]Lifetime lifetime, [NotNull]string path, bool append = false, LoggingLevel enabledLevel = LoggingLevel.VERBOSE)
    {      
      Assertion.Assert(lifetime.IsAlive, "lifetime.IsTerminated");
      
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

        return new TextWriterLogFactory(writer, enabledLevel);
      }
      catch (Exception ex)
      {
        throw new IOException($"Couldn't open or create log file: [{path}]", ex);
      }
    }

    #endregion
  }
}