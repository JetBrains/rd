using System.Collections.Generic;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Base;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public class Animal : RdBindableBase
  {
    // [NonSerialized]
    public List<KeyValuePair<string, object>> PublicMorozov => BindableChildren
    ;
    [CanBeNull] public string[] arrays;

    public IViewableList<FieldsNotNullOk> lists;
    // public IViewableMap<FieldsNotNullOk, int[]> maps { get; set; }
    // public IViewableSet<FieldsNotNullOk> sets;
  }
}