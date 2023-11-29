using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Collections.Synchronized;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;

namespace JetBrains.Rd.Impl
{
  public class Protocol : IProtocol
  {
    public static readonly ILog Logger = Log.GetLog("protocol");
    public static readonly ILog InitLogger = Logger.GetSublogger("INIT");
    public static LogWithLevel? InitTrace = InitLogger.WhenTrace();

    /// <summary>
    /// Should match textual RdId of protocol intern root in Kotlin/js/cpp counterpart
    /// </summary>
    const string ProtocolInternRootRdId = "ProtocolInternRoot";
    const string ContextHandlerRdId = "ProtocolContextHandler";
    internal const string ProtocolExtCreatedRdId = "ProtocolExtCreated";
    
    /// <summary>
    /// Should match whatever is in rd-gen for ProtocolInternScope
    /// </summary>
    const string ProtocolInternScopeStringId = "Protocol";

    public Lifetime Lifetime { get; }

    public DynamicContainer DynamicContainer { get; }
    
    private readonly Protocol? myParentProtocol;

    public Protocol(string name, ISerializers serializers, IIdentities identities, IScheduler scheduler, 
      IWire wire, Lifetime lifetime, params RdContextBase[] initialContexts) 
      : this(name, serializers, identities, scheduler, wire, lifetime, null, null, initialContexts)
    { }

    internal Protocol(string name, ISerializers serializers, IIdentities identities, IScheduler scheduler,
      IWire wire, Lifetime lifetime, Protocol? parentProtocol, RdSignal<ExtCreationInfo>? parentExtConfirmation = null, params RdContextBase[] initialContexts)
    {
      Lifetime = lifetime;
      Name = name ?? throw new ArgumentNullException(nameof(name));
      Location = new RName(name);

      Serializers = serializers ?? throw new ArgumentNullException(nameof(serializers));
      Identities = identities ?? throw new ArgumentNullException(nameof(identities));
      Scheduler = scheduler ?? throw new ArgumentNullException(nameof(scheduler));
      Wire = wire ?? throw new ArgumentNullException(nameof(wire));
      myParentProtocol = parentProtocol;
      DynamicContainer = parentProtocol?.DynamicContainer ?? new DynamicContainer();
      SerializationContext = parentProtocol?.SerializationContext ?? new SerializationCtx(this, new Dictionary<string, IInternRoot<object>>() {{ProtocolInternScopeStringId, CreateProtocolInternRoot(lifetime)}});
      Contexts = parentProtocol?.Contexts ?? new ProtocolContexts(SerializationContext);
      wire.Contexts = Contexts;
      if (parentProtocol?.SerializationContext == null)
        SerializationContext.InternRoots[ProtocolInternScopeStringId].BindTopLevel(lifetime, this, ProtocolInternRootRdId);
      foreach (var rdContextBase in initialContexts) rdContextBase.RegisterOn(Contexts);
      if (parentProtocol?.Contexts == null)
        BindContexts(lifetime);
      OutOfSyncModels = new ViewableSet<RdExtBase>();
      ExtCreated = parentProtocol?.ExtCreated ?? new Signal<ExtCreationInfo>();
      ExtConfirmation = parentExtConfirmation ?? this.CreateExtSignal();
      ExtIsLocal = new ThreadLocal<bool>(() => false);
      ExtConfirmation.Advise(lifetime, message =>
      {
        if (ExtIsLocal.Value) return;
        ExtCreated.Fire(message);
      });
      using (AllowBindCookie.Create()) 
        ExtConfirmation.BindTopLevel(lifetime, this, ProtocolExtCreatedRdId);

      if (wire is IWireWithDelayedDelivery wireWithMessageBroker)
        wireWithMessageBroker.StartDeliveringMessages();
    }

    public bool TryGetSerializationContext(out SerializationCtx ctx)
    {
      ctx = SerializationContext;
      return true;
    }

    private InternRoot<object> CreateProtocolInternRoot(Lifetime lifetime)
    {
      var root = new InternRoot<object>();
      root.RdId = RdId.Nil.Mix(ProtocolInternRootRdId);
      
      return root;
    }

    private void BindContexts(Lifetime lifetime)
    {
      Contexts.RdId = RdId.Nil.Mix(ContextHandlerRdId);
      using (AllowBindCookie.Create()) 
      {
        Contexts.PreBind(lifetime, this, ContextHandlerRdId);
        Contexts.Bind();
      }
    }
    
    internal void SubmitExtCreated(ExtCreationInfo info)
    {
      Assertion.Assert(!ExtIsLocal.Value, "!ExtIsLocal");
      ExtIsLocal.Value = true;
      try
      {
        ExtConfirmation.Fire(info);
      }
      finally
      {
        ExtIsLocal.Value = false;
      }
    }
      
    public string Name { get; }
    
    public IWire Wire { get; }
    public ISerializers Serializers { get; }
    public IIdentities Identities { get; }
    public IScheduler Scheduler { get; }
    public SerializationCtx SerializationContext { get; }
    public ViewableSet<RdExtBase> OutOfSyncModels { get; }

    public ProtocolContexts Contexts { get; }
    
    public ISignal<ExtCreationInfo> ExtCreated { get; }
    
    private RdSignal<ExtCreationInfo> ExtConfirmation { get; }

    private ThreadLocal<bool> ExtIsLocal { get; }

    [PublicAPI] public bool ThrowErrorOnOutOfSyncModels = true;
    
    
    public RName Location { get; }
    IProtocol IRdDynamic.TryGetProto() => this;
  }

  public class DynamicContainer
  {
    private readonly Dictionary<RdId, IRdDynamic> myMap = new();

    public void Register(Lifetime lifetime, RdId rdId, IRdDynamic dynamic)
    {
      Assertion.Assert(!rdId.IsNil);
      
      myMap.BlockingAddUnique(lifetime, myMap, rdId, dynamic);
    }

    public bool TryGetDynamic(RdId rdId, out IRdDynamic dynamic)
    {
      lock (myMap)
      {
        return myMap.TryGetValue(rdId, out dynamic);
      }
    }
  }
}