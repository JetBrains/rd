using System;
using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Util;
using JetBrains.Util.Util;

namespace JetBrains.Rd.Base
{
  public abstract class RdBindableBase : IRdBindable
  {
    #region Bound state: main
    
    public RdId RdId { get; set; }

    internal static readonly RName NotBound = new RName("<<not bound>>");
    public RName Location { get; private set; } = NotBound;   
    
    [CanBeNull] protected IRdDynamic Parent;
    
    [CanBeNull] private Lifetime myBindLifetime = Lifetime.Terminated;
    
    #endregion
        
    
    
    #region Bound state: inferred
    
    public bool IsBound => Parent != null;
    
    public IViewableProperty<bool> IsBoundProperty { get; } = new ViewableProperty<bool>();
   
    public virtual IProtocol Proto => Parent.NotNull(this).Proto;
    
    public virtual SerializationCtx SerializationContext => Parent.NotNull(this).SerializationContext;

    #endregion
    

    
    protected readonly List<KeyValuePair<string, object>> BindableChildren = new List<KeyValuePair<string, object>>();  

    
    public void Bind(Lifetime lf, IRdDynamic parent, string name)
    {
      if (Parent != null)
      {
        Assertion.Fail($"Trying to bound already bound {this} to {parent.Location}");
      }
      //todo uncomment when fix InterningTest
      //Assertion.Require(RdId != RdId.Nil, "Must be identified first");
     
      lf.Bracket(() =>
        {
          Parent = parent;
          Location = parent.Location.Sub(name);
          myBindLifetime = lf;                    
          IsBoundProperty.SetValue(true);                                   
        },
        () =>
        {
          IsBoundProperty.SetValue(false);          
          myBindLifetime = Lifetime.Terminated;
          Location = Location.Sub("<<unbound>>", "::");
          Parent = null;
          RdId = RdId.Nil;
        }
      );
      
      Proto.Scheduler.AssertThread(this);

      using (Signal.PriorityAdviseCookie.Create())
        Init(lf);
    }

    protected virtual void Init(Lifetime lifetime)
    {
      InitBindableFields(lifetime);
    }

    protected virtual void InitBindableFields(Lifetime lifetime)
    {
      foreach (var pair in BindableChildren)
      {
        pair.Value?.BindPolymorphic(lifetime, this, pair.Key);
      }
    }

    public virtual void Identify(IIdentities identities, RdId id)
    {
      Assertion.Require(RdId.IsNil, "Already has RdId: {0}, entity: {1}", RdId, this);      
      Assertion.Require(!id.IsNil, "Assigned RdId mustn't be null, entity: {0}", this);
      
      RdId = id;
      foreach (var pair in BindableChildren)
      {
        pair.Value?.IdentifyPolymorphic(identities, id.Mix("." + pair.Key));
      }
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


    public T GetExtension<T>([NotNull] string name) where T:class
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

    public T GetOrCreateExtension<T>([NotNull] string name, [NotNull] Func<T> create) where T : class =>
      GetOrCreateExtension(name, false, create);
    
    internal T GetOrCreateHighPriorityExtension<T>([NotNull] string name, [NotNull] Func<T> create) where T : class =>
      GetOrCreateExtension(name, true, create);
    
    private T GetOrCreateExtension<T>([NotNull] string name, bool highPriorityExtension, [NotNull] Func<T> create) where T:class
    {
      if (name == null) throw new ArgumentNullException(nameof(name));
      if (create == null) throw new ArgumentNullException(nameof(create));

      lock (myExtensions)
      {
        object existing;
        T res;
        if (myExtensions.TryGetValue(name, out existing))
        {
          res = existing.NotNull("Found null value for key: '{0}'", name) as T;
          Assertion.Require(res != null, "Found bad value for key '{0}'. Expected type: '{1}', actual:'{2}", name, typeof(T).FullName, existing.GetType().FullName);
        }
        else
        {
          res = create().NotNull("'Create' result must not be null");
          
          myExtensions[name] = res;
          if (res is IRdBindable)
          {
            var bindable = res as IRdBindable;
            BindableChildren.Insert(highPriorityExtension ? 0 : BindableChildren.Count, new KeyValuePair<string, object>(name, bindable));
            if (myBindLifetime != Lifetime.Terminated)
            {
              bindable.Identify(Proto.Identities, RdId.Mix("." + name));
              bindable.Bind(myBindLifetime, this, name);
            }
          }
        }

        return res;
      }
    }
    
  }
}