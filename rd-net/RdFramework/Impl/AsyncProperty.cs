using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;

namespace JetBrains.Rd.Impl;

public class AsyncRdProperty<T> : IRdReactive, IAsyncProperty<T>, INotifyPropertyChanged, ITerminationHandler
{
  #region Constructor

  public event PropertyChangedEventHandler? PropertyChanged;

  public AsyncRdProperty(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
  {
    ReadValueDelegate = readValue;
    WriteValueDelegate = writeValue;

    myProperty = new ViewableProperty<T>();
    myProperty.Advise(Lifetime.Eternal, value =>
    {
      PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("Value"));
      myChange.Fire(value); // todo 
    });
  }

  [PublicAPI]
  public AsyncRdProperty(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue, T defaultValue) : this(readValue, writeValue)
  {
    myProperty.Value = defaultValue;
  }

  #endregion


  #region Serializers

  public CtxReadDelegate<T> ReadValueDelegate { get; private set; }
  public CtxWriteDelegate<T> WriteValueDelegate { get; private set; }

  public static AsyncRdProperty<T> Read(SerializationCtx ctx, UnsafeReader reader)
  {
    return Read(ctx, reader, Polymorphic<T>.Read, Polymorphic<T>.Write);
  }

  public static AsyncRdProperty<T> Read(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
  {
    var id = reader.ReadRdId();
    var res = new AsyncRdProperty<T>(readValue, writeValue).WithId(id);
    if (reader.ReadBool())
    {
      res.myProperty.Value = readValue(ctx, reader);
    }

    return res;
  }

  public static void Write(SerializationCtx ctx, UnsafeWriter writer, AsyncRdProperty<T> value)
  {
    value.Write(ctx, writer);
  }

  private void Write(SerializationCtx ctx, UnsafeWriter writer)
  {
    var bookmark = writer.MakeBookmark();
    Maybe<T> maybe;
    lock (myProperty)
      maybe = myProperty.Maybe;

    while (true)
    {
      lock (myProperty)
      {
        writer.Write(RdId);
      }

      if (maybe.HasValue)
      {
        writer.WriteBoolean(true);
        WriteValueDelegate(ctx, writer, myProperty.Value);
      }
      else
      {
        writer.WriteBoolean(false);
      }

      lock (myProperty)
      {
        var currentMaybe = myProperty.Maybe;
        if (maybe.HasValue)
        {
          var currentValue = currentMaybe.Value;
          if (EqualityComparer<T>.Default.Equals(maybe.Value, currentValue))
            return;
        }
        else
        {
          if (!currentMaybe.HasValue)
            return;
        }

        maybe = currentMaybe;
      }

      bookmark.Reset();
    }
  }

  #endregion


  #region Mastering

  public bool IsMaster = false;
  private int myMasterVersion;

  #endregion


  #region Init

  public bool OptimizeNested
  {
    get => true;
    set { }
  }

  public bool Async
  {
    get => true;
    set { }
  }

  public RdId RdId { get; set; }
  public RName Location { get; private set; } = RName.Empty;
  private IRdDynamic? myParent;
  private Lifetime myBindLifetime = Lifetime.Terminated;

  private readonly ThreadLocal<bool> myIsLocalChange = new();


  public IProtocol? TryGetProto() => myParent?.TryGetProto();

  public bool TryGetSerializationContext(out SerializationCtx ctx)
  {
    if (myParent is { } parent)
      return parent.TryGetSerializationContext(out ctx);

    ctx = default;
    return false;
  }

  public IAsyncSource<T> Change => myChange;


  public void Identify(IIdentities identities, RdId id)
  {
    Assertion.Require(!id.IsNil, $"Assigned RdId mustn't be null, entity: {this}");

    lock (myProperty)
    {
      Assertion.Require(RdId.IsNil, $"Already has RdId: {RdId}, entity: {this}");
      RdId = id;
    }
  }

  public void OnTermination(Lifetime lifetime)
  {
    lock (myProperty)
    {
      Location = Location.Sub("<<unbound>>", "::");
      RdId = RdId.Nil;
      myParent = null;
      // BindState = BindState.NotBound;
    }
  }

  public void PreBind(Lifetime lifetime, IRdDynamic parent, string name)
  {
    var proto = parent.TryGetProto();
    if (proto == null)
      return;

    lock (myProperty)
    {
      using var cookie = lifetime.UsingExecuteIfAlive();
      if (!cookie.Succeed)
        return;

      myParent = parent;
      Location = parent.Location.Sub(name);
      myBindLifetime = lifetime;

      lifetime.OnTermination(this);
    }

    proto.Wire.Advise(lifetime, this);
  }

  public void Bind()
  {
    lock (myProperty)
    {
      var lifetime = myBindLifetime;
      var proto = TryGetProto();
      if (proto == null || !TryGetSerializationContext(out var ctx))
        return;
        
      myProperty.Advise(lifetime, v =>
      {
        if (!myIsLocalChange.Value)
          return;
        
        if (IsMaster) myMasterVersion++;

        proto.Wire.Send(RdId, SendContext.Of(ctx, v, this), static (sendContext, writer) =>
        {
          var sContext = sendContext.SzrCtx;
          var evt = sendContext.Event;
          var me = sendContext.This;
          writer.Write(me.myMasterVersion);
          me.WriteValueDelegate(sContext, writer, evt); // todo don't call it under lock
          RdReactiveBase.SendTrace?.Log($"{me} :: ver = {me.myMasterVersion}, value = {me.Value.PrintToString()}");
        });
      });
    }
  }

  public void OnWireReceived(UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper)
  {
    if (!TryGetSerializationContext(out var ctx))
      return;
    
    var version = reader.ReadInt();
    var value = ReadValueDelegate(ctx, reader);

    lock (myProperty)
    {
      if (dispatchHelper.Lifetime.IsNotAlive)
        return;
      
      var rejected = IsMaster && version < myMasterVersion;

      RdReactiveBase.ReceiveTrace?.Log($"{this} :: oldver = {myMasterVersion}, newver = {version}, value = {value.PrintToString()}{(rejected ? " REJECTED" : "")}");

      if (rejected)
        return;

      myMasterVersion = version;
      myProperty.Value = value;
    }
  }

  #endregion

  public bool ValueCanBeNull { get; set; }

  private void AssertNullability(T value)
  {
    if ( //optimization for memory traffic 
        typeof(T).IsValueType || ValueCanBeNull || value != null) return;

    Assertion.Fail("Value is defined as not nullable: {0}", this);
  }


  #region Api

  private readonly IViewableProperty<T> myProperty;
  private readonly AsyncSignal<T> myChange = new();

  public Maybe<T> Maybe
  {
    get
    {
      lock (myProperty)
        return myProperty.Maybe;
    }
  }

  public T Value
  {
    get
    {
      lock (myProperty)
        return myProperty.Value;
    }
    set
    {
      AssertNullability(value);
      myIsLocalChange.Value = true;
      try
      {
        lock (myProperty)
          myProperty.Value = value;
      }
      finally
      {
        myIsLocalChange.Value = false;
      }
    }
  }

  public void AdviseOn(Lifetime lifetime, IScheduler scheduler, Action<T> action)
  {
    lock (myProperty)
    {
      myProperty.Advise(lifetime, value => { scheduler.Queue(() => { action(value); }); });
    }
  }

  #endregion


  public void Print(PrettyPrinter printer)
  {
    if (!printer.PrintContent)
      return;

    lock (myProperty)
    {
      printer.Print("(ver=" + myMasterVersion + ") [");
      if (Maybe.HasValue)
      {
        using (printer.IndentCookie())
        {
          Value.PrintEx(printer);
        }
      }
      else
      {
        printer.Print(" <not initialized> ");
      }

      printer.Print("]");
    }
  }
}