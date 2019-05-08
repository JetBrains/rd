using System.Collections.Generic;
using System.Linq;
using System.Text;
using JetBrains.Rd.Text.Impl.Intrinsics;

namespace JetBrains.Rd.Text.Impl.Ot
{
  public enum OtOperationKind {
    Normal,
    Reset
  }

  // todo make it intrinsic
  public class OtOperation
  {
    public RdChangeOrigin Origin { get; }
    public int Timestamp { get; }
    public OtOperationKind Kind { get; }
    public IEnumerable<OtChange> Changes { get; }

    public OtOperation(IEnumerable<OtChange> changes, RdChangeOrigin origin, int timestamp, OtOperationKind kind)
    {
      Origin = origin;
      Timestamp = timestamp;
      Kind = kind;
      Changes = Normalize(changes); // todo eliminate for deserialization
    }

    private static IEnumerable<OtChange> Normalize(IEnumerable<OtChange> changes)
    {
      var result = new List<OtChange>();
      OtChange prev = null;

      foreach (var curr in changes)
      {
        if (curr.IsId()) continue;

        if (prev is Retain && curr is Retain)
        {
          prev = new Retain(((Retain) prev).Offset + ((Retain) curr).Offset);
        }
        else if (prev is InsertText && curr is InsertText)
        {
          prev = new InsertText(((InsertText) prev).Text + ((InsertText) curr).Text);
        }
        else if (prev is DeleteText && curr is DeleteText)
        {
          prev = new DeleteText(((DeleteText) prev).Text + ((DeleteText) curr).Text);
        }
        else
        {
          if (prev != null) result.Add(prev);
          prev = curr;
        }
      }

      if (prev != null) result.Add(prev);
      if (result.Count == 0) result.Add(new Retain(0));

      return result;
    }

    public int DocumentLengthBefore() => Changes.Sum(x => x.GetTextLengthBefore());

    public int DocumentLengthAfter() => Changes.Sum(x => x.GetTextLengthAfter());

    protected bool Equals(OtOperation other)
    {
      return Origin == other.Origin && Timestamp == other.Timestamp && Kind == other.Kind && Equals(Changes, other.Changes);
    }

    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != this.GetType()) return false;
      return Equals((OtOperation) obj);
    }

    public override int GetHashCode()
    {
      unchecked
      {
        var hashCode = (int) Origin;
        hashCode = (hashCode * 397) ^ Timestamp;
        hashCode = (hashCode * 397) ^ (int) Kind;
        hashCode = (hashCode * 397) ^ (Changes != null ? Changes.GetHashCode() : 0);
        return hashCode;
      }
    }

    public override string ToString()
    {
      var sb = new StringBuilder("OtOperation([");

      foreach (var change in Changes)
      {
        sb.Append(change);
        sb.Append(", ");
      }

      sb.Append($"], origin={Origin}, timestamp={Timestamp}, kind={Kind})");
      return sb.ToString();
    }
  }
}