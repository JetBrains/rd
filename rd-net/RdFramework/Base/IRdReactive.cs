using JetBrains.Collections.Viewable;
using JetBrains.Diagnostics;
using JetBrains.Serialization;

namespace JetBrains.Rd.Base
{
  
  public interface IRdReactive : IRdBindable, IRdWireable
  {
    bool Async { get; set; }
  }


  public static class RdReactiveEx
  {
    //for backward compatibility
    public static void SetValue<T>(this IViewableProperty<T> p, T value)
    {
      p.Value = value;
    }
    
    public static void Set<T>(this IViewableProperty<T> p, T value)
    {
      p.Value = value;
    }

    public static T WithId<T>(this T thIs, RdId id) where T:IRdBindable
    {
//      Assertion.Require(thIs.Id == RdId.Nil, "Precondition failed: thIs.Id == {0}", thIs.Id);
      Assertion.Require(id != RdId.Nil, "Precondition failed: id != null");

      thIs.RdId = id;
      return thIs;
    }

    public static T Static<T>(this T thIs, int id) where T : IRdBindable
    {
      Assertion.Require(id > 0 && id < RdId.MaxStaticId, "id > 0 && id < RdId.MaxStaticId");
      return thIs.WithId(new RdId(id));
    }

  }
}