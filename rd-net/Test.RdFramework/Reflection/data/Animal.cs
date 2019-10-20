using System;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Base;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public class Animal : RdReflectionBindableBase
  {
    /// <summary>
    /// It is possible to have arbitrary data in live models if attribute <see cref="NonSerializedAttribute"/> is specified
    /// </summary>
    [NonSerialized]
    [CanBeNull] public string[] arrays;

    public IViewableList<FieldsNotNullOk> LiveList;
    // public IViewableMap<FieldsNotNullOk, int[]> maps { get; set; }
    // public IViewableSet<FieldsNotNullOk> sets;
  }
}