using JetBrains.Rd.Base;

namespace JetBrains.Rd.Impl
{
  internal static class SendContext
  {
    internal static SendContext<TEvt, TReactive> Of<TEvt, TReactive>(SerializationCtx serializationContext, TEvt @event, TReactive me) where TReactive: IRdReactive
    {
      return new SendContext<TEvt, TReactive>(serializationContext, @event, me);
    }
  }

  internal struct SendContext<TEvt, TReactive> where TReactive : IRdReactive
  {
    public void Deconstruct(out SerializationCtx serializationContext, out TEvt @event, out TReactive map)
    {
      serializationContext = SzrCtx;
      @event = Event;
      map = This;
    }

    internal readonly SerializationCtx SzrCtx;
    internal readonly TEvt Event;
    internal readonly TReactive This;

    public SendContext(SerializationCtx serializationContext, TEvt @event, TReactive me)
    {
      SzrCtx = serializationContext;
      Event = @event;
      This = me;
    }
  }
}