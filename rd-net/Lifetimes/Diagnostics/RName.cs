using System;
using JetBrains.Annotations;

namespace JetBrains.Diagnostics
{
  /// <summary>
  /// Recursive name. For constructs like Aaaa.Bbb::CCC
  /// </summary>
  public class RName
  {
    [PublicAPI] public static readonly RName Empty = new RName(null, "", "");
    
    [PublicAPI, CanBeNull] 
    public readonly object Parent;
    [PublicAPI, NotNull]
    public readonly string Separator;
    [PublicAPI, NotNull] 
    public readonly object LocalName;
    

    public RName([CanBeNull] object parent, [NotNull] object localName, [NotNull] string separator)
    {
      Parent = parent;
      Separator = separator ?? throw new ArgumentNullException(nameof(separator));
      LocalName = localName ?? throw new ArgumentNullException(nameof(localName));
    }
    
    public RName([NotNull] object localName) : this(Empty, localName, "") {}

    /// <summary>
    /// Separator doesn't count if localName is empty or parent is empty.
    /// </summary>
    /// <param name="localName"></param>
    /// <param name="separator"></param>
    /// <returns></returns>
    /// <exception cref="ArgumentNullException"></exception>
    public RName Sub([NotNull] object localName, string separator=".")
    {
      if (localName == null) throw new ArgumentNullException(nameof(localName));
      if (localName is string s && s.Length == 0) 
        return this; //special case for empty string

      return new RName(this, localName, separator ?? "");
    }

    [NotNull]
    public RName GetNonEmptyRoot()
    {
      if (!(Parent is RName parent) || Parent == Empty)
        return this;

      return parent.GetNonEmptyRoot();
    }

    [NotNull]
    public RName DropNonEmptyRoot()
    {
      if (!(Parent is RName parent) || parent == Empty)
        return Empty;

      var tail = parent.DropNonEmptyRoot();
      return tail.Sub(LocalName, Separator);
    }

    public override string ToString()
    {
      var lname = LocalName.ToString();
      if (Parent == null)
        return lname;

      var pname = Parent.ToString();

      return pname.Length == 0 ? lname : pname + Separator + lname;
    }
  }
}