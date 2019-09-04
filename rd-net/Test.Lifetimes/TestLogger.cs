using System;
using System.Collections.Generic;
using System.Threading;
using JetBrains.Annotations;
using JetBrains.Diagnostics;
using JetBrains.Diagnostics.Internal;

namespace Test.Lifetimes
{
    public class TestLogger : LogBase
    {
        public static readonly TestLogger Logger = new TestLogger("Tests");
        public static readonly ILogFactory Factory = new TestLogFactory();
        
        private readonly object myMonitor = new object();
        private readonly List<Exception> myExceptions = new List<Exception>();
        
        private TestLogger([NotNull] string category) : base(category)
        {
        }

        protected override string Format(LoggingLevel level, string message, Exception exception)
        {
            if (level.IsSeriousError())
            {
                lock (myMonitor)
                {
                    myExceptions.Add(exception ?? new Exception(message));
                }
            }

            return JetBrains.Diagnostics.Log.DefaultFormat(
                DateTime.Now,
                level, 
                Category, 
                Thread.CurrentThread, 
                message,
                exception);
        }

        [CanBeNull]
        private CompoundException RecycleLoggedExceptions()
        {
            lock (myMonitor)
            {
                if (myExceptions.Count == 0) return null;
                
                var exception = new CompoundException(myExceptions);
                myExceptions.Clear();
                
                return exception;
            }
        }

        public void ThrowLoggedExceptions()
        {
            var result = RecycleLoggedExceptions();
            if (result != null) throw result;
        }

        private class TestLogFactory : ILogFactory
        {
            public ILog GetLog(string category) => Logger;
        }
    }
}