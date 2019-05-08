using System;
using System.IO;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Util;

namespace JetBrains.Diagnostics.Internal
{
  public class TextWriterLog : LogBase
  {
    public TextWriter Writer { get; }

    protected override string Format(LoggingLevel level, string message, Exception exception)
    {
      return Diagnostics.Log.DefaultFormat(DateTime.Now, level, Category, Thread.CurrentThread, message, exception);      
    }
               
    public TextWriterLog([NotNull] TextWriter writer, [NotNull] string category, LoggingLevel enabledLevel = LoggingLevel.VERBOSE) : base(category, enabledLevel)
    {
      Writer = writer ?? throw new ArgumentNullException(nameof(writer));
      Handlers += WriteMessage;
    }

    private void WriteMessage(LeveledMessage msg)
    {
      lock (Writer) //Can't use TextWriter.Synchronized in NetCore 1.1
      {
        Writer.Write(msg.FormattedMessage);
        Writer.Flush();
      }
    }
  }
  

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