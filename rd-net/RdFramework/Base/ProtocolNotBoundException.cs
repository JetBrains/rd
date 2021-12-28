using System;

namespace JetBrains.Rd.Base;

public class ProtocolNotBoundException : Exception
{
  public ProtocolNotBoundException(string id) : base($"{id} is not bound to a protocol")
  {
      
  }
}