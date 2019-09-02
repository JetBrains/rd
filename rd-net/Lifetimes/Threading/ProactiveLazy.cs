using System;
using System.Runtime.CompilerServices;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Lifetimes;

namespace JetBrains.Threading
{
    public class ProactiveLazy<T>
    {
        [NotNull] private readonly Task<T> myTask;

        public ProactiveLazy(Lifetime lifetime, Func<T> factory, TaskScheduler taskScheduler = null)
        {
            myTask = Task.Factory.StartNew(factory, lifetime, TaskCreationOptions.None, taskScheduler ?? TaskScheduler.Default);
        }

        public T GetOrWait() => myTask.Result;

        public TaskAwaiter<T> GetAwaiter() => myTask.GetAwaiter();

        public Task<T> AsTask() => myTask;
    }
    
    
}
