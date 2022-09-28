using System.Collections.Generic;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Impl;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public sealed class ModelSample : RdReflectionBindableBase
  {
    public IViewableMap<int, int> IMap { get; }
    public IViewableList<int> IList { get; }
    public IViewableProperty<string> Prop { get; }
    public RdSignal<string> Signal;
    public RdList<string> List;
    public RdSet<string> Set;
    public RdMap<string, string> Map;
    public IViewableList<IViewableList<string>> Multilist;
    public RdMap<string, IList<string>> MultimapNonReactive;
    public string RegularFieldInModel;
  }
}