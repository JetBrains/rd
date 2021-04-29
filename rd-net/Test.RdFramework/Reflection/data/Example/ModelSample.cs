using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Rd.Reflection;

namespace Test.RdFramework.Reflection
{
  [RdModel]
  public sealed class ModelSample : RdReflectionBindableBase
  {
    public ModelSample()
    {
    }

    [NotNull] public IViewableList<int> FieldOne;
    
    /// <summary>
    /// TODO: how it should work?
    /// </summary>
    // [NotNull] public string RegularFieldInModel;
  }
}