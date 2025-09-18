using System;
using System.Threading;
using JetBrains.Lifetimes;
using JetBrains.Threading;
using NUnit.Framework;

namespace Test.Lifetimes.Threading
{
    public class ProactiveLazyTest
    {
        [Test]
        public void TestSync()
        {
            var n = 100_000_000L;
            long expected = n * (n - 1) / 2;
            var lazy = new ProactiveLazy<long>(Lifetime.Eternal, () =>
            {
                long res = 0;
                for (int i = 0; i < n; i++)
                    res += i;
                return res;
            });
            
            Assert.AreEqual(expected, lazy.GetOrWait());
        }
        
        [Test]
        public void TestOceOnTerminatedLifetime()
        {
            var n = 100_000_000L;
            long expected = n * (n - 1) / 2;

            bool flag = false;
            var lazy = new ProactiveLazy<long>(Lifetime.Eternal, () =>
            {
                SpinWaitEx.SpinUntil(() => flag);
                return 42;
            });

            try
            {
                //canceled before wait started
                Assert.That(() => lazy.GetOrWait(Lifetime.Terminated), Throws.InstanceOf<OperationCanceledException>());

                //canceled after wait started
                var ld = new LifetimeDefinition();
                ThreadPool.QueueUserWorkItem(_ =>
                {
                    Thread.Sleep(100);
                    ld.Terminate();
                });
                Assert.That(() => lazy.GetOrWait(Lifetime.Terminated), Throws.InstanceOf<OperationCanceledException>());
            }
            finally
            {
                flag = true;
            }

        }
    }
}