using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Diagnostics;

namespace JetBrains.Rd.Text.Impl.Ot
{
  public struct OtTransformResult
  {
    public readonly OtOperation NewLocalDiff;
    public readonly OtOperation LocalizedApplyToDocument;

    public OtTransformResult(OtOperation newLocalDiff, OtOperation localizedApplyToDocument)
    {
      NewLocalDiff = newLocalDiff;
      LocalizedApplyToDocument = localizedApplyToDocument;
    }
  }

  public static class OtFramework
  {
/*
    public static OtOperation Compose(this OtOperation o1, OtOperation o2)
    {
      var after = o1.DocumentLengthAfter();
      var before = o2.DocumentLengthBefore();

      Assertion.Assert(after == before, "after == before");
      Assertion.Assert(o1.Origin == o2.Origin, "o1.Role == o2.Role");
      Assertion.Assert(o1.Timestamp == o2.Timestamp, "o1.Timestamp == o2.Timestamp");
      Assertion.Assert(o1.Kind == o2.Kind, "o1.Kind == o2.Kind");


      var ops1 = new Stack<OtChange>(Enumerable.Reverse(o1.Changes));
      var ops2 = new Stack<OtChange>(Enumerable.Reverse(o2.Changes));
      var acc = new List<OtChange>();

      while (true)
      {
        var op1 = ops1.Count != 0 ? ops1.Peek() : null;
        var op2 = ops2.Count != 0 ? ops2.Peek() : null;

        if (op1 == null && op2 == null)
          break;

        if (op1 != null && op2 == null)
        {
          acc.Add(op1);
          ops1.Pop();
          continue;
        }

        if (op1 == null && op2 != null)
        {
          acc.Add(op2);
          ops2.Pop();
          continue;
        }

        switch (op1)
        {
          case DeleteText d1 when op2 is InsertText d2 && d1.Text == d2.Text:
          {
            var length = d1.Text.Length;
            acc.Add(new Retain(length));
            ops1.Pop();
            ops2.Pop();
            break;
          }
          case OtChange _ when op2 is InsertText:
          {
            acc.Add(op2);
            ops2.Pop();
            break;
          }
          case DeleteText _ when op2 != null:
          {
            acc.Add(op1);
            ops1.Pop();
            break;
          }
          case Retain r1 when op2 is Retain r2:
          {
            var offset1 = r1.Offset;
            var offset2 = r2.Offset;
            if (offset1 > offset2)
            {
              acc.Add(op2);
              ops1.Pop();
              ops2.Pop();
              ops1.Push(new Retain(offset1 - offset2));
            }
            else if (offset1 == offset2)
            {
              acc.Add(op1);
              ops1.Pop();
              ops2.Pop();
            }
            else if (offset1 < offset2)
            {
              acc.Add(op1);
              ops1.Pop();
              ops2.Pop();
              ops2.Push(new Retain(offset2 - offset1));
            }

            break;
          }
          case InsertText i1 when op2 is DeleteText d2:
          {
            var text1 = i1.Text;
            var text2 = d2.Text;
            if (text1.Length > text2.Length)
            {
              Assertion.Assert(text1.StartsWith(text2), "text1.StartsWith(text2)");
              ops1.Pop();
              ops2.Pop();
              ops1.Push(new InsertText(text1.Substring(text2.Length)));
            }
            else if (text1.Length == text2.Length)
            {
              Assertion.Assert(text1 == text2, "text1 == text2");
              ops1.Pop();
              ops2.Pop();
            }
            else if (text1.Length < text2.Length)
            {
              Assertion.Assert(text2.StartsWith(text1), "text2.StartsWith(text1)");
              ops1.Pop();
              ops2.Pop();
              ops2.Push(new DeleteText(text2.Substring(text1.Length)));
            }

            break;
          }
          case InsertText i1 when op2 is Retain r2:
          {
            var text1 = i1.Text;
            var offset2 = r2.Offset;
            if (text1.Length > offset2)
            {
              acc.Add(new InsertText(text1.Substring(0, offset2)));
              ops1.Pop();
              ops2.Pop();
              ops1.Push(new InsertText(text1.Substring(offset2)));
            }
            else if (text1.Length == offset2)
            {
              acc.Add(op1);
              ops1.Pop();
              ops2.Pop();
            }
            else if (text1.Length < offset2)
            {
              acc.Add(op1);
              ops1.Pop();
              ops2.Pop();
              ops2.Push(new Retain(offset2 - text1.Length));
            }

            break;
          }
          case Retain r1 when op2 is DeleteText d2:
          {
            var offset1 = r1.Offset;
            var text2 = d2.Text;
            if (offset1 > text2.Length)
            {
              acc.Add(op2);
              ops1.Pop();
              ops2.Pop();
              ops1.Push(new Retain(offset1 - text2.Length));
            }
            else if (offset1 == text2.Length)
            {
              acc.Add(op2);
              ops1.Pop();
              ops2.Pop();
            }
            else if (offset1 < text2.Length)
            {
              acc.Add(new DeleteText(text2.Substring(0, offset1)));
              ops1.Pop();
              ops2.Pop();
              ops2.Push(new DeleteText(text2.Substring(offset1)));
            }

            break;
          }
          default: throw new ArgumentOutOfRangeException($"Not matched pair: (op1 = {op1}, op2 = {op2})");
        }
      }

      return new OtOperation(acc, o1.Origin, o1.Timestamp, o1.Kind);
    }
*/

