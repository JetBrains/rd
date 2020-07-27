using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  public class UnsafeWriterTest
  {
    /*
    private static UnsafeWriter TryGetThreadStaticField()
    {
      var ourWriterField = typeof(UnsafeWriter).GetField("ourWriter", BindingFlags.Static | BindingFlags.GetField | BindingFlags.NonPublic);
      return (UnsafeWriter) ourWriterField.GetValue(null);
    }
    */

    public void TestWriterIsReused01()
    {
      UnsafeWriter firstWriter;
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        firstWriter = cookie.Writer;
      }

      UnsafeWriter secondWriter;
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        secondWriter = cookie.Writer;
      }

      Assert.IsTrue(ReferenceEquals(firstWriter, secondWriter), "object.ReferenceEquals(firstWriter, secondWriter)");
    }

    public void TestWriterIsReused02()
    {
      UnsafeWriter firstWriter;
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        firstWriter = cookie.Writer;
      }

      UnsafeWriter secondWriter;
      using (var cookie = UnsafeWriter.NewThreadLocalWriterWithCleanup())
      {
        secondWriter = cookie.Writer;
      }

      Assert.IsTrue(ReferenceEquals(firstWriter, secondWriter), "object.ReferenceEquals(firstWriter, secondWriter)");
    }

    public void TestWriterIsNotReused01()
    {
      UnsafeWriter firstWriter;
      using (var cookie = UnsafeWriter.NewThreadLocalWriterWithCleanup())
      {
        firstWriter = cookie.Writer;
      }

      UnsafeWriter secondWriter;
      using (var cookie = UnsafeWriter.NewThreadLocalWriterWithCleanup())
      {
        secondWriter = cookie.Writer;
      }

      Assert.IsFalse(ReferenceEquals(firstWriter, secondWriter), "object.ReferenceEquals(firstWriter, secondWriter)");
    }

    public void TestWriterIsNotReused02()
    {
      UnsafeWriter firstWriter;
      using (var cookie = UnsafeWriter.NewThreadLocalWriterWithCleanup())
      {
        firstWriter = cookie.Writer;
      }

      UnsafeWriter secondWriter;
      using (var cookie = UnsafeWriter.NewThreadLocalWriter())
      {
        secondWriter = cookie.Writer;
      }

      Assert.IsFalse(ReferenceEquals(firstWriter, secondWriter), "object.ReferenceEquals(firstWriter, secondWriter)");
    }
  }
}