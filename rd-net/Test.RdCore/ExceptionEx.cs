using System;
using JetBrains.Annotations;

namespace Test.RdCore
{
    public static class ExceptionEx
    {
        /// <summary>
        /// Try to find out OperationCanceledException in AggregateException. If no PCE but some other exception exist, return it.
        /// </summary>
        /// <param name="toUpdate">Return value that can be updated based on possibleAggregateException</param>
        /// <param name="possibleAggregateException">AggregateException or null</param>
        [PublicAPI]
        public static void ParseAggregateException([CanBeNull] ref Exception toUpdate, [CanBeNull] AggregateException possibleAggregateException)
        {
            if(possibleAggregateException == null)
                return;

            foreach(Exception ex in possibleAggregateException.Flatten().InnerExceptions)
            {
                if(ex != null && (toUpdate == null || toUpdate is OperationCanceledException))
                    toUpdate = ex; //need to visit all exceptions
            }
      
      
        }

        /// <summary>
        /// Whether this exception is <see cref="OperationCanceledException"/> or transitively <see cref="Exception.InnerException"/> is <see cref="OperationCanceledException"/>
        /// or this exception is <see cref="AggregateException"/> that consists only from <see cref="OperationCanceledException"/> (or exception that  
        /// </summary>
        /// <param name="exception"></param>
        /// <returns>if <paramref name="exception"/> is null, returns false. Else  </returns>
        public static bool IsOperationCanceled([CanBeNull] this Exception exception)
        {
            if (exception == null)
                return false;
      
            if(exception is OperationCanceledException)
                return true;
      

            Exception ex = null;
            ParseAggregateException(ref ex, exception as AggregateException);
            if (ex is OperationCanceledException)
                return true;

            //ex ?? exception is guaranteed not OCE nor null
            return (ex ?? exception).InnerException.IsOperationCanceled();
        }
    }
}