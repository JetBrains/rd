using System.Linq;
using System.Threading;
using JetBrains.Serialization;
using NUnit.Framework;

namespace Test.RdFramework
{
  [TestFixture]
  public class UnsafeWriterTest
  {
    [Test]
    public void TestWriterIsReused01()
    {
      UnsafeWriter firstWriter = null, secondWriter = null;
      var thread = new Thread(() =>
      {
        using (var cookie = UnsafeWriter.NewThreadLocalWriter())
        {
          firstWriter = cookie.Writer;
        }

        using (var cookie = UnsafeWriter.NewThreadLocalWriter())
        {
          secondWriter = cookie.Writer;
        }
      });
      thread.Start();
      thread.Join();
      Assert.IsNotNull(firstWriter, "firstWriter != null");
      Assert.IsNotNull(secondWriter, "secondWriter != null");
      Assert.IsTrue(ReferenceEquals(firstWriter, secondWriter), "object.ReferenceEquals(firstWriter, secondWriter)");
    }

    [Test]
    public void TestNullWriterInCookie()
    {
      using (var cookie = new UnsafeWriter.Cookie())
      {
      }
    }
  }
}