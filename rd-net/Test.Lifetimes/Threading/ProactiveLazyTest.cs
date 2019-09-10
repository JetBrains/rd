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
        public void TestOce()
        {
            var n = 100_000_000L;
            long expected = n * (n - 1) / 2;
            var lazy = new ProactiveLazy<long>(Lifetime.Eternal, () =>
            {
                Thread.Sleep(500);
                return 42;
            });

            Assert.Throws<OperationCanceledException>(() => lazy.GetOrWait(Lifetime.Terminated));
        }
    }
}