using System.Collections.Concurrent;
using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl
{
  internal class InterningProtocolContextHandler<T> : RdReactiveBase, ISingleKeyProtocolContextHandler<T>
  {
    private readonly ProtocolContextHandler myHandler;
    private readonly InternRoot myInternRoot = new InternRoot();
    private readonly RdSet<T> myProtocolValueSet = new RdSet<T>();
    private readonly ViewableSet<T> myLocalValueSet = new ViewableSet<T>();
    private ContextValueTransformer<T> myValueTransformer;
    
    private readonly ConcurrentDictionary<T, T> myValuesConcurrentSet = new ConcurrentDictionary<T, T>();


    internal IViewableSet<T> LocalValueSet => myLocalValueSet;
    internal IViewableSet<T> ProtocolValueSet => myProtocolValueSet;

    public InterningProtocolContextHandler(RdContextKey<T> key, ProtocolContextHandler handler)
    {
      myHandler = handler;
      Key = key;
    }

    public RdContextKey<T> Key { get; }

    public ContextValueTransformer<T> ValueTransformer
    {
      get => myValueTransformer;
      set
      {
        myValueTransformer = value;
        using (new LocalChangeCookie(this))
        {
          myLocalValueSet.Clear();
          if (value != null)
          {
            foreach (var protocolValue in myProtocolValueSet)
            {
              var transformedValue = value(protocolValue, ContextValueTransformerDirection.ReadFromProtocol);
              if (transformedValue != null) myLocalValueSet.Add(transformedValue);
            }
          } else
          {
            foreach (var protocolValue in myProtocolValueSet) myLocalValueSet.Add(protocolValue);
          }
        }
      }
    }

    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      using (myHandler.CreateOwnMessageCookie())
      {
        myInternRoot.RdId = RdId.Mix("InternRoot");
        myProtocolValueSet.RdId = RdId.Mix("ValueSet");

        myInternRoot.Bind(lifetime, this, "InternRoot");
        myProtocolValueSet.Bind(lifetime, this, "ValueSet");
      }

      myProtocolValueSet.Advise(lifetime, HandleProtocolSetEvent);
      myLocalValueSet.Advise(lifetime, HandleLocalSetEvent);
    }

    private void HandleLocalSetEvent(SetEvent<T> obj)
    {
      if (IsLocalChange)
        return;
      
      var newValue = myValueTransformer == null ? obj.Value : myValueTransformer(obj.Value, ContextValueTransformerDirection.WriteToProtocol);
      if (newValue == null) return;
      using(myHandler.CreateOwnMessageCookie())
        if (obj.Kind == AddRemove.Add)
        {
          if (!myProtocolValueSet.Contains(newValue))
            myProtocolValueSet.Add(newValue);
        }
        else
        {
          if (myProtocolValueSet.Contains(newValue))
            myProtocolValueSet.Remove(newValue);
        }
    }

    private void HandleProtocolSetEvent(SetEvent<T> obj)
    {
      var value = obj.Value;
      var newValue = myValueTransformer == null ? obj.Value : myValueTransformer(obj.Value, ContextValueTransformerDirection.ReadFromProtocol);
      using(myHandler.CreateOwnMessageCookie())
        if (obj.Kind == AddRemove.Add)
        {
          myValuesConcurrentSet.TryAdd(value, value);
          myInternRoot.Intern(value);
          if (newValue != null)
            myLocalValueSet.Add(newValue);
        }
        else
        {
          myValuesConcurrentSet.TryRemove(value, out _);
          myInternRoot.Remove(value);
          if (newValue != null)
            myLocalValueSet.Remove(newValue);
        }
    }


    public void WriteValue(SerializationCtx context, UnsafeWriter writer)
    {
      Assertion.Assert(!myHandler.IsWritingOwnMessages, "!myHandler.IsWritingOwnMessages");
      var originalValue = Key.Value;
      var value = this.TransformToProtocol(originalValue);
      if (value == null)
      {
        writer.Write(-1);
      }
      else
      {
        using (myHandler.CreateOwnMessageCookie())
        {
          if (!myValuesConcurrentSet.ContainsKey(value))
          {
            if (Proto.Scheduler.IsActive)
            {
              Assertion.AssertNotNull(originalValue, "Can't perform an implicit add with null local context value for key {0}", Key.Key);
              myLocalValueSet.Add(originalValue);
            } else Assertion.Fail($"Attempting to use previously unused context value {value} on a background thread");
          }
          writer.Write(myInternRoot.Intern(value));
        }
      }
    }

    public T ReadValue(SerializationCtx context, UnsafeReader reader)
    {
      var id = reader.ReadInt();
      if (id == -1)
        return default;
      return this.TransformFromProtocol(myInternRoot.UnIntern<T>(id ^ 1));
    }

    public override void OnWireReceived(UnsafeReader reader)
    {
      Assertion.Fail("InterningProtocolContextHandler can't receive messages");
    }

    public void ReadValueAndPush(SerializationCtx context, UnsafeReader reader)
    {
      Key.PushContext(ReadValue(context, reader));
    }

    public void PopValue()
    {
      Key.PopContext();
    }
  }
}