using System.Collections.Generic;
using JetBrains.Rd.Text.Impl.Intrinsics;
using JetBrains.Rd.Text.Impl.Ot;
using NUnit.Framework;

namespace Test.RdFramework.TextBuffer
{
  [TestFixture]
  [Apartment(System.Threading.ApartmentState.STA)]
  public class OtOperationTest
  {
    public static readonly object[] NormalizeTestData =
    {
      new object[] { new List<OtChange> {new Retain(0)}, new List<OtChange> {new Retain(0)} },
      new object[] { new List<OtChange> {new InsertText("abc"), new Retain(0)}, new List<OtChange> {new InsertText("abc")} },
      new object[] { new List<OtChange> {new Retain(2), new Retain(3), new Retain(1)}, new List<OtChange> {new Retain(6)} },
      new object[] { new List<OtChange> {new Retain(2), new Retain(3), new InsertText("abc"), new InsertText("def")}, new List<OtChange> {new Retain(5), new InsertText("abcdef")} },
      new object[] { new List<OtChange> {new Retain(1), new DeleteText("abc"), new DeleteText("def")}, new List<OtChange> {new Retain(1), new DeleteText("abcdef")} },
      new object[] { new List<OtChange> {new Retain(1), new DeleteText("abc"), new InsertText("def")}, new List<OtChange> {new Retain(1), new DeleteText("abc"), new InsertText("def")} },
      new object[] { new List<OtChange> {new Retain(1), new DeleteText("abc"), new DeleteText("def"), new InsertText("ghi")}, new List<OtChange> {new Retain(1), new DeleteText("abcdef"), new InsertText("ghi")} },
      new object[] { new List<OtChange> {new Retain(1), new Retain(2), new InsertText("q"), new DeleteText("abc"), new DeleteText("def"), new InsertText("ghi")}, new List<OtChange> {new Retain(3), new InsertText("q"), new DeleteText("abcdef"), new InsertText("ghi")} },
    };

    [Test, TestCaseSource(nameof(NormalizeTestData))]
    public void TestNormalize(List<OtChange> originalChanges, List<OtChange> normalizedChanges)
    {
      var op = new OtOperation(originalChanges, RdChangeOrigin.Slave, 0, OtOperationKind.Normal);
      Assert.AreEqual(normalizedChanges, op.Changes);
    }
  }
}