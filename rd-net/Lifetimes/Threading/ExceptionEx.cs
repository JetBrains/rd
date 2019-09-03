using System;
using JetBrains.Annotations;

namespace JetBrains.Threading
{
    public static class ExceptionEx
    {
        /// <summary>
        /// Whether this exception is <see cref="OperationCanceledException"/> or <see cref="Exception.InnerException"/> is <see cref="OperationCanceledException"/>
        /// (e.g. <c>"TargetInvocationException"</c>) or Inner.Inner and so on.
        /// Or this exception is <see cref="AggregateException"/> that consists only from <see cref="OperationCanceledException"/> (or exception that has OCE as Inner exception).
        /// </summary>
        /// <param name="exception">exception to test or null</param>
        /// <returns>if <paramref name="exception"/> is null, returns false. Otherwise tries to run algorithm from the summary.</returns>
        [PublicAPI] public static bool IsOperationCanceled([CanBeNull] this Exception exception)
        {
            switch (exception)
            {
                case null:
                    return false;
                case OperationCanceledException _:
                    return true;
                case AggregateException aggregate when aggregate.InnerExceptions.Count == 0:
                    return false;
                case AggregateException aggregate:
                {
                    // ReSharper disable once LoopCanBeConvertedToQuery
                    foreach(var inner in aggregate.InnerExceptions)
                    {
                        if (!inner.IsOperationCanceled())
                            return false;
                    }

                    //all inner exceptions are OCE
                    return true;
                }
                 
                default:
                    return exception.InnerException.IsOperationCanceled();
            }
        }
        
    }
}