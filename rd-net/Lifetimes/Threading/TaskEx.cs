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
        private static readonly Action<Task> ourNoAwaitContinuation = t =>
        {
          if (t.Exception != null && !t.Exception.IsOperationCanceled())
            Log.Root.Error(t.Exception);
        };


        /// <summary>
        ///   <para>Marks that this task is intentionally not awaited or continued-with.</para>
        ///   <para>The task is let to run, its return value (if any) is abandoned, its exceptions are consumed by our logger.</para>
        ///   <para>Prevents Compiler Warning (level 1) CS4014 “Because this call is not awaited, execution of the current method continues before the call is completed. Consider applying the 'await' operator to the result of the call.”</para>
        /// </summary>
        [PublicAPI] public static void NoAwait(this Task? task)
        {
            task?.ContinueWith(ourNoAwaitContinuation, 
              TaskContinuationOptions.ExecuteSynchronously | TaskContinuationOptions.NotOnRanToCompletion);
        }

        
        /// <summary>
        /// Transform this task into <see cref="ViewableProperty{T}"/>. Task could be finished by exception so we need
        /// </summary>
        /// <param name="task"></param>
        /// <typeparam name="T"></typeparam>
        /// <returns></returns>
        /// <exception cref="ArgumentNullException"></exception>
        [PublicAPI] public static IReadonlyProperty<Result<T>> ToResultProperty<T>(this Task<T> task)
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
        /// Waits for result of given task or throw <see cref="OperationCanceledException"/> 
        /// </summary>
        /// <param name="task">Task to wait</param>
        /// <param name="lifetime">Cancellation token for </param>
        /// <typeparam name="T"></typeparam>
        /// <returns><see cref="Task.Result"/> of <paramref name="task"/></returns>
        public static T GetOrWait<T>(this Task<T> task, Lifetime lifetime)
        {
            task.Wait(lifetime);
            return task.Result;
        }

        
        /// <summary>
        /// Return true only if task finished and finished with exception that is or consists only from <see cref="OperationCanceledException"/>.
        /// Allow to dive through all <see cref="AggregateException"/> and Inner exceptions.
        /// </summary>
        /// <param name="task"></param>
        /// <returns>true only if task finished and resulting exception matches <see cref="ExceptionEx.IsOperationCanceled"/></returns>
        public static bool IsOperationCanceled(this Task task)
        {
          return task.IsCanceled || task.Exception.IsOperationCanceled();
        }


        /// <summary>
        /// Transform result of original task right after it finished (with <see cref="Task.ConfigureAwait(bool)"/> == false).
        /// If task is not successfully finished then throw original exception. 
        /// </summary>
        /// <param name="task">original task</param>
        /// <param name="selector">transform function from original type to destination one</param>
        /// <typeparam name="TSrc">original type</typeparam>
        /// <typeparam name="TDst">destination type</typeparam>
        /// <returns>new task that is considered completed right after original task completes</returns>
        /// <exception cref="ArgumentNullException"></exception>
        [PublicAPI] public static async Task<TDst> Select<TSrc, TDst>(this Task<TSrc> task, Func<TSrc, TDst> selector)
        {
          if (task == null) 
            throw new ArgumentNullException(nameof(task));

          var res = await task.ConfigureAwait(false);
          return selector(res);
        }
    }
}