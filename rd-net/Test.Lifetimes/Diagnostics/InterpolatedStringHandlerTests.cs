#if !NET35
using System;
using System.Collections.Generic;
using System.Linq;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;
using JetBrains.Diagnostics.StringInterpolation;
using NUnit.Framework;

namespace Test.Lifetimes.Diagnostics;

public class InterpolatedStringHandlerTests
{
  [TestCase(true)]
  [TestCase(false)]
  public void JetConditionalInterpolatedStringHandlerIsEnabledTest(bool condition)
  {
    var handler = new JetConditionalInterpolatedStringHandler(1, 1, condition, out var isEnabled);
    Assert.AreNotEqual(condition, isEnabled);
    Assert.AreEqual(isEnabled, handler.IsEnabled);
  }
  
  [TestCase(null)]
  [TestCase(0)]
  public void JetNotNullConditionalInterpolatedStringHandlerIsEnabledTest([CanBeNull] object obj)
  {
    var handler = new JetNotNullConditionalInterpolatedStringHandler(1, 1, obj, out var isEnabled);
    Assert.AreEqual(obj == null, isEnabled);
    Assert.AreEqual(isEnabled, handler.IsEnabled);
  }
  
  [Test]
  public void JetLogLevelInterpolatedStringHandler()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    foreach (var loggingLevel in values)
    {
      var log = new MyTestLog(loggingLevel);

      foreach (var level in values)
      {
        var handler = new JetLogLevelInterpolatedStringHandler(1, 1, log, level, out var isEnabled);
        Assert.AreEqual(log.IsEnabled(level), isEnabled);
        Assert.AreEqual(isEnabled, handler.IsEnabled);
      }
    }
  }
  
  [Test]
  public void JetLogTraceInterpolatedStringHandler()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    foreach (var loggingLevel in values)
    {
      var log = new MyTestLog(loggingLevel);

      var handler = new JetLogTraceInterpolatedStringHandler(1, 1, log, out var isEnabled);
      Assert.AreEqual(loggingLevel == LoggingLevel.TRACE, isEnabled);
      Assert.AreEqual(isEnabled, handler.IsEnabled);
    }
  }

  [Test]
  public void AssertionsTest()
  {
    Assertion.Assert(true, $"{ThrowTestException()}");
    Assert.Throws<TestException>(() =>
    {
      Assertion.Assert(false, $"{ThrowTestException()}");
    });

    Assertion.AssertNotNull(0, $"{ThrowTestException()}");
    Assert.Throws<TestException>(() =>
    {
      Assertion.AssertNotNull(null, $"{ThrowTestException()}");
    });
    
    Assertion.Require(true, $"{ThrowTestException()}");
    Assert.Throws<TestException>(() =>
    {
      Assertion.Require(false, $"{ThrowTestException()}");
    });

    {
      var notNullValue = (object)0;
      var nullValue = (object)null;

      var _ = notNullValue.NotNull($"{ThrowTestException()}");
      Assert.Throws<TestException>(() =>
      {
        var _ = nullValue.NotNull($"{ThrowTestException()}");
      });
    }
    
    {
      var notNullValue = (int?)0;
      var nullValue = (int?)null;

      var _ = notNullValue.NotNull($"{ThrowTestException()}");
      Assert.Throws<TestException>(() =>
      {
        var _ = nullValue.NotNull($"{ThrowTestException()}");
      });
    }
  }

  [Test]
  public void LogTraceTest()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    const string message = "Trace enabled";
    
    foreach (var level in values)
    {
      var log = new MyTestLog(level);
      log.Trace($"{(level == LoggingLevel.TRACE ? message : ThrowTestException())}");

      var messages = log.GetMessages();
      if (level == LoggingLevel.TRACE)
      {
        Assert.AreEqual(1, messages.Count);
        Assert.AreEqual(message, messages.Single().FormattedMessage);
      }
      else
      {
        Assert.AreEqual(0, messages.Count);
      }
    }
  }

  private static string ThrowTestException() => throw new TestException();

  private class TestException : Exception
  {
  }
  
  private class InvalidLogLevelExceptionException : Exception
  {
  }

  
  private class MyTestLog : LogBase
  {
    private readonly List<LeveledMessage> myMessages = new();

    public List<LeveledMessage> GetMessages()
    {
      var copy = myMessages.ToList();
      myMessages.Clear();
      return copy;
    }

    public MyTestLog(LoggingLevel level) : base("", level)
    {
      Handlers += message =>
      {
        myMessages.Add(message);
      };
    }

    public override void Log(LoggingLevel level, string? message, Exception? exception = null)
    {
      if (!IsEnabled(level))
        throw new InvalidLogLevelExceptionException();
      base.Log(level, message, exception);
    }

    protected override string Format(LoggingLevel level, string? message, Exception? exception)
    {
      if (exception != null)
        throw new Exception("Unexpected exception", exception);

      return message;
    }
  }
}
#endif
