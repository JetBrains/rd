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
    /// Arbitrary data without [NonSerialized] will be serialized only once at the moment of passing to the other side.
    /// </summary>
    [CanBeNull] public string[] arrays2;

    /// <summary>
    /// Nested RdModel should work
    /// </summary>
    public ModelSample NestedRdModel;

    public IViewableList<ModelSample> LiveList;
    public IViewableMap<ModelSample, int[]> Maps { get; set; }
    public IViewableSet<ModelSample> Sets;
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