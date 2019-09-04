using System.Collections.Generic;

// ReSharper disable InconsistentNaming

namespace JetBrains.Collections.Viewable
{
  public interface IViewableList<T> : IList<T>, ISource<ListEvent<T>>
  {
    
  }

}