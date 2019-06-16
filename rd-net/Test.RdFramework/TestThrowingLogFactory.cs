using System;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;

namespace Test.RdFramework
{
    internal class TestThrowingLogFactory : SingletonLogFactory
    {
        public TestThrowingLogFactory() : base(new TestThrowingLogger())
        {
        }
    }

    internal class TestThrowingLogger : ILog
    {
        public string Category { get { return ""; } }

        public bool IsEnabled(LoggingLevel level) { return level == LoggingLevel.ERROR; }

        public void Log(LoggingLevel level, string message, Exception exception = null)
        {
            if (level == LoggingLevel.ERROR && exception != null) throw exception;
        }
    }
}