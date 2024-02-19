using System;
using System.ComponentModel;
using System.Threading;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;
using JetBrains.Rd.Base;
using JetBrains.Rd.Util;
using JetBrains.Serialization;
using JetBrains.Annotations;
using JetBrains.Util.Internal;

namespace JetBrains.Rd.Impl
{
  public abstract class RdPropertyBase : RdReactiveBase {}
  public class RdProperty<T> : RdPropertyBase, IViewableProperty<T>
  {
    #region Constructor

    public override event PropertyChangedEventHandler? PropertyChanged;

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
      Assertion.Assert(!value.RdId.IsNil);
      writer.Write(value.RdId);
      if (value.HasValue())
      {
        writer.WriteBoolean(true);
        value.WriteValueDelegate(ctx, writer, value.Value);
      }
      else
      {
        writer.WriteBoolean(false);
      }
    }

    #endregion
    #region Mastering

    public bool IsMaster = false;
    private int myMasterVersion;

    #endregion
    #region Init

    public bool OptimizeNested = false;
    private LifetimeDefinition? myBindDefinition;

    public override void Identify(IIdentities identities, RdId id)
    {
      base.Identify(identities, id);
      if (!OptimizeNested)
      {
        Maybe.ValueOrDefault.IdentifyPolymorphic(identities, identities.Next(id));
      }
    }

    protected override void PreInit(Lifetime lifetime, IProtocol proto)
    {
      base.PreInit(lifetime, proto);

      if (!OptimizeNested)
      {
        var maybe = Maybe;
        if (maybe.HasValue)
        {
          var definition = TryPreBindValue(lifetime, maybe.Value, false);
          using var cookie = lifetime.UsingExecuteIfAlive();
          if (cookie.Succeed)
          {
            var prevDefinition = Interlocked.Exchange(ref myBindDefinition , definition);
            Assertion.Assert(prevDefinition?.Lifetime.IsNotAlive ?? true);

          }
          else return;
        }
      }

      proto.Wire.Advise(lifetime, this);
    }

    protected override void Init(Lifetime lifetime, IProtocol proto, SerializationCtx ctx)
    {
      base.Init(lifetime, proto, ctx);

      var maybe = Maybe;
      var hasInitValue = maybe.HasValue;
      if (hasInitValue && !OptimizeNested)
        maybe.Value.BindPolymorphic();

      Advise(lifetime, v =>
      {
        var shouldIdentify = !hasInitValue;
        hasInitValue = false;

        if (!IsLocalChange)
          return;

        if (!OptimizeNested && shouldIdentify)
        {
          // We need to terminate the current lifetime to unbind the existing value before assigning a new value, especially in cases where we are reassigning it.
          Memory.VolatileRead(ref myBindDefinition)?.Terminate();

          v.IdentifyPolymorphic(proto.Identities, proto.Identities.Next(RdId));

          var prevDefinition = Interlocked.Exchange(ref myBindDefinition, TryPreBindValue(lifetime, v, false));
          prevDefinition?.Terminate();
        }

        if (IsMaster) myMasterVersion++;

        proto.Wire.Send(RdId, SendContext.Of(ctx, v, this), static (sendContext, writer) =>
        {
          var sContext = sendContext.SzrCtx;
          var evt = sendContext.Event;
          var me = sendContext.This;
          writer.WriteInt32(me.myMasterVersion);
          me.WriteValueDelegate(sContext, writer, evt);
          SendTrace?.Log($"{me} :: ver = {me.myMasterVersion}, value = {me.Value.PrintToString()}");
        });

        if (!OptimizeNested && shouldIdentify)
          v.BindPolymorphic();
      });
    }

    protected override void Unbind()
    {
      base.Unbind();
      myBindDefinition = null;
    }

    public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader reader, IRdWireableDispatchHelper dispatchHelper)
    {
      var version = reader.ReadInt();
      var value = ReadValueDelegate(ctx, reader);

      var lifetime = dispatchHelper.Lifetime;
      var definition = TryPreBindValue(lifetime, value, true);

      ReceiveTrace?.Log($"OnWireReceived:: {GetMessage(version, value)}");

      dispatchHelper.Dispatch(() =>
      {
        var rejected = IsMaster && version < myMasterVersion;

        ReceiveTrace?.Log($"Dispatch:: {GetMessage(version, value)}{(rejected ? " REJECTED" : "")}");

        if (rejected)
        {
          definition?.Terminate();
          return;
        }

        myMasterVersion = version;

        using (UsingDebugInfo())
        {
          Interlocked.Exchange(ref myBindDefinition, definition)?.Terminate();
          myProperty.Value = value;
        }
      });
    }

    private string GetMessage(int version, T value)
    {
      return $"{this} :: oldver = {myMasterVersion}, newver = {version}, value = {value.PrintToString()}";
    }

    private LifetimeDefinition? TryPreBindValue(Lifetime lifetime, T value, bool bindAlso)
    {
      if (OptimizeNested || !value.IsBindable())
        return null;

      var definition = new LifetimeDefinition { Id = value };
      try
      {
        value.PreBindPolymorphic(definition.Lifetime, this, "$");
        if (bindAlso)
          value.BindPolymorphic();

        lifetime.Definition.Attach(definition, true);
        return definition;
      }
      catch
      {
        definition.Terminate();
        throw;
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

    public override RdBindableBase? FindByRName(RName rName)
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