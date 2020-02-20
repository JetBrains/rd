using System;
using System.Text;
using JetBrains.Lifetimes;
using NUnit.Framework;

namespace Test.Lifetimes.Lifetimes
{
  [TestFixture]
  public class SequentialLifetimesTest: LifetimesTestBase
  {
    [Test]
    public void TestSimple()
    {
      var sequence = new SequentialLifetimes(TestLifetime);
      var sb = new StringBuilder();
      var expected = new StringBuilder();

      const int max = 3;
      for (int i = 0; i < max; i++)
      {
        sb.AppendLine($"before {i}");
        sequence.Next(lifetime =>
        {
          var c = i;
          lifetime.Bracket(
            () => sb.AppendLine($"start {c}"),
            () => sb.AppendLine($"end {c}"));
          sb.AppendLine($"in {c}");
        });

        
        if (i == 0)
          expected.AppendLine($"before {i}");

        expected.AppendLine($"start {i}");
        expected.AppendLine($"in {i}");

        if (i != max - 1)
        {
          expected.AppendLine($"before {i+1}");
          expected.AppendLine($"end {i}");
        }
      }

      Assert.IsFalse(sequence.IsCurrentTerminated);
      Assert.AreEqual(expected.ToString(), sb.ToString());

      sequence.TerminateCurrent();
      Assert.IsTrue(sequence.IsCurrentTerminated);
      expected.AppendLine($"end {max - 1}");
      Assert.AreEqual(expected.ToString(), sb.ToString());
    }


    [Test]
    public void TestTerminateInNext()
    {
      var sequence = new SequentialLifetimes(TestLifetime);
      var sb = new StringBuilder();
      
      sequence.Next(lifetime =>
      {
        lifetime.Bracket(
          () => sb.AppendLine($"start"),
          () => sb.AppendLine($"end"));
        sb.AppendLine("Before terminate");
        sequence.TerminateCurrent();
        sb.AppendLine("After terminate");
      });

      Assert.IsTrue(sequence.IsCurrentTerminated);
      Assert.AreEqual("start\r\nBefore terminate\r\nend\r\nAfter terminate\r\n", sb.ToString());
    }

    [Test]
    public void TestFailedInNext()
    {
      var sequence = new SequentialLifetimes(TestLifetime);
      var sb = new StringBuilder();

      try
      {
        sequence.Next(lifetime =>
        {
          lifetime.Bracket(
            () => sb.AppendLine($"start"),
            () => sb.AppendLine($"end"));

          sb.AppendLine("Before exception");
          throw new Exception("Expected");
        });
      }
      catch (Exception e) when (e.Message == "Expected")
      {
        sb.AppendLine("Expected exception");
      }

      Assert.IsTrue(sequence.IsCurrentTerminated);
      Assert.AreEqual("start\r\nBefore exception\r\nend\r\nExpected exception\r\n", sb.ToString());
    }
  }
}