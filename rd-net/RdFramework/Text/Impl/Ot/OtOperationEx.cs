using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Rd.Text.Impl.Intrinsics;
using JetBrains.Rd.Text.Intrinsics;

namespace JetBrains.Rd.Text.Impl.Ot
{
  public static class OtOperationEx
  {
    public static List<RdTextChange> ToRdTextChanges(this OtOperation op)
    {
      var documentLength = op.DocumentLengthBefore();
      var currOffset = 0;
      InsertText lastInsert = null;
      DeleteText lastDelete = null;
      var res = new List<RdTextChange>();

      foreach (var change in op.Changes)
      {
        switch (change)
        {
          case Retain _:
          {
            if (lastDelete != null || lastInsert != null)
            {
              res.Add(CreateTextChange(currOffset, lastInsert, lastDelete, documentLength));
              lastDelete = null;
              lastInsert = null;
            }

            break;
          }
          case InsertText i:
          {
            if (lastInsert != null)
            {
              res.Add(CreateTextChange(currOffset, lastInsert, lastDelete, documentLength));
              lastDelete = null;
            }

            documentLength += change.GetTextLengthAfter();
            lastInsert = i;
            break;
          }
          case DeleteText d:
          {
            if (lastDelete != null)
            {
              res.Add(CreateTextChange(currOffset, lastInsert, lastDelete, documentLength));
              lastInsert = null;
            }

            documentLength -= change.GetTextLengthBefore();
            lastDelete = d;
            break;
          }
        }

        currOffset += change.GetTextLengthAfter();
      }

      if (lastDelete != null || lastInsert != null)
        res.Add(CreateTextChange(currOffset, lastInsert, lastDelete, documentLength));

      return res;
    }

    private static RdTextChange CreateTextChange(int offset, [CanBeNull] InsertText insert, [CanBeNull] DeleteText delete,
      int documentLength)
    {
      var newText = insert?.Text ?? "";
      var oldText = delete?.Text ?? "";
      var startOffset = offset - (insert?.GetTextLengthAfter() ?? 0);

      RdTextChangeKind kind;
      if (insert != null && delete != null)
        kind = RdTextChangeKind.Replace;
      else if (insert != null)
        kind = RdTextChangeKind.Insert;
      else if (delete != null)
        kind = RdTextChangeKind.Remove;
      else
        throw new ArgumentOutOfRangeException();

      return new RdTextChange(kind, startOffset, oldText, newText, documentLength);
    }

    public static OtOperation ToOperation(this RdTextChange textChange, RdChangeOrigin origin, int ts)
    {
      var changes = new List<OtChange>();
      changes.Add(new Retain(textChange.StartOffset));
      switch (textChange.Kind)
      {
        case RdTextChangeKind.Insert:
          changes.Add(new InsertText(textChange.New));
          break;
        case RdTextChangeKind.Remove:
          changes.Add(new DeleteText(textChange.Old));
          break;
        case RdTextChangeKind.Replace:
        {
          changes.Add(new InsertText(textChange.New));
          changes.Add(new DeleteText(textChange.Old));
          break;
        }
        case RdTextChangeKind.Reset:
          changes.Add(new InsertText(textChange.New));
          break;
        default:
          throw new ArgumentOutOfRangeException();
      }

      var currentOffset = changes.Sum(x => x.GetTextLengthAfter());
      changes.Add(new Retain(textChange.FullTextLength - currentOffset));

      var kind = textChange.Kind == RdTextChangeKind.Reset ? OtOperationKind.Reset : OtOperationKind.Normal;
      var operation = new OtOperation(changes, origin, ts, kind);

      Assertion.Assert(operation.DocumentLengthAfter() == textChange.FullTextLength,
        "operation.DocumentLengthAfter() == textChange.FullTextLength");
      return operation;
    }

  }
}