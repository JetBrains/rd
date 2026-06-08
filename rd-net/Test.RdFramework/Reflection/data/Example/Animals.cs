using System;
using System.Collections.Immutable;
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
    [NonSerialized] [CanBeNull] public string[] arraysNonSerialized;

    /// <summary>
    /// Arbitrary data without [NonSerialized] will be serialized only once at the moment of passing to the other side.
    /// </summary>
    [CanBeNull] public string[] arrays2;
    
    [CanBeNull] public IPolymorphicScalar scalar;

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

  [RdModel]
  public class ModelLight : RdReflectionBindableBase
  {
    public AbstractModel AbstractModelField;
  }

  [RdModel]
  public class AbstractModel : RdReflectionBindableBase{}

  [RdModel]
  public class ConcreteModel : AbstractModel{}

  public interface IPolymorphicScalar
  {

  }

  public class Scalar : IPolymorphicScalar
  {
  }
}