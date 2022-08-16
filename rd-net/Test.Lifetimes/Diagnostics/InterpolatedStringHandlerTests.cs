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
  [Test]
  public void TestWithAlignment()
  {
    var handler = new JetDefaultInterpolatedStringHandler();
    handler.AppendFormatted("foo", 10);
    var s1 = handler.ToStringAndClear();
    
    handler = new JetDefaultInterpolatedStringHandler();
    handler.AppendFormatted("foo", -10);
    var s2 = handler.ToStringAndClear();

    Assert.AreEqual("       foo", s1);
    Assert.AreEqual("foo       ", s2);
    
    handler = new JetDefaultInterpolatedStringHandler();
    handler.AppendLiteral("test");
    handler.AppendFormatted("foo", 5);
    Assert.AreEqual("test  foo", handler.ToStringAndClear());
  }

  [TestCase("foo", null, "foo", 0)]
  [TestCase("barfoo", "bar", "foo", -1)]
  [TestCase("barfoo", "bar", "foo", 1)]
  public void MoreAlignmentTests(string expected, [CanBeNull] string initial, string value, int alignment)
  {
    var handler = new JetDefaultInterpolatedStringHandler();
    handler.AppendLiteral(initial);
    handler.AppendFormatted(value, alignment);
    Assert.AreEqual(expected, handler.ToStringAndClear());

    // Reuse:
    handler.AppendFormatted(1); // value of another type
    handler.AppendLiteral(initial);
    handler.AppendFormatted(value, alignment);
    Assert.AreEqual("1" + expected, handler.ToStringAndClear());
  }
  
  [TestCase("c430b4ba-b2da-400a-bf4f-55419d557497", "C430B4BA-B2DA-400A-BF4F-55419D557497", null, 0)]
  [TestCase("0b7964d370a1455ea87ad0930faac164", "0B7964D3-70a1-455E-A87A-D0930FAAC164", "N", 0)]
  [TestCase("{92b0d81c-30e7-4c48-be86-30deafcb77b6}", "92B0D81C-30E7-4C48-BE86-30DEAFCB77B6", "B", 0)]
  [TestCase("    c430b4ba-b2da-400a-bf4f-55419d557497", "C430B4BA-B2DA-400A-BF4F-55419D557497", null, 40)]
  [TestCase("0b7964d370a1455ea87ad0930faac164", "0B7964D3-70a1-455E-A87A-D0930FAAC164", "N", 10)]
  [TestCase("{92b0d81c-30e7-4c48-be86-30deafcb77b6}  ", "92B0D81C-30E7-4C48-BE86-30DEAFCB77B6", "B", -40)]
  public void FormatterTests(string expected, string value, string format, int alignment)
  {
    var handler = new JetDefaultInterpolatedStringHandler();
    handler.AppendLiteral("GUID: ");
    var guid = Guid.Parse(value);
    handler.AppendFormatted(guid, alignment, format);
    Assert.AreEqual("GUID: " + expected, handler.ToStringAndClear());
  }
  
  private class ToStringIsNull
  {
    public override string ToString() => null!;
  }

  [Test]
  public void NullFormatterCase()
  {
    var handler = new JetDefaultInterpolatedStringHandler();
    handler.AppendFormatted(new ToStringIsNull(), 5);
    Assert.AreEqual("     ", handler.ToStringAndClear());
  }

  [Test]
  public void ToStringTests()
  {
    var handler = new JetDefaultInterpolatedStringHandler();
    Assert.AreEqual("", handler.ToString());

    handler.AppendLiteral("111");
    Assert.AreEqual("111", handler.ToString());
    handler.AppendLiteral("222");
    Assert.AreEqual("111222", handler.ToString());
    Assert.AreEqual("111222", handler.ToStringAndClear());
    Assert.AreEqual("", handler.ToString());
  }
  
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
  public void JetLogVerboseInterpolatedStringHandler()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    foreach (var loggingLevel in values)
    {
      var log = new MyTestLog(loggingLevel);

      var handler = new JetLogVerboseInterpolatedStringHandler(1, 1, log, out var isEnabled);
      Assert.AreEqual(loggingLevel >= LoggingLevel.VERBOSE, isEnabled);
      Assert.AreEqual(isEnabled, handler.IsEnabled);
    }
  }

  [Test]
  public void JetLogInfoInterpolatedStringHandler()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    foreach (var loggingLevel in values)
    {
      var log = new MyTestLog(loggingLevel);

      var handler = new JetLogInfoInterpolatedStringHandler(1, 1, log, out var isEnabled);
      Assert.AreEqual(loggingLevel >= LoggingLevel.INFO, isEnabled);
      Assert.AreEqual(isEnabled, handler.IsEnabled);
    }
  }
  
  [Test]
  public void JetLogWarnInterpolatedStringHandler()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    foreach (var loggingLevel in values)
    {
      var log = new MyTestLog(loggingLevel);

      var handler = new JetLogWarnInterpolatedStringHandler(1, 1, log, out var isEnabled);
      Assert.AreEqual(loggingLevel >= LoggingLevel.WARN, isEnabled);
      Assert.AreEqual(isEnabled, handler.IsEnabled);
    }
  }
  
  [Test]
  public void JetLogErrorInterpolatedStringHandler()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    foreach (var loggingLevel in values)
    {
      var log = new MyTestLog(loggingLevel);

      var handler = new JetLogErrorInterpolatedStringHandler(1, 1, log, out var isEnabled);
      Assert.AreEqual(loggingLevel >= LoggingLevel.ERROR, isEnabled);
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
  
  [Test]
  public void LogVerboseTest()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    const string message = "Verbose enabled";
    
    foreach (var level in values)
    {
      var log = new MyTestLog(level);
      log.Verbose($"{(level >= LoggingLevel.VERBOSE ? message : ThrowTestException())}");

      var messages = log.GetMessages();
      if (level >= LoggingLevel.VERBOSE)
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
  
  [Test]
  public void LogInfoTest()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    const string message = "Info enabled";
    
    foreach (var level in values)
    {
      var log = new MyTestLog(level);
      log.Info($"{(level >= LoggingLevel.INFO ? message : ThrowTestException())}");

      var messages = log.GetMessages();
      if (level >= LoggingLevel.INFO)
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
  
  [Test]
  public void LogWarnTest()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    const string message = "Warn enabled";
    
    foreach (var level in values)
    {
      var log = new MyTestLog(level);
      log.Warn($"{(level >= LoggingLevel.WARN ? message : ThrowTestException())}");

      var messages = log.GetMessages();
      if (level >= LoggingLevel.WARN)
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
  
  [Test]
  public void LogErrorTest()
  {
    var values = (LoggingLevel[])Enum.GetValues(typeof(LoggingLevel));
    const string message = "Error enabled";
    
    foreach (var level in values)
    {
      var log = new MyTestLog(level);
      log.Error($"{(level >= LoggingLevel.ERROR ? message : ThrowTestException())}");

      var messages = log.GetMessages();
      if (level >= LoggingLevel.ERROR)
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
