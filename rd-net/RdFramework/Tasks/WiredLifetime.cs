using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;
using JetBrains.Serialization;

namespace JetBrains.Rd.Tasks
{
  public class WiredLifetime : IRdWireable
  {
    private readonly LifetimeDefinition myDefinition;
    private readonly IRdReactive myParent;
    public RdId RdId { get; }
    public IScheduler WireScheduler { get; }
    
    
    public WiredLifetime(LifetimeDefinition definition, RdId rdId, IRdReactive parent, IWire wire)
    {
      myDefinition = definition;
      myParent = parent;
      RdId = rdId;
      WireScheduler = parent.WireScheduler; //maybe cancellation better to do synchronous
      
      wire.Advise(definition.Lifetime, this);
    }
    
    public void OnWireReceived(UnsafeReader reader)
    {
      if (RdReactiveBase.LogReceived.IsTraceEnabled())
        RdReactiveBase.LogReceived.Trace($"{myParent.Location}.{RdId} received cancellation");
      
      reader.ReadVoid(); //nothing just a void signal
      myDefinition.Terminate();
    }
  }
}