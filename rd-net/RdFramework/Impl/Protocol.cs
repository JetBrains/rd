using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Collections.Synchronized;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;

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

    public RdEntitiesRegistrar RdEntitiesRegistrar { get; }
    
    private readonly Protocol? myParentProtocol;
    private readonly Dictionary<string, object> myExtensions = new();

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
      RdEntitiesRegistrar = parentProtocol?.RdEntitiesRegistrar ?? new RdEntitiesRegistrar();
      SerializationContext = parentProtocol?.SerializationContext ?? new SerializationCtx(this, new Dictionary<string, IInternRoot<object>>() {{ProtocolInternScopeStringId, CreateProtocolInternRoot(lifetime)}});
      Contexts = parentProtocol?.Contexts ?? new ProtocolContexts(SerializationContext);
      wire.Contexts = Contexts;
      if (parentProtocol?.SerializationContext == null)
        SerializationContext.InternRoots[ProtocolInternScopeStringId].BindTopLevel(lifetime, this, ProtocolInternRootRdId);
      foreach (var rdContextBase in initialContexts) rdContextBase.RegisterOn(Contexts);
      if (parentProtocol?.Contexts == null)
        BindContexts(lifetime);
      OutOfSyncModels = new ViewableSet<RdExtBase>();
      ExtCreated = parentProtocol?.ExtCreated ?? new Signal<ExtCreationInfoEx>();
      ExtConfirmation = parentExtConfirmation ?? this.CreateExtSignal(identities);
      ExtIsLocal = new ThreadLocal<bool>(() => false);
      ExtConfirmation.Advise(lifetime, message =>
      {
        ExtCreated.Fire(new ExtCreationInfoEx(message, ExtIsLocal.Value));
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
      root.RdId = Identities.Mix(RdId.Nil, ProtocolInternRootRdId);
      
      return root;
    }

    private void BindContexts(Lifetime lifetime)
    {
      Contexts.RdId = Identities.Mix(RdId.Nil, ContextHandlerRdId);
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
    
    public ISignal<ExtCreationInfoEx> ExtCreated { get; }
    
    private RdSignal<ExtCreationInfo> ExtConfirmation { get; }

    private ThreadLocal<bool> ExtIsLocal { get; }

    [PublicAPI] public bool ThrowErrorOnOutOfSyncModels = true;
    
    
    public RName Location { get; }
    IProtocol IRdDynamic.TryGetProto() => this;

    public virtual T? GetExtension<T>() where T : RdExtBase
    {
      var parentProtocol = myParentProtocol;
      if (parentProtocol != null)
        return parentProtocol.GetExtension<T>();
      
      lock (myExtensions)
      {
        return myExtensions.TryGetValue(typeof(T).Name, out var extension) ? (T)extension : default;
      }
    }
    
    public virtual T GetOrCreateExtension<T>(Func<T> create) where T : RdExtBase
    {
      if (create == null) throw new ArgumentNullException(nameof(create));
      
      var parentProtocol = myParentProtocol;
      if (parentProtocol != null)
        return parentProtocol.GetOrCreateExtension(create);

      lock (myExtensions)
      {
        var name = typeof(T).Name;
        if (myExtensions.TryGetValue(name, out var existing))
        {
          var val = existing.NotNull("Found null value for key: '{0}'", name) as T;
          Assertion.Require(val != null, $"Found bad value for key '{name}'. Expected type: '{typeof(T).FullName}', actual:'{existing.GetType().FullName}");
          return val;
        }

        var res = create().NotNull("'Create' result must not be null");

        myExtensions[name] = res;
        if (res is IRdBindable rdBindable)
        {
          rdBindable.Identify(Identities, Identities.Mix(RdId.Root, name));
          rdBindable.PreBind(Lifetime, this, name);
          rdBindable.Bind();
        }

        return res;
      }
    }
  }
}