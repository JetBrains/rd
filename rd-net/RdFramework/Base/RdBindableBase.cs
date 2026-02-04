using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Util;
using JetBrains.Util.Internal;
using JetBrains.Util.Util;

namespace JetBrains.Rd.Base
{
  public abstract class RdBindableBase : IRdBindable, INotifyPropertyChanged, ITerminationHandler
  {
    #region Bound state: main
    
    public RdId RdId { get; set; }

    internal static readonly RName NotBound = new RName("<<not bound>>");
    public RName Location { get; private set; } = NotBound;   
    
    protected IRdDynamic? Parent;
    
    private Lifetime myBindLifetime = Lifetime.Terminated;
    
    #endregion
        
    
    
    #region Bound state: inferred
    
    public bool IsBound => BindState == BindState.Bound;

    public BindState BindState { get; private set; } = BindState.NotBound;

    public virtual IProtocol? TryGetProto() => Parent?.TryGetProto();

    public virtual bool TryGetSerializationContext(out SerializationCtx ctx)
    {
      var parent = Parent;
      if (parent != null)
        return parent.TryGetSerializationContext(out ctx);

      ctx = default;
      return default;
    }

    public RdExtBase? ContainingExt
    {
      get
      {
        IRdDynamic? cur = this;
        while (cur is RdBindableBase bindable)
        {
          if (cur is RdExtBase ext) return ext;
          cur = bindable.Parent;
        }
        return null;
      }
    }

    #endregion
    

    
    protected readonly List<KeyValuePair<string, object>> BindableChildren = new List<KeyValuePair<string, object>>();  

    
    public void PreBind(Lifetime lf, IRdDynamic parent, string name)
    {
      if (Parent != null)
      {
        Assertion.Fail($"Trying to bound already bound {this} to {parent.Location}");
      }

      //todo uncomment when fix InterningTest
      // Assertion.Require(RdId != RdId.Nil, "Must be identified first");
      var proto = parent.TryGetProto();
      if (proto == null)
        return;

      using var cookie = lf.UsingExecuteIfAlive();
      if (!cookie.Succeed)
        return;

      Parent = parent;
      Location = parent.Location.Sub(name);
      myBindLifetime = lf;

      AssertBindingThread();

      if (proto is Protocol proto2 && !RdId.IsNil)
      {
        proto2.RdEntitiesRegistrar.Register(lf, RdId, this);
      }

      using (Signal.PriorityAdviseCookie.Create())
        PreInit(lf, proto);

      BindState = BindState.PreBound;

      lf.OnTermination(this);
    }

    public void Bind()
    {
      AssertBindingThread();
      var bindLifetime = myBindLifetime;
      var bindState = BindState;
      
      var proto = TryGetProto();
      if (proto == null || !TryGetSerializationContext(out var ctx))
        return;

      using var cookie = bindLifetime.UsingExecuteIfAlive();
      if (!cookie.Succeed)
        return;

      Assertion.Assert(bindState == BindState.PreBound);

      using (Signal.PriorityAdviseCookie.Create())
        Init(bindLifetime, proto, ctx);
      
      BindState = BindState.Bound;
    }

    protected virtual void Unbind()
    {
    }

    public void OnTermination(Lifetime lifetime)
    {
      Unbind();
      
      Location = Location.Sub("<<unbound>>", "::");
      RdId = RdId.Nil;
      BindState = BindState.NotBound;
      
      Memory.VolatileWrite(ref Parent, null);
    }

    protected virtual void AssertBindingThread()
    {
      if (AllowBindCookie.IsBindNotAllowed)
      {
        var proto = TryGetProto().NotNull(this);
        if (proto.Lifetime.IsNotAlive)
          return;
        
        proto.Scheduler.AssertThread(this);
      }
    }

    protected virtual void PreInit(Lifetime lifetime, IProtocol proto)
    {
      PreInitBindableFields(lifetime);
    }
    
    protected virtual void Init(Lifetime lifetime, IProtocol proto, SerializationCtx ctx)
    {
      InitBindableFields(lifetime);
    }

    protected virtual void PreInitBindableFields(Lifetime lifetime)
    {
      foreach (var pair in BindableChildren)
      {
        pair.Value?.PreBindPolymorphic(lifetime, this, pair.Key);
      }
    }
    
