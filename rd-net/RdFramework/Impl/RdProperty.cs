using System;
using System.ComponentModel;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;
using JetBrains.Annotations;

namespace JetBrains.Rd.Impl
{
  public abstract class RdPropertyBase : RdReactiveBase {}
  public class RdProperty<T> : RdPropertyBase, IViewableProperty<T>
  {
    #region Constructor

    public override event PropertyChangedEventHandler PropertyChanged;

    public RdProperty() : this(Polymorphic<T>.Read, Polymorphic<T>.Write) {}

    public RdProperty(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
    {
      ReadValueDelegate = readValue;
      WriteValueDelegate = writeValue;

      Advise(Lifetime.Eternal, _ =>
      {
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs("Value"));
      });
    }
    
    [PublicAPI]
    public RdProperty(CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue, T defaultValue) : this(readValue, writeValue)
    {
      Value = defaultValue;
    }

    #endregion


    #region Serializers

    public CtxReadDelegate<T> ReadValueDelegate { get; private set; }
    public CtxWriteDelegate<T> WriteValueDelegate { get; private set; }

    public static RdProperty<T> Read(SerializationCtx ctx, UnsafeReader reader)
    {
      return Read(ctx, reader, Polymorphic<T>.Read, Polymorphic<T>.Write);
    }

    public static RdProperty<T> Read(SerializationCtx ctx, UnsafeReader reader, CtxReadDelegate<T> readValue, CtxWriteDelegate<T> writeValue)
    {
      var id = reader.ReadRdId();
      var res =  new RdProperty<T>(readValue, writeValue).WithId(id);
      if (reader.ReadBool())
      {
        res.myProperty.Value = readValue(ctx, reader);
      }
      return res;
    }

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, RdProperty<T> value)
    {
      Assertion.Assert(!value.RdId.IsNil, "!value.RdId.IsNil");
      writer.Write(value.RdId);
      if (value.HasValue())
      {
        writer.Write(true);
        value.WriteValueDelegate(ctx, writer, value.Value);
      }
      else
      {
        writer.Write(false);
      }
    }
    #endregion



    #region Mastering

    public bool IsMaster = false;
    private int myMasterVersion;
    
    #endregion



    #region Init

    public bool OptimizeNested = false;

    public override void Identify(IIdentities identities, RdId id)
    {
      base.Identify(identities, id);
      if (!OptimizeNested)
      {
        Maybe.ValueOrDefault.IdentifyPolymorphic(identities, identities.Next(id));
      }
    }


    protected override void Init(Lifetime lifetime)
    {
      base.Init(lifetime);

      var serializationContext = SerializationContext;

      if (!OptimizeNested)
      {
        Change.Advise(lifetime, v =>
        {
          if (IsLocalChange)
            v.IdentifyPolymorphic(Proto.Identities, Proto.Identities.Next(RdId));
        });
      }

      Advise(lifetime, v =>
      {
        if (!IsLocalChange) return;
        if (IsMaster) myMasterVersion++;

        Wire.Send(RdId, SendContext.Of(serializationContext, v, this), (sendContext, writer) =>
        {
          var sContext = sendContext.SzrCtx;
          var evt = sendContext.Event;
          var me = sendContext.This;
          writer.Write(me.myMasterVersion);
          me.WriteValueDelegate(sContext, writer, evt);
          SendTrace?.Log($"{this} :: ver = {me.myMasterVersion}, value = {me.Value.PrintToString()}");
        });
      });


      Wire.Advise(lifetime, this);


      if (!OptimizeNested)
      {
        this.View(lifetime, (lf, v) =>
        {
          v.BindPolymorphic(lf, this, "$");
        });
      }

    }


    public override void OnWireReceived(UnsafeReader reader)
    {
      var version = reader.ReadInt();
      var value = ReadValueDelegate(SerializationContext, reader);

      var rejected = IsMaster && version < myMasterVersion;
        
      ReceiveTrace?.Log($"{this} :: oldver = {myMasterVersion}, newver = {version}, value = {value.PrintToString()}{(rejected ? " REJECTED" : "")}");

      if (rejected) return;
        
      myMasterVersion = version;

      using (UsingDebugInfo())
      {
        myProperty.Value = value;
      }

    }

    #endregion


    #region Api

    private readonly IViewableProperty<T> myProperty = new ViewableProperty<T>();

    public ISource<T> Change => myProperty.Change;

    public Maybe<T> Maybe => myProperty.Maybe;

    public T Value
    {
      get => myProperty.Value;
      set
      {
        AssertNullability(value);
        using (UsingLocalChange())
        {
          myProperty.Value = value;
        }
      }
    }

    public void Advise(Lifetime lifetime, Action<T> handler)
    {
      if (IsBound) AssertThreading();
      
      using (UsingDebugInfo())
        myProperty.Advise(lifetime, handler);
    }

    public override RdBindableBase FindByRName(RName rName)
    {
      var rootName = rName.GetNonEmptyRoot();
      var localName = rootName.LocalName.ToString();
      if (localName != "$")
        return null;

      var maybe = myProperty.Maybe;
      if (!maybe.HasValue)
        return null;

      if (!(maybe.Value is RdBindableBase value))
        return null;

      if (rootName == rName)
        return value;

      return value.FindByRName(rName.DropNonEmptyRoot());
    }

    #endregion


    protected override string ShortName => "property";

    public override void Print(PrettyPrinter printer)
    {
      base.Print(printer);
      
      if (!printer.PrintContent)
        return;
      
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