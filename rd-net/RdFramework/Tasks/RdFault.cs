using System;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Serialization;

namespace JetBrains.Rd.Tasks
{
  [PublicAPI] 
  [Serializable] 
  public class RdFault : Exception
  {
    public string ReasonTypeFqn { get; private set; }
    public string ReasonText { get; private set; }
    public string ReasonMessage { get; private set; }

    public RdFault(Exception inner) : base(inner.Message, inner)
    {
      ReasonTypeFqn = inner.GetType().FullName;
      ReasonMessage = inner.Message;
      ReasonText = inner.ToString(); //todo Use system capabilities, stack traces, etc
    }

    
    
    public RdFault(string reasonTypeFqn, string reasonMessage, string reasonText, Exception? reason = null) 
      : base(reasonMessage + (reason == null ? ", reason: " + reasonText : ""), reason)
    {
      ReasonTypeFqn = reasonTypeFqn;
      ReasonMessage = reasonMessage;
      ReasonText = reasonText;
    }
    
    
    public static RdFault Read(SerializationCtx ctx, UnsafeReader reader)
    {
      var typeFqn = reader.ReadString().NotNull("typeFqn");
      var message = reader.ReadString().NotNull("message");
      var body = reader.ReadString().NotNull("body");
      
      return new RdFault(typeFqn, message, body);
    }

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdFault value)
    {
      writer.Write(value.ReasonTypeFqn);
      writer.Write(value.ReasonMessage);
      writer.Write(value.ReasonText);
    }
    
  }
}