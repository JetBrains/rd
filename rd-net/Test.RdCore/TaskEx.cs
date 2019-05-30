using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Diagnostics;

namespace Test.RdCore
{
    public static class TaskEx
    {
        /// <summary>
        ///   <para>Marks that this task is intentionally not awaited or continued-with.</para>
        ///   <para>The task is let to run, its return value (if any) is abandoned, its exceptions are consumed by our logger.</para>
        ///   <para>Prevents Compiler Warning (level 1) CS4014 “Because this call is not awaited, execution of the current method continues before the call is completed. Consider applying the 'await' operator to the result of the call.”</para>
        /// </summary>
        public static void NoAwait([CanBeNull] this Task task)
        {
            task?.ContinueWith
            (t =>
            {
                if(t.Exception != null && !t.Exception.IsOperationCanceled())
                    Log.Root.Error(t.Exception);
            });
        }
    }
}