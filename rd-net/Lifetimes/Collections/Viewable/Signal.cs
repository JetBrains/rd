using System;
using JetBrains.Diagnostics;
using JetBrains.Lifetimes;

// ReSharper disable InconsistentNaming

namespace JetBrains.Collections.Viewable
{
    public class Signal
    {
        [ThreadStatic] private static int ourPriorityCookie;

        internal static bool IsPriorityAdvise => ourPriorityCookie > 0;

        public ref struct PriorityAdviseCookie
        {
            public static PriorityAdviseCookie Create()
            {
                ourPriorityCookie++;
                return new PriorityAdviseCookie();
            }

            public void Dispose()
            {
                ourPriorityCookie--;
            }
        }

        public ref struct NonPriorityAdviseCookie
        {
          private readonly int myOldValue;

          private NonPriorityAdviseCookie(int oldValue)
          {
            myOldValue = oldValue;
          }

          public static NonPriorityAdviseCookie Create()
          {
            var oldValue = ourPriorityCookie;
            ourPriorityCookie = 0;
            return new NonPriorityAdviseCookie(oldValue);
          }

          public void Dispose()
          {
            Assertion.Assert(ourPriorityCookie == 0);
            ourPriorityCookie = myOldValue;
          }
        }
    }



    public class SignalBase<T> : ISignal<T>, ITerminationHandler
    {
        private LifetimedList<Action<T>> myListeners;


        //todo for future use
        public IScheduler? Scheduler { get; set; }

        
        public virtual void Fire(T value)
        {
            foreach (var l in myListeners)
            {
              if (!l.Lifetime.IsAlive) 
                continue;

              try
              {
                l.Value?.Invoke(value);
              }
              catch (Exception e)
              {
                //todo suppress operation canceled
                Log.Root.Error(e);
              }
            }
        }

        public virtual void Advise(Lifetime lifetime, Action<T> handler)
        {
            if (!lifetime.IsAlive) 
              return;

            if (Signal.IsPriorityAdvise)
              myListeners.AddPriorityItem(lifetime, handler);
            else
              myListeners.Add(lifetime, handler);

            lifetime.TryOnTermination(this);
            
        }

        
        public void OnTermination(Lifetime lifetime)
        {
          myListeners.ClearValuesIfNotAlive();
        }
    }

    /// <summary>
    /// Default implementation of <see cref="ISignal{T}"/>.
    /// </summary>
    /// <typeparam name="T"></typeparam>
    public sealed /*to allow devirtualization*/ class Signal<T> : SignalBase<T>
    {
    }
}