using System;
using System.Runtime.CompilerServices;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Lifetimes;

namespace JetBrains.Threading
{
    /// <summary>
    /// Holder that starts evaluation immediately right in the constructor in background. 
    /// </summary>
    /// <typeparam name="T"></typeparam>
    public class ProactiveLazy<T>
    {
        [NotNull] private readonly Task<T> myTask; //todo Not very footprint-friendly. Need to clear task after execution.

        public ProactiveLazy(Lifetime lifetime, Func<T> factory, TaskScheduler taskScheduler = null)
        {
            myTask = Task.Factory.StartNew(factory, lifetime, TaskCreationOptions.None, taskScheduler ?? TaskScheduler.Default);
        }

        [PublicAPI] public T GetOrWait() => GetOrWait(Lifetime.Eternal);

        [PublicAPI]
        public T GetOrWait(Lifetime lifetime) => myTask.GetOrWait(lifetime);


        [PublicAPI] public TaskAwaiter<T> GetAwaiter() => myTask.GetAwaiter();

        [PublicAPI] public Task<T> AsTask() => myTask;
    }
    
    
}
