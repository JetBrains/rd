using System;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
  public class Protocol : IProtocol
  {
    public static readonly ILog TraceLogger = Log.GetLog("protocol");
    public static readonly ILog InitializationLogger = TraceLogger.GetSublogger("INIT");

    /// <summary>
    /// Should match textual RdId of protocol intern root in Kotlin/js/cpp counterpart
    /// </summary>
    const string ProtocolInternRootRdId = "ProtocolInternRoot";
    const string ClientIdSetRdId = "ProtocolClientIdSet";
    
    /// <summary>
    /// Should match whatever is in rd-gen for ProtocolInternScope
    /// </summary>
    const string ProtocolInternScopeStringId = "Protocol";
    

    public Protocol([NotNull] string name, [NotNull] ISerializers serializers, [NotNull] IIdentities identities, [NotNull] IScheduler scheduler,
      [NotNull] IWire wire, Lifetime lifetime, SerializationCtx? serializationCtx = null, [CanBeNull] RdSet<ClientId> parentClientIdSet = null)
    {
      
      Name = name ?? throw new ArgumentNullException(nameof(name));
      Location = new RName(name);

      Serializers = serializers ?? throw new ArgumentNullException(nameof(serializers));
      Identities = identities ?? throw new ArgumentNullException(nameof(identities));
      Scheduler = scheduler ?? throw new ArgumentNullException(nameof(scheduler));
      Wire = wire ?? throw new ArgumentNullException(nameof(wire));
      ClientIdSet = parentClientIdSet ?? new RdSet<ClientId>(ClientId.ReadDelegate, ClientId.WriteDelegate);
      SerializationContext = serializationCtx ?? new SerializationCtx(this, new Dictionary<string, IInternRoot>() {{ProtocolInternScopeStringId, CreateProtocolInternRoot(lifetime)}});
      OutOfSyncModels = new ViewableSet<RdExtBase>();
    }

    private InternRoot CreateProtocolInternRoot(Lifetime lifetime)
    {
      var root = new InternRoot();
      root.RdId = RdId.Nil.Mix(ProtocolInternRootRdId);
      ClientIdSet.RdId = RdId.Nil.Mix(ClientIdSetRdId);
      Scheduler.Queue(() =>
      {
        if (!lifetime.IsAlive) return;
        root.Bind(lifetime, this, ProtocolInternRootRdId);
        ClientIdSet.Bind(lifetime, this, ClientIdSetRdId);
      });
      return root;
    }
      
    public string Name { get; }
    
    public IWire Wire { get; }
    public ISerializers Serializers { get; }
    public IIdentities Identities { get; }
    public IScheduler Scheduler { get; }
    public SerializationCtx SerializationContext { get; }
    public ViewableSet<RdExtBase> OutOfSyncModels { get; }

    public RdSet<ClientId> ClientIdSet { get; }

    [PublicAPI] public bool ThrowErrorOnOutOfSyncModels = true;
    
    
    public RName Location { get; }
    IProtocol IRdDynamic.Proto => this;

  }
}