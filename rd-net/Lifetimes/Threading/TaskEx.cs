using System;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Collections.Viewable;
using JetBrains.Core;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;

namespace JetBrains.Threading
{
    public static class TaskEx
    {
        /// <summary>
        ///   <para>Marks that this task is intentionally not awaited or continued-with.</para>
        ///   <para>The task is let to run, its return value (if any) is abandoned, its exceptions are consumed by our logger.</para>
        ///   <para>Prevents Compiler Warning (level 1) CS4014 “Because this call is not awaited, execution of the current method continues before the call is completed. Consider applying the 'await' operator to the result of the call.”</para>
        /// </summary>
        [PublicAPI] public static void NoAwait([CanBeNull] this Task task)
        {
            task?.ContinueWith
            (t =>
            {
                if(t.Exception != null && !t.Exception.IsOperationCanceled())
                    Log.Root.Error(t.Exception);
            }, TaskContinuationOptions.ExecuteSynchronously);
        }

        
        /// <summary>
        /// Transform this task into <see cref="ViewableProperty{T}"/>. Task could be finished by exception so we need
        /// </summary>
        /// <param name="task"></param>
        /// <typeparam name="T"></typeparam>
        /// <returns></returns>
        /// <exception cref="ArgumentNullException"></exception>
        [PublicAPI] public static IReadonlyProperty<Result<T>> ToResultProperty<T>([NotNull] this Task<T> task)
        {
            if (task == null) throw new ArgumentNullException(nameof(task));

            var res = new ViewableProperty<Result<T>>();
            task.ContinueWith(t =>
            {
                res.Value = Result.FromCompletedTask(t);
            }, TaskContinuationOptions.ExecuteSynchronously);

            return res;

            
        }


        /// <summary>
        /// Get exception 
        /// </summary>
        /// <param name="task"></param>
        /// <param name="lifetime"></param>
        /// <typeparam name="T"></typeparam>
        /// <returns></returns>
        public static T GetOrWait<T>([NotNull] this Task<T> task, Lifetime lifetime)
        {
            task.Wait(lifetime);
            return task.Result;
        }
    }
}