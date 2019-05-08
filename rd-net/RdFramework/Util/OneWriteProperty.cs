using System;
using JetBrains.Collections.Viewable;

namespace JetBrains.Rd.Util
{
  public class WriteOnceProperty<T> : ViewableProperty<T>
  {
    public override T Value
    {
      set
      {
        if (Maybe.HasValue && !Equals(Maybe.Value, value))
          throw new InvalidOperationException("OneWriteProperty already set with `"+Maybe.Value+"`, but you try to rewrite it to `"+value+"`");
        base.Value = value;
      }
    }
  }
}