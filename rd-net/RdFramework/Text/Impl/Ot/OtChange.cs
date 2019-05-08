using JetBrains.Annotations;
using JetBrains.Diagnostics;

namespace JetBrains.Rd.Text.Impl.Ot
{
  public abstract class OtChange
  {
    public abstract bool IsId();
    public abstract OtChange Invert();
    public abstract int GetTextLengthBefore();
    public abstract int GetTextLengthAfter();
  }

  public class Retain : OtChange
  {
    public int Offset { get; }

    public Retain(int offset)
    {
      Assertion.Assert(offset >= 0, "offset >= 0");
      Offset = offset;
    }

    public override bool IsId() => Offset == 0;

    public override OtChange Invert() => this;

    public override int GetTextLengthBefore() => Offset;

    public override int GetTextLengthAfter() => Offset;

    public override string ToString() => $"Retain({Offset})";

    protected bool Equals(Retain other)
    {
      return Offset == other.Offset;
    }

    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != this.GetType()) return false;
      return Equals((Retain) obj);
    }

    public override int GetHashCode()
    {
      return Offset;
    }
  }

  public class InsertText : OtChange
  {
    public string Text { get; }

    public InsertText([NotNull] string text) => Text = text;

    public override bool IsId() => Text.Length == 0;

    public override OtChange Invert() => new DeleteText(Text);

    public override int GetTextLengthBefore() => 0;

    public override int GetTextLengthAfter() => Text.Length;

    public override string ToString() => $"InsertText(\"{Text}\")";

    protected bool Equals(InsertText other)
    {
      return string.Equals(Text, other.Text);
    }

    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != this.GetType()) return false;
      return Equals((InsertText) obj);
    }

    public override int GetHashCode()
    {
      return (Text != null ? Text.GetHashCode() : 0);
    }
  }

  public class DeleteText : OtChange
  {
    public string Text { get; }

    public DeleteText(string text) => Text = text;

    public override bool IsId() => Text.Length == 0;

    public override OtChange Invert() => new InsertText(Text);

    public override int GetTextLengthBefore() => Text.Length;

    public override int GetTextLengthAfter() => 0;

    public override string ToString() => $"DeleteText(\"{Text}\")";

    protected bool Equals(DeleteText other)
    {
      return string.Equals(Text, other.Text);
    }

    public override bool Equals(object obj)
    {
      if (ReferenceEquals(null, obj)) return false;
      if (ReferenceEquals(this, obj)) return true;
      if (obj.GetType() != this.GetType()) return false;
      return Equals((DeleteText) obj);
    }

    public override int GetHashCode()
    {
      return (Text != null ? Text.GetHashCode() : 0);
    }
  }
}