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
    public void ReportReentrancy01()
    {
      var thread = new Thread(() =>
      {
        UnsafeWriterStatistics.ClearEvents();
        using (var cookie = UnsafeWriter.NewThreadLocalWriter())
        {
          using (var nestedCookie = UnsafeWriter.NewThreadLocalWriter())
            cookie.Writer.Write(0);
        }
      });
      thread.Start();
      thread.Join();
      var reentrancyEvent = UnsafeWriterStatistics.GetEvents().FirstOrDefault(@event => @event.Type == UnsafeWriterStatistics.EventType.REENTRANCY);
      Assert.IsNull(reentrancyEvent);
    }

    [Test]
    public void ReportReentrancy02()
    {
      var thread = new Thread(() =>
      {
        UnsafeWriterStatistics.ClearEvents();
        UnsafeWriterStatistics.ReportReentrancy = true;
        try
        {
          using (var cookie = UnsafeWriter.NewThreadLocalWriter())
          {
            using (var nestedCookie = UnsafeWriter.NewThreadLocalWriter())
              cookie.Writer.Write(0);
          }
        }
        finally
        {
          UnsafeWriterStatistics.ReportReentrancy = false;
        }
      });
      thread.Start();
      thread.Join();
      var reentrancyEvent = UnsafeWriterStatistics.GetEvents().First(@event => @event.Type == UnsafeWriterStatistics.EventType.REENTRANCY);
      Assert.IsTrue(reentrancyEvent.Stacktraces.Count == 2, "reentrancyEvent.Stacktraces.Count == 2");
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