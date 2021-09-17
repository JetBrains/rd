using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;

namespace JetBrains.Rd
{
  public interface IProtocol : IRdDynamic
  {
    ViewableSet<RdExtBase> OutOfSyncModels { get; }
    string Name { get; }
    ISerializers Serializers { get; }
    IIdentities Identities { get; }
    IScheduler Scheduler { get; }
    IWire Wire { get; }    
    ProtocolContexts Contexts { get; }
    ISignal<ExtCreationInfo> ExtCreated { get; }
  }
}
