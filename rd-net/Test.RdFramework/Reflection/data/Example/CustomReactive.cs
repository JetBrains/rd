using JetBrains.Annotations;
using JetBrains.Lifetimes;
using JetBrains.Rd;
using JetBrains.Rd.Base;
using JetBrains.Rd.Reflection;
using JetBrains.Serialization;

namespace Test.RdFramework.Reflection
{
  [AssertBuiltInType(BuiltInSerializers.BuiltInType.ProtocolCollectionLike2)]
  public sealed class CustomReactive<T1, T2> : RdReactiveBase
  {
    private bool myCreatedFromRead = false;
    public T1 t1;
    public T2 t2;
    public int EventCount;

    public CtxReadDelegate<T1> ReadKeyDelegate { get; private set; }
    public CtxWriteDelegate<T1> WriteKeyDelegate { get; private set; }
    public CtxReadDelegate<T2> ReadValueDelegate { get; private set; }
    public CtxWriteDelegate<T2> WriteValueDelegate { get; private set; }

    [UsedImplicitly]
    public CustomReactive(CtxReadDelegate<T1> readKeyDelegate,
      CtxWriteDelegate<T1> writeKeyDelegate,
      CtxReadDelegate<T2> readValueDelegate,
      CtxWriteDelegate<T2> writeValueDelegate)
    {
      ReadKeyDelegate = readKeyDelegate;
      WriteKeyDelegate = writeKeyDelegate;
      ReadValueDelegate = readValueDelegate;
      WriteValueDelegate = writeValueDelegate;
    }

    public CustomReactive(CtxReadDelegate<T1> readKeyDelegate,
      CtxWriteDelegate<T1> writeKeyDelegate,
      CtxReadDelegate<T2> readValueDelegate,
      CtxWriteDelegate<T2> writeValueDelegate,
      T1 t,
      T2 t2,
      bool createdFromRead = false)
    {
      ReadKeyDelegate = readKeyDelegate;
      WriteKeyDelegate = writeKeyDelegate;
      ReadValueDelegate = readValueDelegate;
      WriteValueDelegate = writeValueDelegate;
      myCreatedFromRead = createdFromRead;
      this.t1 = t;
      this.t2 = t2;
    }

    public override string ToString() => $"{myCreatedFromRead}:{t1}:{t2}";

    #region Intrinsic

    public static CustomReactive<T1, T2> Read(SerializationCtx ctx, UnsafeReader reader,
      CtxReadDelegate<T1> readKey, CtxWriteDelegate<T1> writeKey,
      CtxReadDelegate<T2> readValue, CtxWriteDelegate<T2> writeValue)
    {
      var flag = reader.ReadBool();
      var t1 = ctx.Serializers.Read<T1>(ctx, reader);
      var t2 = ctx.Serializers.Read<T2>(ctx, reader);
      return new CustomReactive<T1, T2>(readKey, writeKey, readValue, writeValue, t1, t2, flag);
    }

    public static void Write(SerializationCtx ctx, UnsafeWriter writer, CustomReactive<T1, T2> value)
    {
      writer.Write(true);
      ctx.Serializers.Write(ctx, writer, value.t1);
      ctx.Serializers.Write(ctx, writer, value.t2);
      writer.Write(true);
    }

    #endregion

    public override void OnWireReceived(IProtocol proto, SerializationCtx ctx, UnsafeReader stream, IRdWireableDispatchHelper dispatchHelper)
    {
      dispatchHelper.Dispatch(() =>
      {
        EventCount++;
      });
    }
  }
}