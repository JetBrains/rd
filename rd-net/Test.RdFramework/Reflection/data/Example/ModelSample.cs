using JetBrains.Collections.Viewable;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public sealed class ModelSample : RdReflectionBindableBase
  {
    public IViewableList<int> IList { get; }
    public IViewableProperty<string> Prop { get; }
    public RdSignal<string> Signal;
    public RdList<string> List;
    public RdSet<string> Set;
    public RdMap<string, string> Map;
    public string RegularFieldInModel;
  }
}