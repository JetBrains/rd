using System;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public class Animal : RdReflectionBindableBase
  {
    /// <summary>
    /// It is possible to have arbitrary data in live models if attribute <see cref="NonSerializedAttribute"/> is specified
    /// </summary>
    [NonSerialized] [CanBeNull] public string[] arrays;

    /// <summary>
    /// Nested RdModel should work
    /// </summary>
    public ModelSample NestedRdModel;

    public IViewableList<ModelSample> LiveList;
    // public IViewableMap<FieldsNotNullOk, int[]> maps { get; set; }
    // public IViewableSet<FieldsNotNullOk> sets;
  }

  [RdModel]
  public sealed class Bear : Mammal
  {
  }

  [RdModel]
  public class Mammal : Animal
  {
  }
}