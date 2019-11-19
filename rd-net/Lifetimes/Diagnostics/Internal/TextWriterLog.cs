using System;
using System.IO;
using System.Threading;
using JetBrains.Annotations;

namespace JetBrains.Diagnostics.Internal
{
  /// <summary>
  /// Log that able to write to <see cref="TextWriter"/>
  /// </summary>
  public class TextWriterLog : LogBase
  {
    [PublicAPI] public TextWriter Writer { get; }

    protected override string Format(LoggingLevel level, string message, Exception exception)
    {
      return Diagnostics.Log.DefaultFormat(DateTime.Now, level, Category, Thread.CurrentThread, message, exception);      
    }
               
    public TextWriterLog([NotNull] TextWriter writer, [NotNull] string category, LoggingLevel enabledLevel = LoggingLevel.VERBOSE) : base(category, enabledLevel)
    {
      Writer = TextWriter.Synchronized(writer ?? throw new ArgumentNullException(nameof(writer)));
      Handlers += WriteMessage;
    }

    private void WriteMessage(LeveledMessage msg)
    {
      Writer.Write(msg.FormattedMessage);
      Writer.Flush();
    }
  }
  

  /// <summary>
  /// Log factory that create <see cref="TextWriterLog"/>. Could be created for file by <see cref="Log.CreateFileLogFactory"/>
  /// </summary>
  public class TextWriterLogFactory : LogFactoryBase
  {
    public LoggingLevel EnabledLevel { get; }
    public TextWriter Writer { get; }
    
    public TextWriterLogFactory([NotNull] TextWriter writer, LoggingLevel enabledLevel = LoggingLevel.VERBOSE)
    {
      EnabledLevel = enabledLevel;
      Writer = writer ?? throw new ArgumentNullException(nameof(writer));
    }

    protected override LogBase GetLogBase(string category)
    {
      return new TextWriterLog(Writer, category, EnabledLevel);
    }
  }
}