    // Ressel’s transformation function
    public static OtTransformResult Transform(OtOperation localDiff, OtOperation remoteApplyToDocument)
    {
      Assertion.Assert(localDiff.DocumentLengthBefore() == remoteApplyToDocument.DocumentLengthBefore(),
        "localDiff.DocumentLengthBefore() == remoteApplyToDocument.DocumentLengthBefore()");
      Assertion.Assert(localDiff.Origin != remoteApplyToDocument.Origin, "localDiff.Role != remoteApplyToDocument.Role");
      Assertion.Assert(localDiff.Kind == OtOperationKind.Normal && remoteApplyToDocument.Kind == OtOperationKind.Normal,
        "localDiff.Kind == OtOperationKind.Normal && remoteApplyToDocument.Kind == OtOperationKind.Normal");

      var resOp1 = new List<OtChange>();
      var resOp2 = new List<OtChange>();
      var ops1 = new Stack<OtChange>(Enumerable.Reverse(localDiff.Changes));
      var ops2 = new Stack<OtChange>(Enumerable.Reverse(remoteApplyToDocument.Changes));

      while (true)
      {
        var op1 = ops1.Count != 0 ? ops1.Peek() : null;
        var op2 = ops2.Count != 0 ? ops2.Peek() : null;

        if (op1 == null && op2 == null)
          break;

        if (op1 == null && op2 != null)
        {
          var offset = op2.GetTextLengthAfter();
          if (offset > 0)
          {
            resOp1.Add(new Retain(offset));
            resOp2.Add(op2);
            ops2.Pop();
          }
          else
          {
            resOp2.Add(op2);
            ops2.Pop();
          }

          continue;
        }

        if (op1 != null && op2 == null)
        {
          var offset = op1.GetTextLengthAfter();
          if (offset > 0)
          {
            resOp1.Add(op1);
            resOp2.Add(new Retain(offset));
            ops1.Pop();
          }
          else
          {
            resOp2.Add(op1);
            ops1.Pop();
          }

          continue;
        }


        switch (op1)
        {
          case InsertText i1 when op2 is InsertText i2:
          {
            if (localDiff.Origin < remoteApplyToDocument.Origin)
            {
              resOp1.Add(i1);
              resOp2.Add(new Retain(i1.Text.Length));
              ops1.Pop();
            }
            else
            {
              resOp1.Add(new Retain(i2.Text.Length));
              resOp2.Add(op2);
              ops2.Pop();
            }

            break;
          }
          case InsertText i1:
          {
            resOp1.Add(i1);
            resOp2.Add(new Retain(i1.Text.Length));
            ops1.Pop();
            break;
          }
          case var _ when op2 is InsertText i2:
          {
            resOp1.Add(new Retain(i2.Text.Length));
            resOp2.Add(i2);
            ops2.Pop();
            break;
          }
          case Retain r1 when op2 is Retain r2:
          {
            var offset1 = r1.Offset;
            var offset2 = r2.Offset;
            if (offset1 > offset2)
            {
              resOp1.Add(op2);
              resOp2.Add(op2);
              ops1.Pop();
              ops2.Pop();
              ops1.Push(new Retain(offset1 - offset2));
            }
            else if (offset1 == offset2)
            {
              resOp1.Add(op1);
              resOp2.Add(op1);
              ops1.Pop();
              ops2.Pop();
            }
            else if (offset1 < offset2)
            {
              resOp1.Add(op1);
              resOp2.Add(op1);
              ops1.Pop();
              ops2.Pop();
              ops2.Push(new Retain(offset2 - offset1));
            }

            break;
          }
          case DeleteText d1 when op2 is DeleteText d2:
          {
            var text1 = d1.Text;
            var text2 = d2.Text;
            if (text1.Length > text2.Length)
            {
              ops1.Pop();
              ops2.Pop();
              ops1.Push(new DeleteText(text1.Substring(text2.Length)));
            }
            else if (text1.Length == text2.Length)
            {
              Assertion.Assert(text1 == text2, "text1 == text2");
              ops1.Pop();
              ops2.Pop();
            }
            else if (text1.Length < text2.Length)
            {
              ops1.Pop();
              ops2.Pop();
              ops2.Push(new DeleteText(text2.Substring(text1.Length)));
            }

            break;
          }
          case DeleteText d1 when op2 is Retain r2:
          {
            var text1 = d1.Text;
            var offset2 = r2.Offset;
            if (text1.Length > offset2)
            {
              resOp1.Add(new DeleteText(text1.Substring(0, offset2)));
              ops1.Pop();
              ops2.Pop();
              ops1.Push(new DeleteText(text1.Substring(offset2)));
            }
            else if (text1.Length == offset2)
            {
              resOp1.Add(op1);
              ops1.Pop();
              ops2.Pop();
            }
            else if (text1.Length < offset2)
            {
              resOp1.Add(op1);
              ops1.Pop();
              ops2.Pop();
              ops2.Push(new Retain(offset2 - text1.Length));
            }

            break;
          }
          case Retain r1 when op2 is DeleteText r2:
          {
            var offset1 = r1.Offset;
            var text2 = r2.Text;
            if (offset1 > text2.Length)
            {
              resOp2.Add(op2);
              ops1.Pop();
              ops2.Pop();
              ops1.Push(new Retain(offset1 - text2.Length));
            }
            else if (offset1 == text2.Length)
            {
              resOp2.Add(op2);
              ops1.Pop();
              ops2.Pop();
            }
            else if (offset1 < text2.Length)
            {
              resOp2.Add(new DeleteText(text2.Substring(0, offset1)));
              ops1.Pop();
              ops2.Pop();
              ops2.Push(new DeleteText(text2.Substring(offset1)));
            }
            break;
          }
          default: throw new ArgumentOutOfRangeException($"Not matched pair: (op1 = {op1}, op2 = {op2})");
        }
      }

      return new OtTransformResult(
        new OtOperation(resOp1, localDiff.Origin, localDiff.Timestamp, OtOperationKind.Normal),
        new OtOperation(resOp2, remoteApplyToDocument.Origin, remoteApplyToDocument.Timestamp, OtOperationKind.Normal));
    }
  }
}