using System;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Impl;

namespace JetBrains.Rd
{
  public interface IProtocol : IRdDynamic
  {
    Lifetime Lifetime { get; }
    SerializationCtx SerializationContext { get; }
    ViewableSet<RdExtBase> OutOfSyncModels { get; }
    string Name { get; }
    ISerializers Serializers { get; }
    IIdentities Identities { get; }
    IScheduler Scheduler { get; }
    IWire Wire { get; }    
    ProtocolContexts Contexts { get; }
    ISignal<ExtCreationInfoEx> ExtCreated { get; }
    
    public T? GetExtension<T>() where T : RdExtBase;
    public T GetOrCreateExtension<T>(Func<T> create) where T : RdExtBase;
  }
}
