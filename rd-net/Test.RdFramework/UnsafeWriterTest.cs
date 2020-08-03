using System.Threading;
using System.Threading.Tasks;
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
    [Test]
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

    [Test]
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

    [Test]
    public void TestWriterIsNotReused01()
    {
      var thread = new Thread(() =>
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
      });
      thread.Start();
      thread.Join();
    }

    [Test]
    public void TestWriterIsNotReused02()
    {
      var thread = new Thread(() =>
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
      });
      thread.Start();
      thread.Join();
    }
  }
}