    protected virtual void InitBindableFields(Lifetime lifetime)
    {
      foreach (var pair in BindableChildren)
      {
        pair.Value?.BindPolymorphic();
      }
    }

    public virtual void Identify(IIdentities identities, RdId id)
    {
      Assertion.Require(RdId.IsNil, "Already has RdId: {0}, entity: {1}", RdId, this);      
      Assertion.Require(!id.IsNil, "Assigned RdId mustn't be null, entity: {0}", this);
      
      RdId = id;
      foreach (var pair in BindableChildren)
      {
        pair.Value?.IdentifyPolymorphic(identities, identities.Mix(id, "." + pair.Key));
      }
    }

    public virtual RdBindableBase? FindByRName(RName rName)
    {
      var rootName = rName.GetNonEmptyRoot();
      var child = BindableChildren
        .Select(child => child.Value)
        .OfType<RdBindableBase>()
        .FirstOrDefault(child => child.Location.Separator == rootName.Separator &&
                        child.Location.LocalName == rootName.LocalName);

      if (child == null)
        return null;
      
      if (rootName == rName)
        return child;

      return child.FindByRName(rName.DropNonEmptyRoot());
    }
    
    
    
    protected virtual string ShortName => GetType().ToString(false, true);

    public virtual void Print(PrettyPrinter printer)
    {
      if (Location == NotBound || printer.PrintContent)
        printer.Print(GetType().ToString(false, true));
      else
        printer.Print($"{ShortName} `{Location}`" + (RdId != RdId.Nil ? $" ({RdId})" : ""));
    }

    public override string ToString()
    {
      var printer = new SingleLinePrettyPrinter {PrintContent = false};
      Print(printer);
      return printer.ToString();
    }


    private readonly IDictionary<string, object> myExtensions = new Dictionary<string, object>();


    public T? GetExtension<T>(string name) where T:class
    {
      lock (myExtensions)
      {
        if (myExtensions.TryGetValue(name, out var existing))
        {
          return (T) existing;
        }
        else
        {
          return null;
        }
      }
    }

    public T GetOrCreateExtension<T>(string name, Func<T> create) where T : class =>
      GetOrCreateExtension(name, false, create);
    
    internal T GetOrCreateHighPriorityExtension<T>(string name, Func<T> create) where T : class =>
      GetOrCreateExtension(name, true, create);
    
    private T GetOrCreateExtension<T>(string name, bool highPriorityExtension, Func<T> create) where T:class
    {
      if (name == null) throw new ArgumentNullException(nameof(name));
      if (create == null) throw new ArgumentNullException(nameof(create));

      lock (myExtensions)
      {
        T res;
        if (myExtensions.TryGetValue(name, out var existing))
        {
          var val = existing.NotNull("Found null value for key: '{0}'", name) as T;
          Assertion.Require(val != null, "Found bad value for key '{0}'. Expected type: '{1}', actual:'{2}", name, typeof(T).FullName, existing.GetType().FullName);
          res = val;
        }
        else
        {
          res = create().NotNull("'Create' result must not be null");
          
          myExtensions[name] = res;
          
          if (res is IRdBindable bindable)
          {
            BindableChildren.Insert(highPriorityExtension ? 0 : BindableChildren.Count, new KeyValuePair<string, object>(name, bindable));
            var proto = TryGetProto();
            if (proto == null)
              return res;

            var bindLifetime = myBindLifetime;
            if (bindLifetime.IsAlive)
            {
              if (bindable.RdId == RdId.Nil)
                bindable.Identify(proto.Identities, proto.Identities.Mix(RdId, "." + name));
              bindable.PreBind(bindLifetime, this, name);
              bindable.Bind();
            }
          }
        }

        return res;
      }
    }

    // NOTE: dummy implementation which prevents WPF from hanging the viewmodel forever on reflection property descriptor fabricated change events:
    //       when it sees PropertyChanged, it does not look for property descriptor events
    public virtual event PropertyChangedEventHandler PropertyChanged { add { } remove { } }
  }
  
  public enum BindState
  {
    NotBound,
    PreBound,
    Bound
  }
}